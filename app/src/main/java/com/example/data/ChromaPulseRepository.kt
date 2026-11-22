package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.VerifiableRoundOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.random.Random

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

    fun observeAppConfig(): Flow<AppConfigEntity?> =
        dao.observeAppConfig()

    fun observeVouchers(): Flow<List<VirtualVoucherEntity>> =
        dao.observeVouchers()

    fun observeAdminLogs(): Flow<List<AdminAuditLogEntity>> =
        dao.observeAdminLogs()

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
                            message = "2,500 virtual coins credited. Explore WinGo Color Arena, Mini-Games, QR Vouchers & Admin Panel.",
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

                if (dao.getAppConfig() == null) {
                    dao.upsertAppConfig(AppConfigEntity(updatedAtMs = now))
                }

                // Pre-seed 2 demo virtual gift vouchers for QR Center testing
                dao.insertVoucher(
                    VirtualVoucherEntity(
                        code = "CP-WELCOME-500",
                        coinsAmount = 500L,
                        label = "Starter Gift QR Voucher",
                        createdAtMs = now - 60_000L
                    )
                )
                dao.insertVoucher(
                    VirtualVoucherEntity(
                        code = "CP-MEGA-1000",
                        coinsAmount = 1000L,
                        label = "Mega Demo QR Voucher",
                        createdAtMs = now
                    )
                )
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
                val config = dao.getAppConfig() ?: AppConfigEntity()
                val modeEnabled = when (mode) {
                    GameMode.MODE_30S -> config.mode30sEnabled
                    GameMode.MODE_60S -> config.mode60sEnabled
                    GameMode.MODE_3M -> config.mode3mEnabled
                    GameMode.MODE_5M -> config.mode5mEnabled
                }
                if (!modeEnabled) {
                    throw IllegalStateException("${mode.title} is currently paused by Admin.")
                }

                val user = dao.getUserProfile(userId) ?: error("Player wallet not initialized yet.")
                if (dao.getPredictionById(predId) != null) {
                    throw IllegalStateException("Prediction already locked for period $periodId.")
                }
                if (user.virtualCoins < stakeCoins) {
                    throw IllegalStateException("Insufficient virtual coins. Claim daily bonus or refill.")
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
    ): Result<Pair<Long, VerifiableRoundOutcome>> =
        try {
            val predId = prediction.id.ifBlank {
                ColorPredictionEngine.buildRoundDocId(outcome.mode, prediction.periodId)
            }
            val now = System.currentTimeMillis()

            val result = db.withTransaction {
                val currentPred = dao.getPredictionById(predId) ?: return@withTransaction (0L to outcome)
                if (currentPred.status != "PENDING") return@withTransaction (0L to outcome)

                val user = dao.getUserProfile(userId) ?: return@withTransaction (0L to outcome)
                val config = dao.getAppConfig() ?: AppConfigEntity()

                // Honor one-shot Admin demo color override if configured
                val effectiveOutcome = if (!config.forcedNextColor.isNullOrBlank()) {
                    val forcedColor = PredictionColor.fromRaw(config.forcedNextColor)
                    val forcedDigit = when (forcedColor) {
                        PredictionColor.VIOLET -> 5
                        PredictionColor.GREEN -> 7
                        PredictionColor.RED -> 4
                    }
                    dao.upsertAppConfig(config.copy(forcedNextColor = null, updatedAtMs = now))
                    outcome.copy(
                        winningColor = forcedColor,
                        resultDigit = forcedDigit
                    )
                } else {
                    outcome
                }

                val selectedColor = PredictionColor.fromRaw(currentPred.selectedColor)
                val multiplier = when (selectedColor) {
                    PredictionColor.VIOLET -> config.violetMultiplier
                    PredictionColor.RED, PredictionColor.GREEN -> config.redGreenMultiplier
                }
                val reward = if (selectedColor == effectiveOutcome.winningColor) {
                    (currentPred.stakeCoins.toDouble() * multiplier).roundToLong()
                } else {
                    0L
                }
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
                        winningColor = effectiveOutcome.winningColor.name,
                        rewardCoins = reward,
                        resultHash = effectiveOutcome.resultHash,
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
                            referenceId = effectiveOutcome.periodId,
                            description = "Won ${effectiveOutcome.mode.shortLabel} (${effectiveOutcome.winningColor.displayName})",
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
                            title = if (didWin) "+$reward Virtual Coins Won!" else "Round ${effectiveOutcome.periodId} Completed",
                            message = if (didWin) {
                                "Your ${selectedColor.displayName} pick matched ${effectiveOutcome.winningColor.displayName} (#${effectiveOutcome.resultDigit})."
                            } else {
                                "${effectiveOutcome.mode.title} landed on ${effectiveOutcome.winningColor.displayName} (#${effectiveOutcome.resultDigit})."
                            },
                            referenceId = effectiveOutcome.periodId,
                            read = false,
                            createdAtMs = now,
                            updatedAtMs = now
                        )
                    )
                }

                reward to effectiveOutcome
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    /**
     * Atomic virtual-coin settlement for Mini-Games (Lucky Spin Wheel, Color Mines, Cyber Dice).
     */
    suspend fun playMiniGameRound(
        userId: String = DEFAULT_USER_ID,
        gameTitle: String,
        stakeCoins: Long,
        rewardCoins: Long,
        summary: String
    ): Result<Long> =
        try {
            require(stakeCoins >= 0L && rewardCoins >= 0L) { "Invalid virtual coin values." }
            val now = System.currentTimeMillis()
            val txBase = "mini_${now}_${Random.nextInt(100, 999)}"

            val netDelta = db.withTransaction {
                val user = dao.getUserProfile(userId) ?: error("Player wallet not ready.")
                if (user.virtualCoins < stakeCoins) {
                    throw IllegalStateException("Not enough virtual coins. Claim Daily Bonus or Demo Refill.")
                }

                val afterStake = user.virtualCoins - stakeCoins
                val finalBalance = afterStake + rewardCoins
                val didWin = rewardCoins > stakeCoins
                val nextGames = user.totalGames + 1L
                val nextWins = if (didWin) user.totalWins + 1L else user.totalWins
                val nextPoints = user.totalVirtualPoints + rewardCoins
                val nextStreak = if (didWin) user.currentStreak + 1L else 0L
                val nextBestStreak = maxOf(user.bestStreak, nextStreak)
                val nextBadge = ColorPredictionEngine.resolveBadgeTitle(nextPoints, nextWins, nextBestStreak)

                dao.updateUserProfile(
                    user.copy(
                        virtualCoins = finalBalance,
                        totalGames = nextGames,
                        totalWins = nextWins,
                        totalVirtualPoints = nextPoints,
                        currentStreak = nextStreak,
                        bestStreak = nextBestStreak,
                        updatedAtMs = now
                    )
                )

                if (stakeCoins > 0L) {
                    dao.insertWalletTransaction(
                        WalletTransaction(
                            id = "${txBase}_in",
                            userId = userId,
                            txType = "MINI_GAME_ENTRY",
                            amount = -stakeCoins,
                            previousBalance = user.virtualCoins,
                            newBalance = afterStake,
                            referenceId = gameTitle,
                            description = "$gameTitle entry ($summary)",
                            createdAtMs = now
                        )
                    )
                }

                if (rewardCoins > 0L) {
                    dao.insertWalletTransaction(
                        WalletTransaction(
                            id = "${txBase}_out",
                            userId = userId,
                            txType = "MINI_GAME_REWARD",
                            amount = rewardCoins,
                            previousBalance = afterStake,
                            newBalance = finalBalance,
                            referenceId = gameTitle,
                            description = "$gameTitle win: $summary",
                            createdAtMs = now + 1L
                        )
                    )
                }

                dao.upsertLeaderboardEntry(
                    LeaderboardEntry(
                        id = userId,
                        userId = userId,
                        displayName = user.displayName,
                        publicUserId = user.publicUserId,
                        photoUrl = user.photoUrl,
                        totalVirtualPoints = nextPoints,
                        totalGames = nextGames,
                        totalWins = nextWins,
                        bestStreak = nextBestStreak,
                        badgeTitle = nextBadge,
                        createdAtMs = user.createdAtMs,
                        updatedAtMs = now
                    )
                )

                rewardCoins - stakeCoins
            }
            Result.success(netDelta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun redeemVoucherCode(
        userId: String = DEFAULT_USER_ID,
        rawCode: String
    ): Result<Long> =
        try {
            val cleanCode = rawCode.trim().uppercase(Locale.US)
            require(cleanCode.isNotBlank()) { "Please enter a Virtual Voucher code." }
            val now = System.currentTimeMillis()

            val credited = db.withTransaction {
                val voucher = dao.getVoucherByCode(cleanCode)
                    ?: throw IllegalStateException("Voucher '$cleanCode' not found. Create one in Admin Panel or try CP-WELCOME-500.")
                if (voucher.redeemed) {
                    throw IllegalStateException("Voucher '$cleanCode' has already been redeemed.")
                }
                val user = dao.getUserProfile(userId) ?: error("Player wallet not ready.")
                val newBalance = user.virtualCoins + voucher.coinsAmount

                dao.updateVoucher(voucher.copy(redeemed = true, redeemedAtMs = now))
                dao.updateUserProfile(user.copy(virtualCoins = newBalance, updatedAtMs = now))
                dao.insertWalletTransaction(
                    WalletTransaction(
                        id = "voucher_$cleanCode",
                        userId = userId,
                        txType = "QR_VOUCHER_REDEEM",
                        amount = voucher.coinsAmount,
                        previousBalance = user.virtualCoins,
                        newBalance = newBalance,
                        referenceId = cleanCode,
                        description = "Redeemed QR Voucher: ${voucher.label}",
                        createdAtMs = now
                    )
                )
                dao.insertNotification(
                    UserNotification(
                        id = "notif_voucher_$cleanCode",
                        userId = userId,
                        category = "DAILY_BONUS",
                        title = "QR Voucher Redeemed!",
                        message = "+${voucher.coinsAmount} virtual coins added via code $cleanCode.",
                        referenceId = cleanCode,
                        read = false,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                )
                voucher.coinsAmount
            }
            Result.success(credited)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    // ---- ADMIN PANEL ACTIONS ----

    suspend fun adminUpdateConfig(
        config: AppConfigEntity,
        auditSummary: String
    ): Result<Unit> =
        try {
            val now = System.currentTimeMillis()
            db.withTransaction {
                dao.upsertAppConfig(config.copy(updatedAtMs = now))
                dao.insertAdminLog(
                    AdminAuditLogEntity(
                        id = "adm_$now",
                        action = "CONFIG_UPDATE",
                        details = auditSummary,
                        createdAtMs = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun adminAdjustUserCoins(
        userId: String = DEFAULT_USER_ID,
        deltaCoins: Long,
        reason: String
    ): Result<Long> =
        try {
            require(deltaCoins != 0L) { "Adjustment amount cannot be zero." }
            val now = System.currentTimeMillis()
            val cleanReason = reason.trim().ifBlank {
                if (deltaCoins > 0) "Admin virtual demo credit" else "Admin virtual demo debit"
            }

            val updatedBalance = db.withTransaction {
                val user = dao.getUserProfile(userId) ?: error("Player profile not found.")
                val nextBalance = (user.virtualCoins + deltaCoins).coerceAtLeast(0L)
                val appliedDelta = nextBalance - user.virtualCoins

                dao.updateUserProfile(user.copy(virtualCoins = nextBalance, updatedAtMs = now))
                dao.insertWalletTransaction(
                    WalletTransaction(
                        id = "admin_adj_$now",
                        userId = userId,
                        txType = if (appliedDelta >= 0L) "ADMIN_CREDIT" else "ADMIN_DEBIT",
                        amount = appliedDelta,
                        previousBalance = user.virtualCoins,
                        newBalance = nextBalance,
                        referenceId = "ADMIN",
                        description = cleanReason,
                        createdAtMs = now
                    )
                )
                dao.insertNotification(
                    UserNotification(
                        id = "notif_admin_$now",
                        userId = userId,
                        category = "ANNOUNCEMENT",
                        title = if (appliedDelta >= 0L) "Admin Credited +$appliedDelta VC" else "Admin Adjusted $appliedDelta VC",
                        message = cleanReason,
                        referenceId = "ADMIN",
                        read = false,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                )
                dao.insertAdminLog(
                    AdminAuditLogEntity(
                        id = "adm_coin_$now",
                        action = if (appliedDelta >= 0L) "CREDIT_COINS" else "DEBIT_COINS",
                        details = "${if (appliedDelta >= 0L) "+" else ""}$appliedDelta VC to ${user.publicUserId}: $cleanReason",
                        createdAtMs = now
                    )
                )
                nextBalance
            }
            Result.success(updatedBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun adminCreateVoucher(
        coinsAmount: Long,
        label: String
    ): Result<VirtualVoucherEntity> =
        try {
            require(coinsAmount in 50L..50_000L) { "Voucher amount must be 50–50,000 VC." }
            val now = System.currentTimeMillis()
            val randomSuffix = Random.nextInt(1000, 9999)
            val code = "CP-${coinsAmount}VC-$randomSuffix"
            val voucher = VirtualVoucherEntity(
                code = code,
                coinsAmount = coinsAmount,
                label = label.trim().ifBlank { "Admin QR Gift Voucher" },
                redeemed = false,
                createdAtMs = now
            )
            db.withTransaction {
                dao.insertVoucher(voucher)
                dao.insertAdminLog(
                    AdminAuditLogEntity(
                        id = "adm_vch_$now",
                        action = "CREATE_QR_VOUCHER",
                        details = "Generated $code (+$coinsAmount VC)",
                        createdAtMs = now
                    )
                )
            }
            Result.success(voucher)
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
