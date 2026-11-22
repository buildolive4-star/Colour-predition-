package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.VerifiableRoundOutcome
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChromaPulseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .logFirestoreErrors(uid, OperationType.GET, path)
        )
    }

    fun observePredictions(userId: String): Flow<List<PredictionRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/predictions"
        emitAll(
            db.collection("users").document(uid).collection("predictions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PredictionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeWalletTransactions(userId: String): Flow<List<WalletTransaction>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/wallet_transactions"
        emitAll(
            db.collection("users").document(uid).collection("wallet_transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeGameRounds(userId: String): Flow<List<GameRoundRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "game_rounds"
        emitAll(
            db.collection("game_rounds")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(160)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameRoundRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.endTimeMs }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    fun observeLeaderboard(userId: String): Flow<List<LeaderboardEntry>> = flow {
        val uid = requireUserId(userId)
        val path = "leaderboard"
        emitAll(
            db.collection("leaderboard")
                .whereEqualTo("visibility", "PUBLIC")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<LeaderboardEntry> { it.totalVirtualPoints }
                                .thenByDescending { it.totalWins }
                                .thenByDescending { it.bestStreak }
                        )
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    suspend fun ensureUserInitialized(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val userPath = "users/$uid"
        return try {
            requireUserId(uid)
            val userRef = db.collection("users").document(uid)
            val leaderboardRef = db.collection("leaderboard").document(uid)
            val welcomeTxRef = userRef.collection("wallet_transactions").document("welcome_bonus")
            val welcomeNotifRef = userRef.collection("notifications").document("welcome_notice")

            val displayName = (user.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Chroma Player").take(60)
            val email = (user.email?.trim() ?: "").take(120)
            val photoUrl = user.photoUrl?.toString()?.takeIf { it.isNotBlank() && it.length <= 500 }
            val publicUserId = ColorPredictionEngine.buildPublicUserId(uid)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val lbSnap = tx.get(leaderboardRef)

                if (!userSnap.exists()) {
                    val initialCoins = ColorPredictionEngine.INITIAL_DEMO_COINS
                    val profilePayload = buildMap<String, Any> {
                        put("userId", uid)
                        put("displayName", displayName)
                        put("email", email)
                        put("publicUserId", publicUserId)
                        if (photoUrl != null) put("photoUrl", photoUrl)
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
                    }
                    tx.set(userRef, profilePayload)

                    val txPayload = mapOf(
                        "userId" to uid,
                        "txType" to "WELCOME_BONUS",
                        "amount" to initialCoins,
                        "previousBalance" to 0L,
                        "newBalance" to initialCoins,
                        "referenceId" to "welcome_bonus",
                        "description" to "Welcome virtual demo coins (entertainment only)",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(welcomeTxRef, txPayload)

                    val notifPayload = mapOf(
                        "userId" to uid,
                        "category" to "ANNOUNCEMENT",
                        "title" to "Welcome to ChromaPulse!",
                        "message" to "2,500 virtual coins credited to your demo wallet. Pick Red, Green, or Violet in live timed rounds.",
                        "referenceId" to "welcome_bonus",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(welcomeNotifRef, notifPayload)
                }

                if (!lbSnap.exists()) {
                    val lbPayload = buildMap<String, Any> {
                        put("userId", uid)
                        put("displayName", displayName)
                        put("publicUserId", publicUserId)
                        if (photoUrl != null) put("photoUrl", photoUrl)
                        put("totalVirtualPoints", 0L)
                        put("totalGames", 0L)
                        put("totalWins", 0L)
                        put("bestStreak", 0L)
                        put("badgeTitle", "Pulse Rookie")
                        put("visibility", "PUBLIC")
                        put("createdAt", FieldValue.serverTimestamp())
                        put("updatedAt", FieldValue.serverTimestamp())
                    }
                    tx.set(leaderboardRef, lbPayload)
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    suspend fun submitPrediction(
        userId: String,
        mode: GameMode,
        periodId: String,
        selectedColor: PredictionColor,
        stakeCoins: Long
    ): Result<Unit> {
        val predId = ColorPredictionEngine.buildRoundDocId(mode, periodId)
        val path = "users/$userId/predictions/$predId"
        return try {
            val uid = requireUserId(userId)
            require(stakeCoins in ColorPredictionEngine.MIN_STAKE_COINS..ColorPredictionEngine.MAX_STAKE_COINS) {
                "Virtual stake must be between ${ColorPredictionEngine.MIN_STAKE_COINS} and ${ColorPredictionEngine.MAX_STAKE_COINS} coins."
            }

            val userRef = db.collection("users").document(uid)
            val predRef = userRef.collection("predictions").document(predId)
            val txRef = userRef.collection("wallet_transactions").document("bet_$predId")
            val lbRef = db.collection("leaderboard").document(uid)

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) error("Player wallet not initialized yet.")
                val existingPred = tx.get(predRef)
                if (existingPred.exists()) {
                    throw IllegalStateException("Prediction already locked for period $periodId.")
                }

                val currentBalance = userSnap.getLong("virtualCoins") ?: 0L
                val totalGames = userSnap.getLong("totalGames") ?: 0L
                if (currentBalance < stakeCoins) {
                    throw IllegalStateException("Insufficient virtual coins. Claim daily bonus or choose a smaller stake.")
                }

                val newBalance = currentBalance - stakeCoins
                val lbSnap = tx.get(lbRef)

                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "totalGames" to (totalGames + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val predPayload = mapOf(
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
                tx.set(predRef, predPayload)

                val walletTxPayload = mapOf(
                    "userId" to uid,
                    "txType" to "PREDICTION_ENTRY",
                    "amount" to -stakeCoins,
                    "previousBalance" to currentBalance,
                    "newBalance" to newBalance,
                    "referenceId" to periodId,
                    "description" to "${mode.shortLabel} prediction: ${selectedColor.displayName} ($periodId)",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                tx.set(txRef, walletTxPayload)

                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "totalGames" to (totalGames + 1L),
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
            val snap = roundRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "modeId" to outcome.mode.id,
                    "periodId" to outcome.periodId,
                    "startTimeMs" to outcome.startTimeMs,
                    "endTimeMs" to outcome.endTimeMs,
                    "winningColor" to outcome.winningColor.name,
                    "resultDigit" to outcome.resultDigit.toLong(),
                    "resultHash" to outcome.resultHash,
                    "status" to "COMPLETED",
                    "visibility" to "PUBLIC",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                roundRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            // If another player created this immutable round concurrently, treat as idempotent success.
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS
            ) {
                Result.success(Unit)
            } else {
                handleFirestoreError(e, OperationType.CREATE, path)
                Result.failure(e)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun settlePrediction(
        userId: String,
        prediction: PredictionRecord,
        outcome: VerifiableRoundOutcome
    ): Result<Long> {
        val predId = prediction.id.ifBlank {
            ColorPredictionEngine.buildRoundDocId(outcome.mode, prediction.periodId)
        }
        val path = "users/$userId/predictions/$predId"
        return try {
            val uid = requireUserId(userId)
            val userRef = db.collection("users").document(uid)
            val predRef = userRef.collection("predictions").document(predId)
            val rewardTxRef = userRef.collection("wallet_transactions").document("win_$predId")
            val notifRef = userRef.collection("notifications").document("res_$predId")
            val lbRef = db.collection("leaderboard").document(uid)

            val awardedCoins = db.runTransaction { tx ->
                val predSnap = tx.get(predRef)
                if (!predSnap.exists()) return@runTransaction 0L
                val status = predSnap.getString("status") ?: "PENDING"
                if (status != "PENDING") return@runTransaction 0L

                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) return@runTransaction 0L
                val lbSnap = tx.get(lbRef)

                val selectedColor = PredictionColor.fromRaw(predSnap.getString("selectedColor"))
                val stake = predSnap.getLong("stakeCoins") ?: 0L
                val reward = ColorPredictionEngine.calculateRewardCoins(
                    selectedColor = selectedColor,
                    winningColor = outcome.winningColor,
                    stakeCoins = stake
                )
                val didWin = reward > 0L

                val currentCoins = userSnap.getLong("virtualCoins") ?: 0L
                val totalWins = userSnap.getLong("totalWins") ?: 0L
                val totalGames = userSnap.getLong("totalGames") ?: 0L
                val totalPoints = userSnap.getLong("totalVirtualPoints") ?: 0L
                val currentStreak = userSnap.getLong("currentStreak") ?: 0L
                val bestStreak = userSnap.getLong("bestStreak") ?: 0L
                val notifyResults = userSnap.getBoolean("notifyResults") ?: true

                val nextCoins = currentCoins + reward
                val nextWins = if (didWin) totalWins + 1L else totalWins
                val nextPoints = totalPoints + reward
                val nextStreak = if (didWin) currentStreak + 1L else 0L
                val nextBestStreak = maxOf(bestStreak, nextStreak)
                val nextBadge = ColorPredictionEngine.resolveBadgeTitle(nextPoints, nextWins, nextBestStreak)

                tx.update(
                    predRef,
                    mapOf(
                        "status" to if (didWin) "WON" else "LOST",
                        "winningColor" to outcome.winningColor.name,
                        "rewardCoins" to reward,
                        "resultHash" to outcome.resultHash,
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
                            "totalVirtualPoints" to nextPoints,
                            "totalGames" to maxOf(totalGames, nextWins),
                            "totalWins" to nextWins,
                            "bestStreak" to nextBestStreak,
                            "badgeTitle" to nextBadge,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (didWin) {
                    tx.set(
                        rewardTxRef,
                        mapOf(
                            "userId" to uid,
                            "txType" to "PREDICTION_REWARD",
                            "amount" to reward,
                            "previousBalance" to currentCoins,
                            "newBalance" to nextCoins,
                            "referenceId" to outcome.periodId,
                            "description" to "Won ${outcome.mode.shortLabel} (${outcome.winningColor.displayName} ${selectedColor.multiplierLabel})",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (notifyResults) {
                    tx.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "category" to if (didWin) "REWARD" else "ROUND_RESULT",
                            "title" to if (didWin) "+$reward Virtual Coins Won!" else "Round ${outcome.periodId} Completed",
                            "message" to if (didWin) {
                                "Your ${selectedColor.displayName} pick matched ${outcome.winningColor.displayName} (#${outcome.resultDigit}) in ${outcome.mode.title}."
                            } else {
                                "${outcome.mode.title} landed on ${outcome.winningColor.displayName} (#${outcome.resultDigit}). Your pick was ${selectedColor.displayName}."
                            },
                            "referenceId" to outcome.periodId,
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                reward
            }.await()

            Result.success(awardedCoins)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun claimDailyBonus(userId: String, utcIsoDate: String): Result<Long> {
        val compactDate = utcIsoDate.replace("-", "")
        val txPath = "users/$userId/wallet_transactions/daily_$compactDate"
        return try {
            val uid = requireUserId(userId)
            val bonusAmount = ColorPredictionEngine.DAILY_BONUS_COINS
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("wallet_transactions").document("daily_$compactDate")
            val notifRef = userRef.collection("notifications").document("daily_$compactDate")

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) error("Player wallet not ready.")
                val lastClaim = userSnap.getString("lastDailyBonusDate")
                val existingTx = tx.get(txRef)
                if (lastClaim == utcIsoDate || existingTx.exists()) {
                    throw IllegalStateException("Daily virtual coin bonus already claimed today.")
                }

                val currentCoins = userSnap.getLong("virtualCoins") ?: 0L
                val notifyBonuses = userSnap.getBoolean("notifyBonuses") ?: true
                val newCoins = currentCoins + bonusAmount

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
                        "previousBalance" to currentCoins,
                        "newBalance" to newCoins,
                        "referenceId" to "daily_$utcIsoDate",
                        "description" to "Daily virtual coin login reward",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                if (notifyBonuses) {
                    tx.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "category" to "DAILY_BONUS",
                            "title" to "Daily Bonus Credited",
                            "message" to "+$bonusAmount virtual coins added to your demo wallet for $utcIsoDate.",
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
            handleFirestoreError(e, OperationType.WRITE, txPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileSettings(
        userId: String,
        displayName: String,
        notifyResults: Boolean,
        notifyRoundClosing: Boolean,
        notifyBonuses: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanName = displayName.trim().take(60)
            require(cleanName.isNotBlank()) { "Display name cannot be empty." }

            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)

            db.runTransaction { tx ->
                val lbSnap = tx.get(lbRef)
                tx.update(
                    userRef,
                    mapOf(
                        "displayName" to cleanName,
                        "notifyResults" to notifyResults,
                        "notifyRoundClosing" to notifyRoundClosing,
                        "notifyBonuses" to notifyBonuses,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    tx.update(
                        lbRef,
                        mapOf(
                            "displayName" to cleanName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId(userId)
            require(notificationId.isNotBlank())
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .update(
                    mapOf(
                        "read" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> {
        val path = "users/$userId/notifications/$notificationId"
        return try {
            val uid = requireUserId(userId)
            require(notificationId.isNotBlank())
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
