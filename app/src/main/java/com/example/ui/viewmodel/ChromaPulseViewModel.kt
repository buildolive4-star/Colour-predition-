package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChromaPulseRepository
import com.example.data.GameRoundRecord
import com.example.data.LeaderboardEntry
import com.example.data.PredictionRecord
import com.example.data.UserNotification
import com.example.data.UserProfile
import com.example.data.WalletTransaction
import com.example.domain.ActiveRoundSnapshot
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.UiState
import com.example.domain.VerifiableRoundOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RoundSettlementBanner(
    val mode: GameMode,
    val periodId: String,
    val winningColor: PredictionColor,
    val resultDigit: Int,
    val resultHash: String,
    val userPredicted: Boolean,
    val didWin: Boolean,
    val rewardCoins: Long
)

class ChromaPulseViewModel(
    private val repository: ChromaPulseRepository,
    val currentUserId: String = ChromaPulseRepository.DEFAULT_USER_ID
) : ViewModel() {

    private val _selectedMode = MutableStateFlow(GameMode.MODE_30S)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _activeRounds = MutableStateFlow(buildActiveRoundsMap(System.currentTimeMillis()))
    val activeRounds: StateFlow<Map<GameMode, ActiveRoundSnapshot>> = _activeRounds.asStateFlow()

    private val _recentOutcomesByMode = MutableStateFlow(buildRecentOutcomesMap(System.currentTimeMillis()))
    val recentOutcomesByMode: StateFlow<Map<GameMode, List<VerifiableRoundOutcome>>> = _recentOutcomesByMode.asStateFlow()

    private val _latestBanner = MutableStateFlow<RoundSettlementBanner?>(null)
    val latestBanner: StateFlow<RoundSettlementBanner?> = _latestBanner.asStateFlow()

    private val _isSubmittingPrediction = MutableStateFlow(false)
    val isSubmittingPrediction: StateFlow<Boolean> = _isSubmittingPrediction.asStateFlow()

    private val _isClaimingBonus = MutableStateFlow(false)
    val isClaimingBonus: StateFlow<Boolean> = _isClaimingBonus.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .toUiStateFlow("Failed to load player profile")

    val predictionsState: StateFlow<UiState<List<PredictionRecord>>> = repository.observePredictions(currentUserId)
        .toUiStateFlow("Failed to load predictions")

    val walletTransactionsState: StateFlow<UiState<List<WalletTransaction>>> =
        repository.observeWalletTransactions(currentUserId)
            .toUiStateFlow("Failed to load wallet ledger")

    val notificationsState: StateFlow<UiState<List<UserNotification>>> =
        repository.observeNotifications(currentUserId)
            .toUiStateFlow("Failed to load notifications")

    val gameRoundsState: StateFlow<UiState<List<GameRoundRecord>>> =
        repository.observeGameRounds()
            .toUiStateFlow("Failed to load round history")

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeLeaderboard()
            .toUiStateFlow("Failed to load leaderboard")

    private val settlingIds = mutableSetOf<String>()
    private val publishedRoundIds = mutableSetOf<String>()
    private val lastPeriodByMode = mutableMapOf<GameMode, String>()

    init {
        initializeLocalPlayerAndEngine()
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackMessage: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, fallbackMessage, error)
                emit(UiState.Error(error.localizedMessage ?: fallbackMessage))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private fun initializeLocalPlayerAndEngine() {
        viewModelScope.launch {
            repository.ensureLocalPlayerInitialized(currentUserId)
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Failed to initialize demo wallet")
                }

            val now = System.currentTimeMillis()
            GameMode.entries.forEach { mode ->
                val active = ColorPredictionEngine.computeActiveRound(mode, now)
                lastPeriodByMode[mode] = active.periodId
                val prev = ColorPredictionEngine.computePreviousCompletedRound(mode, now)
                publishRoundIfNeeded(prev)
            }
        }

        viewModelScope.launch {
            predictionsState.collect { state ->
                if (state is UiState.Success) {
                    settleDuePredictions(state.data, System.currentTimeMillis())
                }
            }
        }

        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val nextActive = buildActiveRoundsMap(now)
                _activeRounds.value = nextActive

                var periodChanged = false
                GameMode.entries.forEach { mode ->
                    val currentPeriod = nextActive[mode]?.periodId ?: return@forEach
                    val previousPeriod = lastPeriodByMode[mode]
                    if (previousPeriod != null && previousPeriod != currentPeriod) {
                        periodChanged = true
                        lastPeriodByMode[mode] = currentPeriod
                        val completedOutcome = ColorPredictionEngine.computePreviousCompletedRound(mode, now)
                        launch { publishRoundIfNeeded(completedOutcome) }
                        if (mode == _selectedMode.value) {
                            showRoundResultBanner(completedOutcome)
                        }
                    } else if (previousPeriod == null) {
                        lastPeriodByMode[mode] = currentPeriod
                    }
                }

                if (periodChanged) {
                    _recentOutcomesByMode.value = buildRecentOutcomesMap(now)
                    val preds = (predictionsState.value as? UiState.Success)?.data.orEmpty()
                    settleDuePredictions(preds, now)
                }

                delay(250L)
            }
        }
    }

    private suspend fun publishRoundIfNeeded(outcome: VerifiableRoundOutcome) {
        if (!publishedRoundIds.add(outcome.roundDocId)) return
        repository.recordCompletedRoundIfAbsent(outcome)
    }

    private fun settleDuePredictions(predictions: List<PredictionRecord>, nowMs: Long) {
        predictions.filter { it.status == "PENDING" }.forEach { pred ->
            val mode = GameMode.fromId(pred.modeId)
            val endMs = ColorPredictionEngine.parsePeriodEndTimeMs(mode, pred.periodId) ?: return@forEach
            if (nowMs >= endMs) {
                val key = pred.id.ifBlank { "${mode.id}_${pred.periodId}" }
                if (settlingIds.add(key)) {
                    viewModelScope.launch {
                        val startMs = endMs - mode.durationMs
                        val outcome = ColorPredictionEngine.generateVerifiableOutcomeForPeriod(
                            mode = mode,
                            periodId = pred.periodId,
                            startTimeMs = startMs,
                            endTimeMs = endMs
                        )
                        publishRoundIfNeeded(outcome)
                        repository.settlePrediction(currentUserId, pred, outcome)
                            .onSuccess { reward ->
                                _latestBanner.value = RoundSettlementBanner(
                                    mode = mode,
                                    periodId = pred.periodId,
                                    winningColor = outcome.winningColor,
                                    resultDigit = outcome.resultDigit,
                                    resultHash = outcome.resultHash,
                                    userPredicted = true,
                                    didWin = reward > 0L,
                                    rewardCoins = reward
                                )
                            }
                            .onFailure {
                                settlingIds.remove(key)
                            }
                    }
                }
            }
        }
    }

    private fun showRoundResultBanner(outcome: VerifiableRoundOutcome) {
        val preds = (predictionsState.value as? UiState.Success)?.data.orEmpty()
        val matching = preds.firstOrNull {
            it.modeId == outcome.mode.id && it.periodId == outcome.periodId
        }
        if (matching == null) {
            _latestBanner.value = RoundSettlementBanner(
                mode = outcome.mode,
                periodId = outcome.periodId,
                winningColor = outcome.winningColor,
                resultDigit = outcome.resultDigit,
                resultHash = outcome.resultHash,
                userPredicted = false,
                didWin = false,
                rewardCoins = 0L
            )
        }
    }

    fun selectMode(mode: GameMode) {
        _selectedMode.value = mode
    }

    fun dismissBanner() {
        _latestBanner.value = null
    }

    fun submitPrediction(mode: GameMode, color: PredictionColor, stakeCoins: Long) {
        val active = _activeRounds.value[mode] ?: return
        if (!active.canPredict) {
            _snackbarMessages.tryEmit("Round is locked! Wait for the next period to open.")
            return
        }
        if (_isSubmittingPrediction.value) return

        viewModelScope.launch {
            _isSubmittingPrediction.value = true
            repository.submitPrediction(
                userId = currentUserId,
                mode = mode,
                periodId = active.periodId,
                selectedColor = color,
                stakeCoins = stakeCoins
            ).onSuccess {
                _snackbarMessages.tryEmit(
                    "Locked $stakeCoins virtual coins on ${color.displayName} (${active.periodId})"
                )
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Unable to lock prediction.")
            }
            _isSubmittingPrediction.value = false
        }
    }

    fun claimDailyBonus() {
        if (_isClaimingBonus.value) return
        val todayUtc = ColorPredictionEngine.formatUtcIsoDate(System.currentTimeMillis())
        viewModelScope.launch {
            _isClaimingBonus.value = true
            repository.claimDailyBonus(currentUserId, todayUtc)
                .onSuccess { bonus ->
                    _snackbarMessages.tryEmit("+$bonus virtual coins added to your demo wallet!")
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit(err.localizedMessage ?: "Daily bonus unavailable.")
                }
            _isClaimingBonus.value = false
        }
    }

    fun updateProfileSettings(
        displayName: String,
        notifyResults: Boolean,
        notifyRoundClosing: Boolean,
        notifyBonuses: Boolean
    ) {
        viewModelScope.launch {
            repository.updateProfileSettings(
                userId = currentUserId,
                displayName = displayName,
                notifyResults = notifyResults,
                notifyRoundClosing = notifyRoundClosing,
                notifyBonuses = notifyBonuses
            ).onSuccess {
                _snackbarMessages.tryEmit("Profile & preferences saved.")
            }.onFailure { err ->
                _snackbarMessages.tryEmit(err.localizedMessage ?: "Failed to save settings.")
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
        }
    }

    private companion object {
        const val TAG = "ChromaPulseVM"
        const val STOP_TIMEOUT_MILLIS = 5000L

        fun buildActiveRoundsMap(nowMs: Long): Map<GameMode, ActiveRoundSnapshot> =
            GameMode.entries.associateWith { mode ->
                ColorPredictionEngine.computeActiveRound(mode, nowMs)
            }

        fun buildRecentOutcomesMap(nowMs: Long): Map<GameMode, List<VerifiableRoundOutcome>> =
            GameMode.entries.associateWith { mode ->
                ColorPredictionEngine.computeRecentCompletedRounds(mode, nowMs, 10)
            }
    }
}
