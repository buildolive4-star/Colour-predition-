package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.VerifiableRoundOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class ChromaPulseRepository(
    private val db: ChromaPulseDatabase
) {
    constructor(context: Context) : this(ChromaPulseDatabase.getInstance(context))

    private val dao: ChromaPulseDao get() = db.dao()

    fun observeUserProfile(userId: String = DEFAULT_USER_ID): Flow<UserProfile?> =
        dao.observeUserProfile(userId)

    fun observePredictions(userId: String = DEFAULT_USER_ID): Flow<List<PredictionRecord>> =
        dao.observePredictions(userId)

    fun observeWalletTransactions(userId: String = DEFAULT_USER_ID): Flow<List<WalletTransaction>> =
        dao.observeWalletTransactions(userId)

    fun observeNotifications(userId: String = DEFAULT_USER_ID): Flow<List<UserNotification>> =
        dao.observeNotifications(userId)

    fun observeGameRounds(): Flow<List<GameRoundRecord>> =
        dao.observeGameRounds()

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> =
        dao.observeLeaderboard()

    suspend fun ensureLocalPlayerInitialized(userId: String = DEFAULT_USER_ID): Result<Unit> =
        try {
            val now = System.currentTimeMillis()
            val publicId = ColorPredictionEngine.buildPublicUserId(userId)
            db.withTransaction {
                val existing = dao.getUserProfile(userId)
                if (existing == null) {
                    val initialCoins = ColorPredictionEngine.INITIAL_DEMO_COINS
                    dao.insertUserProfile(
                        UserProfile(
                            id = userId,
                            userId = userId,
                            displayName = "Chroma Player",
                            email = "Instant Play • Device Wallet",
                            publicUserId = publicId,
                            virtualCoins = initialCoins,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                    dao.insertWalletTransaction(
                        WalletTransaction(
                            id = "welcome_bonus",
                            userId = userId,
                            txType = "WELCOME_BONUS",
                            amount = initialCoins,
                            previousBalance = 0L,
                            newBalance = initialCoins,
                            referenceId = "welcome_bonus",
                            description = "Welcome virtual demo coins (entertainment only)",
                            createdAtMs = now
                        )
                    )
                    dao.insertNotification(
                        UserNotification(
                            id = "welcome_notice",
                            userId = userId,
                            category = "ANNOUNCEMENT",
                            title = "Welcome to ChromaPulse!",
                            message = "2,500 virtual coins credited to your demo wallet. Pick Red, Green, or Violet in live timed rounds.",
                            referenceId = "welcome_bonus",
                            read = false,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                }

                if (dao.getLeaderboardEntry(userId) == null) {
                    val profile = dao.getUserProfile(userId)
                    dao.upsertLeaderboardEntry(
                        LeaderboardEntry(
                            id = userId,
                            userId = userId,
                            displayName = profile?.displayName ?: "Chroma Player",
                            publicUserId = profile?.publicUserId ?: publicId,
                            totalVirtualPoints = profile?.totalVirtualPoints ?: 0L,
                            totalGames = profile?.totalGames ?: 0L,
                            totalWins = profile?.totalWins ?: 0L,
                            bestStreak = profile?.bestStreak ?: 0L,
                            badgeTitle = "Pulse Rookie",
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun submitPrediction(
        userId: String = DEFAULT_USER_ID,
        mode: GameMode,
        periodId: String,
        selectedColor: PredictionColor,
        stakeCoins: Long
    ): Result<Unit> =
        try {
            require(stakeCoins in ColorPredictionEngine.MIN_STAKE_COINS..ColorPredictionEngine.MAX_STAKE_COINS) {
                "Virtual stake must be between ${ColorPredictionEngine.MIN_STAKE_COINS} and ${ColorPredictionEngine.MAX_STAKE_COINS} coins."
            }
            val predId = ColorPredictionEngine.buildRoundDocId(mode, periodId)
            val now = System.currentTimeMillis()

            db.withTransaction {
                val user = dao.getUserProfile(userId) ?: error("Player wallet not initialized yet.")
                if (dao.getPredictionById(predId) != null) {
                    throw IllegalStateException("Prediction already locked for period $periodId.")
                }
                if (user.virtualCoins < stakeCoins) {
                    throw IllegalStateException("Insufficient virtual coins. Claim daily bonus or choose a smaller stake.")
                }

                val newBalance = user.virtualCoins - stakeCoins
                val nextGames = user.totalGames + 1L

                dao.updateUserProfile(
                    user.copy(
                        virtualCoins = newBalance,
                        totalGames = nextGames,
                        updatedAtMs = now
                    )
                )

                dao.insertPrediction(
                    PredictionRecord(
                        id = predId,
                        userId = userId,
                        modeId = mode.id,
                        periodId = periodId,
                        selectedColor = selectedColor.name,
                        stakeCoins = stakeCoins,
                        status = "PENDING",
                        rewardCoins = 0L,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                )

                dao.insertWalletTransaction(
                    WalletTransaction(
                        id = "bet_$predId",
                        userId = userId,
                        txType = "PREDICTION_ENTRY",
                        amount = -stakeCoins,
                        previousBalance = user.virtualCoins,
                        newBalance = newBalance,
                        referenceId = periodId,
                        description = "${mode.shortLabel} prediction: ${selectedColor.displayName} ($periodId)",
                        createdAtMs = now
                    )
                )

                dao.getLeaderboardEntry(userId)?.let { lb ->
                    dao.upsertLeaderboardEntry(
                        lb.copy(
                            totalGames = nextGames,
                            updatedAtMs = now
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun recordCompletedRoundIfAbsent(outcome: VerifiableRoundOutcome): Result<Unit> =
        try {
            dao.insertGameRoundIfAbsent(
                GameRoundRecord(
                    id = outcome.roundDocId,
                    modeId = outcome.mode.id,
                    periodId = outcome.periodId,
                    startTimeMs = outcome.startTimeMs,
                    endTimeMs = outcome.endTimeMs,
                    winningColor = outcome.winningColor.name,
                    resultDigit = outcome.resultDigit.toLong(),
                    resultHash = outcome.resultHash,
                    status = "COMPLETED",
                    createdAtMs = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun settlePrediction(
        userId: String = DEFAULT_USER_ID,
        prediction: PredictionRecord,
        outcome: VerifiableRoundOutcome
    ): Result<Long> =
        try {
            val predId = prediction.id.ifBlank {
                ColorPredictionEngine.buildRoundDocId(outcome.mode, prediction.periodId)
            }
            val now = System.currentTimeMillis()

            val awarded = db.withTransaction {
                val currentPred = dao.getPredictionById(predId) ?: return@withTransaction 0L
                if (currentPred.status != "PENDING") return@withTransaction 0L

                val user = dao.getUserProfile(userId) ?: return@withTransaction 0L
                val selectedColor = PredictionColor.fromRaw(currentPred.selectedColor)
                val reward = ColorPredictionEngine.calculateRewardCoins(
                    selectedColor = selectedColor,
                    winningColor = outcome.winningColor,
                    stakeCoins = currentPred.stakeCoins
                )
                val didWin = reward > 0L

                val nextCoins = user.virtualCoins + reward
                val nextWins = if (didWin) user.totalWins + 1L else user.totalWins
                val nextPoints = user.totalVirtualPoints + reward
                val nextStreak = if (didWin) user.currentStreak + 1L else 0L
                val nextBestStreak = maxOf(user.bestStreak, nextStreak)
                val nextBadge = ColorPredictionEngine.resolveBadgeTitle(nextPoints, nextWins, nextBestStreak)

                dao.updatePrediction(
                    currentPred.copy(
                        status = if (didWin) "WON" else "LOST",
                        winningColor = outcome.winningColor.name,
                        rewardCoins = reward,
                        resultHash = outcome.resultHash,
                        updatedAtMs = now
                    )
                )

                dao.updateUserProfile(
                    user.copy(
                        virtualCoins = nextCoins,
                        totalWins = nextWins,
                        totalVirtualPoints = nextPoints,
                        currentStreak = nextStreak,
                        bestStreak = nextBestStreak,
                        updatedAtMs = now
                    )
                )

                dao.upsertLeaderboardEntry(
                    LeaderboardEntry(
                        id = userId,
                        userId = userId,
                        displayName = user.displayName,
                        publicUserId = user.publicUserId,
                        photoUrl = user.photoUrl,
                        totalVirtualPoints = nextPoints,
                        totalGames = maxOf(user.totalGames, nextWins),
                        totalWins = nextWins,
                        bestStreak = nextBestStreak,
                        badgeTitle = nextBadge,
                        createdAtMs = user.createdAtMs,
                        updatedAtMs = now
                    )
                )

                if (didWin) {
                    dao.insertWalletTransaction(
                        WalletTransaction(
                            id = "win_$predId",
                            userId = userId,
                            txType = "PREDICTION_REWARD",
                            amount = reward,
                            previousBalance = user.virtualCoins,
                            newBalance = nextCoins,
                            referenceId = outcome.periodId,
                            description = "Won ${outcome.mode.shortLabel} (${outcome.winningColor.displayName} ${selectedColor.multiplierLabel})",
                            createdAtMs = now
                        )
                    )
                }

                if (user.notifyResults) {
                    dao.insertNotification(
                        UserNotification(
                            id = "res_$predId",
                            userId = userId,
                            category = if (didWin) "REWARD" else "ROUND_RESULT",
                            title = if (didWin) "+$reward Virtual Coins Won!" else "Round ${outcome.periodId} Completed",
                            message = if (didWin) {
                                "Your ${selectedColor.displayName} pick matched ${outcome.winningColor.displayName} (#${outcome.resultDigit}) in ${outcome.mode.title}."
                            } else {
                                "${outcome.mode.title} landed on ${outcome.winningColor.displayName} (#${outcome.resultDigit}). Your pick was ${selectedColor.displayName}."
                            },
                            referenceId = outcome.periodId,
                            read = false,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                }

                reward
            }
            Result.success(awarded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun claimDailyBonus(
        userId: String = DEFAULT_USER_ID,
        utcIsoDate: String
    ): Result<Long> =
        try {
            val compactDate = utcIsoDate.replace("-", "")
            val txId = "daily_$compactDate"
            val bonusAmount = ColorPredictionEngine.DAILY_BONUS_COINS
            val now = System.currentTimeMillis()

            db.withTransaction {
                val user = dao.getUserProfile(userId) ?: error("Player wallet not ready.")
                if (user.lastDailyBonusDate == utcIsoDate || dao.getTransactionById(txId) != null) {
                    throw IllegalStateException("Daily virtual coin bonus already claimed today.")
                }
                val newCoins = user.virtualCoins + bonusAmount
                dao.updateUserProfile(
                    user.copy(
                        virtualCoins = newCoins,
                        lastDailyBonusDate = utcIsoDate,
                        updatedAtMs = now
                    )
                )
                dao.insertWalletTransaction(
                    WalletTransaction(
                        id = txId,
                        userId = userId,
                        txType = "DAILY_BONUS",
                        amount = bonusAmount,
                        previousBalance = user.virtualCoins,
                        newBalance = newCoins,
                        referenceId = "daily_$utcIsoDate",
                        description = "Daily virtual coin login reward",
                        createdAtMs = now
                    )
                )
                if (user.notifyBonuses) {
                    dao.insertNotification(
                        UserNotification(
                            id = "notif_$txId",
                            userId = userId,
                            category = "DAILY_BONUS",
                            title = "Daily Bonus Credited",
                            message = "+$bonusAmount virtual coins added to your demo wallet for $utcIsoDate.",
                            referenceId = "daily_$utcIsoDate",
                            read = false,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                }
            }
            Result.success(bonusAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun updateProfileSettings(
        userId: String = DEFAULT_USER_ID,
        displayName: String,
        notifyResults: Boolean,
        notifyRoundClosing: Boolean,
        notifyBonuses: Boolean
    ): Result<Unit> =
        try {
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotBlank()) { "Display name cannot be empty." }
            val now = System.currentTimeMillis()
            db.withTransaction {
                val user = dao.getUserProfile(userId) ?: return@withTransaction
                dao.updateUserProfile(
                    user.copy(
                        displayName = cleanName,
                        notifyResults = notifyResults,
                        notifyRoundClosing = notifyRoundClosing,
                        notifyBonuses = notifyBonuses,
                        updatedAtMs = now
                    )
                )
                dao.getLeaderboardEntry(userId)?.let { lb ->
                    dao.upsertLeaderboardEntry(
                        lb.copy(
                            displayName = cleanName,
                            updatedAtMs = now
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun claimDemoRefill(userId: String = DEFAULT_USER_ID): Result<Long> =
        try {
            val refillAmount = ColorPredictionEngine.DEMO_REFILL_COINS
            val threshold = ColorPredictionEngine.DEMO_REFILL_THRESHOLD
            val now = System.currentTimeMillis()
            val txId = "refill_$now"

            db.withTransaction {
                val user = dao.getUserProfile(userId) ?: error("Player wallet not ready.")
                if (user.virtualCoins >= threshold) {
                    throw IllegalStateException("Free demo refill unlocks when your balance is below $threshold VC.")
                }
                val newCoins = user.virtualCoins + refillAmount
                dao.updateUserProfile(
                    user.copy(
                        virtualCoins = newCoins,
                        updatedAtMs = now
                    )
                )
                dao.insertWalletTransaction(
                    WalletTransaction(
                        id = txId,
                        userId = userId,
                        txType = "DEMO_REFILL",
                        amount = refillAmount,
                        previousBalance = user.virtualCoins,
                        newBalance = newCoins,
                        referenceId = txId,
                        description = "Free virtual demo coin refill (entertainment only)",
                        createdAtMs = now
                    )
                )
                if (user.notifyBonuses) {
                    dao.insertNotification(
                        UserNotification(
                            id = "notif_$txId",
                            userId = userId,
                            category = "DAILY_BONUS",
                            title = "Demo Wallet Refilled",
                            message = "+$refillAmount virtual demo coins credited so you can keep playing.",
                            referenceId = txId,
                            read = false,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                }
            }
            Result.success(refillAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> =
        try {
            dao.markNotificationRead(notificationId, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun markAllNotificationsRead(userId: String = DEFAULT_USER_ID): Result<Unit> =
        try {
            dao.markAllNotificationsRead(userId, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun deleteNotification(notificationId: String): Result<Unit> =
        try {
            dao.deleteNotification(notificationId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        const val DEFAULT_USER_ID = "local_player"
    }
}
