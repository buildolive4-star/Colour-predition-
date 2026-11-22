package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.VerifiableRoundOutcome
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.random.Random

class ChromaPulseRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Cloud Firestore.")

    private fun currentUserEmail(): String =
        auth.currentUser?.email.orEmpty().lowercase(Locale.US)

    fun isSuperAdminEmail(email: String? = auth.currentUser?.email): Boolean {
        val verified = auth.currentUser?.isEmailVerified == true
        return verified && email.orEmpty().equals(SUPER_ADMIN_EMAIL, ignoreCase = true)
    }

    private fun <T> Flow<T>.wrapFirestoreListener(uid: String, path: String, op: OperationType): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // ---- Realtime Player Streams ----

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Profile stream must match authenticated UID." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.GET)
        )
    }

    fun observePredictions(userId: String): Flow<List<PredictionRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/predictions"
        emitAll(
            db.collection("users").document(uid).collection("predictions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PredictionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/wallet_transactions"
        emitAll(
            db.collection("users").document(uid).collection("wallet_transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeGameRounds(): Flow<List<GameRoundRecord>> = flow {
        val uid = requireUserId()
        val path = "game_rounds"
        emitAll(
            db.collection("game_rounds")
                .whereEqualTo("status", "COMPLETED")
                .limit(120)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameRoundRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.endTimeMs }
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId()
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereGreaterThanOrEqualTo("totalVirtualPoints", 0L)
                .orderBy("totalVirtualPoints", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeAppConfig(): Flow<AppConfigEntity> = flow {
        val uid = requireUserId()
        val path = "app_config/global"
        emitAll(
            db.collection("app_config").document("global")
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) AppConfigEntity()
                    else snap.toObject(AppConfigEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: AppConfigEntity()
                }
                .wrapFirestoreListener(uid, path, OperationType.GET)
        )
    }

    fun observePublicVouchers(): Flow<List<VirtualVoucherEntity>> = flow {
        val uid = requireUserId()
        val path = "virtual_vouchers"
        emitAll(
            db.collection("virtual_vouchers")
                .whereEqualTo("publicVisible", true)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(VirtualVoucherEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<VirtualVoucherEntity> { it.redeemed }.thenByDescending { it.createdAtMs })
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeOwnCoAdminGrant(): Flow<CoAdminGrantEntity?> = flow {
        val uid = requireUserId()
        val path = "co_admins/$uid"
        emitAll(
            db.collection("co_admins").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(CoAdminGrantEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.GET)
        )
    }

    // ---- Realtime Admin-Only Streams (collected ONLY when authorized as Main Admin / Co-Admin) ----

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        val path = "users"
        emitAll(
            db.collection("users")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.virtualCoins }
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeAdminLogs(): Flow<List<AdminAuditLogEntity>> = flow {
        val uid = requireUserId()
        val path = "admin_audit_logs"
        emitAll(
            db.collection("admin_audit_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AdminAuditLogEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeCoAdminInvites(): Flow<List<CoAdminInviteEntity>> = flow {
        val uid = requireUserId()
        val path = "co_admin_invites"
        emitAll(
            db.collection("co_admin_invites")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CoAdminInviteEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAtMs }
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeAllCoAdminGrants(): Flow<List<CoAdminGrantEntity>> = flow {
        val uid = requireUserId()
        val path = "co_admins"
        emitAll(
            db.collection("co_admins")
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CoAdminGrantEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAtMs }
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    // ---- Player Bootstrapping ----

    suspend fun ensureCloudPlayerInitialized(): Result<Unit> {
        var path = "users"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("Not signed in.")
            val uid = user.uid
            path = "users/$uid"
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            val publicId = ColorPredictionEngine.buildPublicUserId(uid)
            val displayName = (user.displayName?.trim()?.take(60)).orEmpty().ifBlank { "Chroma Player" }
            val email = user.email.orEmpty().take(120)
            val photoUrl = user.photoUrl?.toString()?.take(600)

            if (!snap.exists()) {
                val initialCoins = ColorPredictionEngine.INITIAL_DEMO_COINS
                val batch = db.batch()
                val profilePayload = buildMap<String, Any?> {
                    put("userId", uid)
                    put("displayName", displayName)
                    put("email", email)
                    put("publicUserId", publicId)
                    put("photoUrl", photoUrl)
                    put("virtualCoins", initialCoins)
                    put("totalGames", 0L)
                    put("totalWins", 0L)
                    put("totalVirtualPoints", 0L)
                    put("currentStreak", 0L)
                    put("bestStreak", 0L)
                    put("notifyResults", true)
                    put("notifyRoundClosing", true)
                    put("notifyBonuses", true)
                    put("accountStatus", "ACTIVE")
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                batch.set(userRef, profilePayload)

                val txRef = userRef.collection("wallet_transactions").document("welcome_bonus")
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "WELCOME_BONUS",
                        "amount" to initialCoins,
                        "previousBalance" to 0L,
                        "newBalance" to initialCoins,
                        "referenceId" to "welcome_bonus",
                        "description" to "Welcome virtual demo coins (entertainment only)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                val notifRef = userRef.collection("notifications").document("welcome_notice")
                batch.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "category" to "ANNOUNCEMENT",
                        "title" to "Welcome to ChromaPulse Cloud!",
                        "message" to "2,500 virtual coins credited. Your wallet & ranks sync live via Firebase.",
                        "referenceId" to "welcome_bonus",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val lbRef = db.collection("leaderboard").document(uid)
                val lbPayload = buildMap<String, Any?> {
                    put("userId", uid)
                    put("displayName", displayName)
                    put("publicUserId", publicId)
                    put("photoUrl", photoUrl)
                    put("totalVirtualPoints", 0L)
                    put("totalGames", 0L)
                    put("totalWins", 0L)
                    put("bestStreak", 0L)
                    put("badgeTitle", "Pulse Rookie")
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                batch.set(lbRef, lbPayload)

                batch.commit().await()
            }

            // If Main Admin logs in for the first time, bootstrap global config & starter QR vouchers
            if (isSuperAdminEmail(user.email)) {
                bootstrapGlobalConfigAndVouchersIfNeeded(email)
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun bootstrapGlobalConfigAndVouchersIfNeeded(adminEmail: String) {
        try {
            val cfgRef = db.collection("app_config").document("global")
            if (!cfgRef.get().await().exists()) {
                val defaults = AppConfigEntity()
                cfgRef.set(
                    mapOf(
                        "announcementText" to defaults.announcementText,
                        "redGreenMultiplier" to defaults.redGreenMultiplier,
                        "violetMultiplier" to defaults.violetMultiplier,
                        "mode30sEnabled" to defaults.mode30sEnabled,
                        "mode60sEnabled" to defaults.mode60sEnabled,
                        "mode3mEnabled" to defaults.mode3mEnabled,
                        "mode5mEnabled" to defaults.mode5mEnabled,
                        "adminQrLabel" to defaults.adminQrLabel,
                        "adminQrPayload" to defaults.adminQrPayload,
                        "updatedByEmail" to adminEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val v1Ref = db.collection("virtual_vouchers").document("CP-WELCOME-500")
            if (!v1Ref.get().await().exists()) {
                v1Ref.set(
                    mapOf(
                        "code" to "CP-WELCOME-500",
                        "coinsAmount" to 500L,
                        "label" to "Starter Gift QR Voucher",
                        "publicVisible" to true,
                        "redeemed" to false,
                        "createdByEmail" to adminEmail,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "app_config/global")
        }
    }

    // ---- Gameplay Operations ----

    suspend fun submitPrediction(
        mode: GameMode,
        periodId: String,
        selectedColor: PredictionColor,
        stakeCoins: Long
    ): Result<Unit> {
        var path = "users/predictions"
        return try {
            val uid = requireUserId()
            require(stakeCoins in ColorPredictionEngine.MIN_STAKE_COINS..ColorPredictionEngine.MAX_STAKE_COINS) {
                "Virtual stake must be between ${ColorPredictionEngine.MIN_STAKE_COINS} and ${ColorPredictionEngine.MAX_STAKE_COINS} VC."
            }
            val predId = ColorPredictionEngine.buildRoundDocId(mode, periodId)
            path = "users/$uid/predictions/$predId"

            val configSnap = db.collection("app_config").document("global").get().await()
            val config = configSnap.toObject(AppConfigEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: AppConfigEntity()
            val modeEnabled = when (mode) {
                GameMode.MODE_30S -> config.mode30sEnabled
                GameMode.MODE_60S -> config.mode60sEnabled
                GameMode.MODE_3M -> config.mode3mEnabled
                GameMode.MODE_5M -> config.mode5mEnabled
            }
            if (!modeEnabled) {
                throw IllegalStateException("${mode.title} is currently paused by Admin.")
            }

            val userRef = db.collection("users").document(uid)
            val predRef = userRef.collection("predictions").document(predId)
            val txRef = userRef.collection("wallet_transactions").document("bet_$predId")
            val lbRef = db.collection("leaderboard").document(uid)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not initialized yet.")
                if (user.accountStatus == "SUSPENDED") {
                    throw IllegalStateException("This demo account is paused by Admin.")
                }
                if (tx.get(predRef).exists()) {
                    throw IllegalStateException("Prediction already locked for period $periodId.")
                }
                if (user.virtualCoins < stakeCoins) {
                    throw IllegalStateException("Insufficient virtual coins. Claim daily bonus or refill.")
                }

                val newBalance = user.virtualCoins - stakeCoins
                val nextGames = user.totalGames + 1L
                val lbSnap = tx.get(lbRef)

                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "totalGames" to nextGames,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    predRef,
                    mapOf(
                        "userId" to uid,
                        "modeId" to mode.id,
                        "periodId" to periodId,
                        "selectedColor" to selectedColor.name,
                        "stakeCoins" to stakeCoins,
                        "status" to "PENDING",
                        "rewardCoins" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "PREDICTION_ENTRY",
                        "amount" to -stakeCoins,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to periodId,
                        "description" to "${mode.shortLabel} prediction: ${selectedColor.displayName} ($periodId)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "totalGames" to nextGames,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordCompletedRoundIfAbsent(outcome: VerifiableRoundOutcome): Result<Unit> {
        val path = "game_rounds/${outcome.roundDocId}"
        return try {
            requireUserId()
            val roundRef = db.collection("game_rounds").document(outcome.roundDocId)
            if (!roundRef.get().await().exists()) {
                roundRef.set(
                    mapOf(
                        "modeId" to outcome.mode.id,
                        "periodId" to outcome.periodId,
                        "startTimeMs" to outcome.startTimeMs,
                        "endTimeMs" to outcome.endTimeMs,
                        "winningColor" to outcome.winningColor.name,
                        "resultDigit" to outcome.resultDigit.toLong(),
                        "resultHash" to outcome.resultHash,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun settlePrediction(
        prediction: PredictionRecord,
        outcome: VerifiableRoundOutcome
    ): Result<Pair<Long, VerifiableRoundOutcome>> {
        var path = "users/predictions"
        return try {
            val uid = requireUserId()
            val predId = prediction.id.ifBlank {
                ColorPredictionEngine.buildRoundDocId(outcome.mode, prediction.periodId)
            }
            path = "users/$uid/predictions/$predId"

            val configSnap = db.collection("app_config").document("global").get().await()
            val config = configSnap.toObject(AppConfigEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: AppConfigEntity()

            val effectiveOutcome = if (!config.forcedNextColor.isNullOrBlank()) {
                val forcedColor = PredictionColor.fromRaw(config.forcedNextColor)
                val forcedDigit = when (forcedColor) {
                    PredictionColor.VIOLET -> 5
                    PredictionColor.GREEN -> 7
                    PredictionColor.RED -> 4
                }
                outcome.copy(winningColor = forcedColor, resultDigit = forcedDigit)
            } else {
                outcome
            }

            val userRef = db.collection("users").document(uid)
            val predRef = userRef.collection("predictions").document(predId)
            val winTxRef = userRef.collection("wallet_transactions").document("win_$predId")
            val notifRef = userRef.collection("notifications").document("res_$predId")
            val lbRef = db.collection("leaderboard").document(uid)

            val reward = db.runTransaction { tx ->
                val predSnap = tx.get(predRef)
                val currentPred = predSnap.toObject(PredictionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@runTransaction 0L
                if (currentPred.status != "PENDING") return@runTransaction 0L

                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@runTransaction 0L
                val lbSnap = tx.get(lbRef)

                val selectedColor = PredictionColor.fromRaw(currentPred.selectedColor)
                val multiplier = when (selectedColor) {
                    PredictionColor.VIOLET -> config.violetMultiplier
                    PredictionColor.RED, PredictionColor.GREEN -> config.redGreenMultiplier
                }
                val wonCoins = if (selectedColor == effectiveOutcome.winningColor) {
                    (currentPred.stakeCoins.toDouble() * multiplier).roundToLong().coerceAtMost(100_000L)
                } else {
                    0L
                }
                val didWin = wonCoins > 0L

                val nextCoins = user.virtualCoins + wonCoins
                val nextWins = if (didWin) user.totalWins + 1L else user.totalWins
                val nextPoints = user.totalVirtualPoints + wonCoins
                val nextStreak = if (didWin) user.currentStreak + 1L else 0L
                val nextBestStreak = maxOf(user.bestStreak, nextStreak)
                val nextBadge = ColorPredictionEngine.resolveBadgeTitle(nextPoints, nextWins, nextBestStreak)

                tx.update(
                    predRef,
                    mapOf(
                        "status" to if (didWin) "WON" else "LOST",
                        "winningColor" to effectiveOutcome.winningColor.name,
                        "rewardCoins" to wonCoins,
                        "resultHash" to effectiveOutcome.resultHash,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to nextCoins,
                        "totalWins" to nextWins,
                        "totalVirtualPoints" to nextPoints,
                        "currentStreak" to nextStreak,
                        "bestStreak" to nextBestStreak,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "displayName" to user.displayName,
                            "totalVirtualPoints" to nextPoints,
                            "totalGames" to maxOf(user.totalGames, nextWins),
                            "totalWins" to nextWins,
                            "bestStreak" to nextBestStreak,
                            "badgeTitle" to nextBadge,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val lbCreate = buildMap<String, Any?> {
                        put("userId", uid)
                        put("displayName", user.displayName)
                        put("publicUserId", user.publicUserId)
                        put("photoUrl", user.photoUrl)
                        put("totalVirtualPoints", nextPoints)
                        put("totalGames", maxOf(user.totalGames, nextWins))
                        put("totalWins", nextWins)
                        put("bestStreak", nextBestStreak)
                        put("badgeTitle", nextBadge)
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }.filterValues { it != null }
                    tx.set(lbRef, lbCreate)
                }

                if (didWin) {
                    tx.set(
                        winTxRef,
                        mapOf(
                            "userId" to uid,
                            "txType" to "PREDICTION_REWARD",
                            "amount" to wonCoins,
                            "previousBalance" to user.virtualCoins,
                            "newBalance" to nextCoins,
                            "referenceId" to effectiveOutcome.periodId,
                            "description" to "Won ${effectiveOutcome.mode.shortLabel} (${effectiveOutcome.winningColor.displayName})",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (user.notifyResults) {
                    tx.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "category" to if (didWin) "REWARD" else "ROUND_RESULT",
                            "title" to if (didWin) "+$wonCoins Virtual Coins Won!" else "Round ${effectiveOutcome.periodId} Completed",
                            "message" to if (didWin) {
                                "Your ${selectedColor.displayName} pick matched ${effectiveOutcome.winningColor.displayName} (#${effectiveOutcome.resultDigit})."
                            } else {
                                "${effectiveOutcome.mode.title} landed on ${effectiveOutcome.winningColor.displayName} (#${effectiveOutcome.resultDigit})."
                            },
                            "referenceId" to effectiveOutcome.periodId,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                wonCoins
            }.await()

            Result.success(reward to effectiveOutcome)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun playMiniGameRound(
        gameTitle: String,
        stakeCoins: Long,
        rewardCoins: Long,
        summary: String
    ): Result<Long> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(stakeCoins >= 0L && rewardCoins >= 0L) { "Invalid virtual coin values." }
            path = "users/$uid"
            val now = System.currentTimeMillis()
            val txBase = "mini_${now}_${Random.nextInt(100, 999)}"
            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)

            val netDelta = db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not ready.")
                if (user.accountStatus == "SUSPENDED") {
                    throw IllegalStateException("This demo account is paused by Admin.")
                }
                if (user.virtualCoins < stakeCoins) {
                    throw IllegalStateException("Not enough virtual coins. Claim Daily Bonus or Demo Refill.")
                }
                val lbSnap = tx.get(lbRef)

                val afterStake = user.virtualCoins - stakeCoins
                val finalBalance = afterStake + rewardCoins
                val didWin = rewardCoins > stakeCoins
                val nextGames = user.totalGames + 1L
                val nextWins = if (didWin) user.totalWins + 1L else user.totalWins
                val nextPoints = user.totalVirtualPoints + rewardCoins
                val nextStreak = if (didWin) user.currentStreak + 1L else 0L
                val nextBestStreak = maxOf(user.bestStreak, nextStreak)
                val nextBadge = ColorPredictionEngine.resolveBadgeTitle(nextPoints, nextWins, nextBestStreak)

                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to finalBalance,
                        "totalGames" to nextGames,
                        "totalWins" to nextWins,
                        "totalVirtualPoints" to nextPoints,
                        "currentStreak" to nextStreak,
                        "bestStreak" to nextBestStreak,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (stakeCoins > 0L) {
                    tx.set(
                        userRef.collection("wallet_transactions").document("${txBase}_in"),
                        mapOf(
                            "userId" to uid,
                            "txType" to "MINI_GAME_ENTRY",
                            "amount" to -stakeCoins,
                            "previousBalance" to user.virtualCoins,
                            "newBalance" to afterStake,
                            "referenceId" to gameTitle.take(60),
                            "description" to "$gameTitle entry ($summary)".take(220),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (rewardCoins > 0L) {
                    tx.set(
                        userRef.collection("wallet_transactions").document("${txBase}_out"),
                        mapOf(
                            "userId" to uid,
                            "txType" to "MINI_GAME_REWARD",
                            "amount" to rewardCoins,
                            "previousBalance" to afterStake,
                            "newBalance" to finalBalance,
                            "referenceId" to gameTitle.take(60),
                            "description" to "$gameTitle win: $summary".take(220),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "displayName" to user.displayName,
                            "totalVirtualPoints" to nextPoints,
                            "totalGames" to nextGames,
                            "totalWins" to nextWins,
                            "bestStreak" to nextBestStreak,
                            "badgeTitle" to nextBadge,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                rewardCoins - stakeCoins
            }.await()

            Result.success(netDelta)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun redeemVoucherCode(rawCode: String): Result<Long> {
        val cleanCode = rawCode.trim().uppercase(Locale.US)
        val path = "virtual_vouchers/$cleanCode"
        return try {
            val uid = requireUserId()
            require(cleanCode.matches(Regex("^[A-Z0-9\\-]{4,40}$"))) {
                "Enter a valid voucher code (letters, digits, dashes)."
            }
            val voucherRef = db.collection("virtual_vouchers").document(cleanCode)
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("wallet_transactions").document("voucher_$cleanCode")
            val notifRef = userRef.collection("notifications").document("notif_voucher_$cleanCode")

            val credited = db.runTransaction { tx ->
                val voucherSnap = tx.get(voucherRef)
                val voucher = voucherSnap.toObject(VirtualVoucherEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Voucher '$cleanCode' not found.")
                if (voucher.redeemed) {
                    throw IllegalStateException("Voucher '$cleanCode' has already been redeemed.")
                }
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not ready.")
                val newBalance = user.virtualCoins + voucher.coinsAmount

                tx.update(
                    voucherRef,
                    mapOf(
                        "redeemed" to true,
                        "redeemedByUid" to uid,
                        "redeemedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "QR_VOUCHER_REDEEM",
                        "amount" to voucher.coinsAmount,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to cleanCode,
                        "description" to "Redeemed QR Voucher: ${voucher.label}".take(220),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "category" to "DAILY_BONUS",
                        "title" to "QR Voucher Redeemed!",
                        "message" to "+${voucher.coinsAmount} virtual coins added via code $cleanCode.",
                        "referenceId" to cleanCode,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                voucher.coinsAmount
            }.await()

            Result.success(credited)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ---- Co-Admin Password Login & Super Admin Co-Admin Management ----

    suspend fun unlockCoAdminWithPassword(rawPassword: String): Result<String> {
        var path = "co_admin_invites"
        return try {
            val user = auth.currentUser ?: throw IllegalStateException("Sign in with Google first.")
            val trimmed = rawPassword.trim()
            require(trimmed.length >= 4) { "Password must be at least 4 characters." }
            val inviteHash = sha256Hex(trimmed)
            path = "co_admin_invites/$inviteHash"

            val inviteSnap = db.collection("co_admin_invites").document(inviteHash).get().await()
            if (!inviteSnap.exists()) {
                throw IllegalStateException("Incorrect password. Ask the Main Admin for an active Co-Admin password.")
            }
            val active = inviteSnap.getBoolean("active") == true
            if (!active) {
                throw IllegalStateException("This Co-Admin password has been revoked by Main Admin.")
            }
            val label = inviteSnap.getString("label").orEmpty().ifBlank { "Co-Admin" }.take(60)

            path = "co_admins/${user.uid}"
            val grantRef = db.collection("co_admins").document(user.uid)
            val existingGrant = grantRef.get().await()
            val displayName = (user.displayName?.trim()?.take(60)).orEmpty().ifBlank { "Co-Admin User" }

            if (existingGrant.exists()) {
                grantRef.update(
                    mapOf(
                        "displayName" to displayName,
                        "label" to label,
                        "inviteHash" to inviteHash,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                grantRef.set(
                    mapOf(
                        "userId" to user.uid,
                        "email" to user.email.orEmpty().take(120),
                        "displayName" to displayName,
                        "label" to label,
                        "inviteHash" to inviteHash,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            writeAdminAuditLog("CO_ADMIN_LOGIN", "Unlocked Co-Admin access via passkey '$label'", "CO_ADMIN")
            Result.success(label)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun superAdminCreateCoAdminPassword(label: String, rawPassword: String): Result<String> {
        var path = "co_admin_invites"
        return try {
            requireUserId()
            require(isSuperAdminEmail()) { "Only Main Admin can create Co-Admin passwords." }
            val cleanLabel = label.trim().take(60).ifBlank { "Co-Admin Passkey" }
            val cleanPass = rawPassword.trim()
            require(cleanPass.length in 6..48) { "Password must be 6–48 characters." }

            val inviteHash = sha256Hex(cleanPass)
            path = "co_admin_invites/$inviteHash"
            val docRef = db.collection("co_admin_invites").document(inviteHash)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(
                    mapOf(
                        "label" to cleanLabel,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "label" to cleanLabel,
                        "active" to true,
                        "createdByEmail" to currentUserEmail(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAdminAuditLog("CREATE_CO_ADMIN_PASS", "Created Co-Admin passkey '$cleanLabel'", "SUPER_ADMIN")
            Result.success(cleanPass)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun superAdminToggleCoAdminInvite(inviteHash: String, active: Boolean, label: String): Result<Unit> {
        val path = "co_admin_invites/$inviteHash"
        return try {
            requireUserId()
            require(isSuperAdminEmail()) { "Only Main Admin can manage Co-Admin passkeys." }
            db.collection("co_admin_invites").document(inviteHash).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAdminAuditLog(
                if (active) "ENABLE_CO_ADMIN_PASS" else "REVOKE_CO_ADMIN_PASS",
                "${if (active) "Re-enabled" else "Revoked"} Co-Admin passkey '$label'",
                "SUPER_ADMIN"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun superAdminDeleteCoAdminInvite(inviteHash: String, label: String): Result<Unit> {
        val path = "co_admin_invites/$inviteHash"
        return try {
            requireUserId()
            require(isSuperAdminEmail()) { "Only Main Admin can delete Co-Admin passkeys." }
            db.collection("co_admin_invites").document(inviteHash).delete().await()
            writeAdminAuditLog("DELETE_CO_ADMIN_PASS", "Deleted Co-Admin passkey '$label'", "SUPER_ADMIN")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun superAdminRevokeCoAdminUser(targetUid: String, email: String): Result<Unit> {
        val path = "co_admins/$targetUid"
        return try {
            requireUserId()
            require(isSuperAdminEmail()) { "Only Main Admin can remove Co-Admins." }
            db.collection("co_admins").document(targetUid).delete().await()
            writeAdminAuditLog("REVOKE_CO_ADMIN", "Removed Co-Admin access for $email", "SUPER_ADMIN")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ---- Cloud Admin Panel Actions (Main Admin & Active Co-Admins) ----

    suspend fun adminUpdateConfig(config: AppConfigEntity, auditSummary: String, actorRole: String): Result<Unit> {
        val path = "app_config/global"
        return try {
            requireUserId()
            val cfgRef = db.collection("app_config").document("global")
            val payload = buildMap<String, Any?> {
                put("announcementText", config.announcementText.trim().take(240).ifBlank { "ChromaPulse Live Arena" })
                put("redGreenMultiplier", config.redGreenMultiplier.coerceIn(1.1, 5.0))
                put("violetMultiplier", config.violetMultiplier.coerceIn(2.0, 10.0))
                put("mode30sEnabled", config.mode30sEnabled)
                put("mode60sEnabled", config.mode60sEnabled)
                put("mode3mEnabled", config.mode3mEnabled)
                put("mode5mEnabled", config.mode5mEnabled)
                put("forcedNextColor", config.forcedNextColor)
                put("adminQrLabel", config.adminQrLabel.trim().take(80).ifBlank { "ChromaPulse Counter" })
                put("adminQrPayload", config.adminQrPayload.trim().take(200).ifBlank { "CHROMAPULSE:DEMO" })
                put("updatedByEmail", currentUserEmail())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (cfgRef.get().await().exists()) {
                cfgRef.update(payload).await()
            } else {
                cfgRef.set(payload.filterValues { it != null }).await()
            }
            writeAdminAuditLog("CONFIG_UPDATE", auditSummary, actorRole)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminAdjustUserCoins(
        targetUserId: String,
        deltaCoins: Long,
        reason: String,
        actorRole: String
    ): Result<Long> {
        val path = "users/$targetUserId"
        return try {
            requireUserId()
            require(deltaCoins != 0L) { "Adjustment amount cannot be zero." }
            val now = System.currentTimeMillis()
            val cleanReason = reason.trim().take(200).ifBlank {
                if (deltaCoins > 0) "Admin virtual demo credit" else "Admin virtual demo debit"
            }
            val userRef = db.collection("users").document(targetUserId)

            val (nextBalance, publicUserId, appliedDelta) = db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player profile not found.")
                val updatedCoins = (user.virtualCoins + deltaCoins).coerceIn(0L, 100_000_000L)
                val applied = updatedCoins - user.virtualCoins

                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to updatedCoins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("wallet_transactions").document("admin_adj_$now"),
                    mapOf(
                        "userId" to targetUserId,
                        "txType" to if (applied >= 0L) "ADMIN_CREDIT" else "ADMIN_DEBIT",
                        "amount" to applied,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to updatedCoins,
                        "referenceId" to "ADMIN",
                        "description" to cleanReason,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("notifications").document("notif_admin_$now"),
                    mapOf(
                        "userId" to targetUserId,
                        "category" to "ANNOUNCEMENT",
                        "title" to if (applied >= 0L) "Admin Credited +$applied VC" else "Admin Adjusted $applied VC",
                        "message" to cleanReason,
                        "referenceId" to "ADMIN",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Triple(updatedCoins, user.publicUserId, applied)
            }.await()

            writeAdminAuditLog(
                if (appliedDelta >= 0L) "CREDIT_COINS" else "DEBIT_COINS",
                "${if (appliedDelta >= 0L) "+" else ""}$appliedDelta VC to $publicUserId: $cleanReason",
                actorRole
            )
            Result.success(nextBalance)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminCreateVoucher(coinsAmount: Long, label: String, actorRole: String): Result<VirtualVoucherEntity> {
        val randomSuffix = Random.nextInt(1000, 9999)
        val code = "CP-${coinsAmount}VC-$randomSuffix"
        val path = "virtual_vouchers/$code"
        return try {
            requireUserId()
            require(coinsAmount in 50L..50_000L) { "Voucher amount must be 50–50,000 VC." }
            val cleanLabel = label.trim().take(80).ifBlank { "Admin QR Gift Voucher" }
            val email = currentUserEmail()

            db.collection("virtual_vouchers").document(code).set(
                mapOf(
                    "code" to code,
                    "coinsAmount" to coinsAmount,
                    "label" to cleanLabel,
                    "publicVisible" to true,
                    "redeemed" to false,
                    "createdByEmail" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            writeAdminAuditLog("CREATE_QR_VOUCHER", "Generated $code (+$coinsAmount VC)", actorRole)
            Result.success(
                VirtualVoucherEntity(
                    id = code,
                    code = code,
                    coinsAmount = coinsAmount,
                    label = cleanLabel,
                    publicVisible = true,
                    redeemed = false,
                    createdByEmail = email
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun writeAdminAuditLog(action: String, details: String, actorRole: String) {
        val uid = auth.currentUser?.uid ?: return
        val logId = "adm_${System.currentTimeMillis()}_${Random.nextInt(100, 999)}"
        try {
            db.collection("admin_audit_logs").document(logId).set(
                mapOf(
                    "actorUid" to uid,
                    "actorEmail" to currentUserEmail().take(120),
                    "actorRole" to if (actorRole == "SUPER_ADMIN") "SUPER_ADMIN" else "CO_ADMIN",
                    "action" to action.take(60),
                    "details" to details.take(300),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "admin_audit_logs/$logId")
        }
    }

    // ---- Bonuses, Profile Settings & Notifications ----

    suspend fun claimDailyBonus(utcIsoDate: String): Result<Long> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val compactDate = utcIsoDate.replace("-", "")
            val txId = "daily_$compactDate"
            val bonusAmount = ColorPredictionEngine.DAILY_BONUS_COINS
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("wallet_transactions").document(txId)
            val notifRef = userRef.collection("notifications").document("notif_$txId")

            db.runTransaction { tx ->
                val user = tx.get(userRef)
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not ready.")
                if (user.lastDailyBonusDate == utcIsoDate || tx.get(txRef).exists()) {
                    throw IllegalStateException("Daily virtual coin bonus already claimed today.")
                }
                val newCoins = user.virtualCoins + bonusAmount
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newCoins,
                        "lastDailyBonusDate" to utcIsoDate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "DAILY_BONUS",
                        "amount" to bonusAmount,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newCoins,
                        "referenceId" to "daily_$utcIsoDate",
                        "description" to "Daily virtual coin login reward",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (user.notifyBonuses) {
                    tx.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "category" to "DAILY_BONUS",
                            "title" to "Daily Bonus Credited",
                            "message" to "+$bonusAmount virtual coins added to your cloud wallet for $utcIsoDate.",
                            "referenceId" to "daily_$utcIsoDate",
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(bonusAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDemoRefill(): Result<Long> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val refillAmount = ColorPredictionEngine.DEMO_REFILL_COINS
            val threshold = ColorPredictionEngine.DEMO_REFILL_THRESHOLD
            val now = System.currentTimeMillis()
            val txId = "refill_$now"
            val userRef = db.collection("users").document(uid)

            db.runTransaction { tx ->
                val user = tx.get(userRef)
                    .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not ready.")
                if (user.virtualCoins >= threshold) {
                    throw IllegalStateException("Free demo refill unlocks when your balance is below $threshold VC.")
                }
                val newCoins = user.virtualCoins + refillAmount
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newCoins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("wallet_transactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "txType" to "DEMO_REFILL",
                        "amount" to refillAmount,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newCoins,
                        "referenceId" to txId,
                        "description" to "Free virtual demo coin refill (entertainment only)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                if (user.notifyBonuses) {
                    tx.set(
                        userRef.collection("notifications").document("notif_$txId"),
                        mapOf(
                            "userId" to uid,
                            "category" to "DAILY_BONUS",
                            "title" to "Demo Wallet Refilled",
                            "message" to "+$refillAmount virtual demo coins credited so you can keep playing.",
                            "referenceId" to txId,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(refillAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileSettings(
        displayName: String,
        notifyResults: Boolean,
        notifyRoundClosing: Boolean,
        notifyBonuses: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotBlank()) { "Display name cannot be empty." }
            path = "users/$uid"

            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)

            userRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "notifyResults" to notifyResults,
                    "notifyRoundClosing" to notifyRoundClosing,
                    "notifyBonuses" to notifyBonuses,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (lbRef.get().await().exists()) {
                lbRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        var path = "users/notifications"
        return try {
            val uid = requireUserId()
            require(notificationId.isNotBlank())
            path = "users/$uid/notifications/$notificationId"
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update(
                    mapOf(
                        "read" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(): Result<Unit> {
        var path = "users/notifications"
        return try {
            val uid = requireUserId()
            path = "users/$uid/notifications"
            val unread = db.collection("users").document(uid)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .await()
            if (!unread.isEmpty) {
                val batch = db.batch()
                unread.documents.forEach { doc ->
                    batch.update(
                        doc.reference,
                        mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
                    )
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        var path = "users/notifications"
        return try {
            val uid = requireUserId()
            require(notificationId.isNotBlank())
            path = "users/$uid/notifications/$notificationId"
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val SUPER_ADMIN_EMAIL = "buildolive4@gmail.com"

        fun sha256Hex(raw: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
