package com.example.data

import android.content.Context
import com.example.R
import com.example.domain.BetType
import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.SizeChoice
import com.example.domain.VerifiableRoundOutcome
import com.example.domain.WinGoBetTarget
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
                .limit(150)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { doc -> parsePredictionDoc(doc) }
                        .sortedByDescending { pred ->
                            pred.createdAt?.toDate()?.time ?: Long.MAX_VALUE
                        }
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    private fun parsePredictionDoc(doc: DocumentSnapshot): PredictionRecord? {
        if (!doc.exists()) return null
        return try {
            PredictionRecord(
                id = doc.id,
                userId = doc.getString("userId").orEmpty(),
                modeId = doc.getString("modeId").orEmpty().ifBlank { "mode_30s" },
                periodId = doc.getString("periodId").orEmpty(),
                betType = doc.getString("betType").orEmpty().ifBlank { "COLOR" },
                betLabel = doc.getString("betLabel").orEmpty(),
                selectedColor = doc.getString("selectedColor").orEmpty().ifBlank { "GREEN" },
                selectedDigit = doc.getLong("selectedDigit")?.toInt(),
                selectedSize = doc.getString("selectedSize"),
                stakeCoins = doc.getLong("stakeCoins") ?: 0L,
                status = doc.getString("status").orEmpty().ifBlank { "PENDING" },
                winningColor = doc.getString("winningColor"),
                winningDigit = doc.getLong("winningDigit")?.toInt(),
                rewardCoins = doc.getLong("rewardCoins") ?: 0L,
                resultHash = doc.getString("resultHash"),
                createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
                updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun observeMyReferrals(userId: String): Flow<List<ReferralClaimEntity>> = flow {
        val uid = requireUserId()
        val path = "referrals"
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerUid", uid)
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ReferralClaimEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<ReferralClaimEntity> { it.referrerRewardClaimed }
                                .thenByDescending { it.createdAtMs }
                        )
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

    fun observeUserWalletRequests(userId: String): Flow<List<WalletRequestEntity>> = flow {
        val uid = requireUserId()
        val path = "wallet_requests"
        emitAll(
            db.collection("wallet_requests")
                .whereEqualTo("userId", uid)
                .limit(60)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletRequestEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAtMs }
                }
                .wrapFirestoreListener(uid, path, OperationType.LIST)
        )
    }

    fun observeAllWalletRequestsForAdmin(): Flow<List<WalletRequestEntity>> = flow {
        val uid = requireUserId()
        val path = "wallet_requests"
        emitAll(
            db.collection("wallet_requests")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletRequestEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<WalletRequestEntity> { if (it.status == "PENDING") 0 else 1 }
                                .thenByDescending { it.createdAtMs }
                        )
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

            // Always ensure public referral code directory entry is published for this user
            ensureReferralCodePublished(uid, publicId, displayName)

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

    private suspend fun ensureReferralCodePublished(uid: String, publicId: String, displayName: String) {
        val code = publicId.trim().uppercase(Locale.US)
        if (!code.matches(Regex("^[A-Z0-9\\-]{3,32}$"))) return
        try {
            val codeRef = db.collection("referral_codes").document(code)
            if (!codeRef.get().await().exists()) {
                codeRef.set(
                    mapOf(
                        "code" to code,
                        "referrerUid" to uid,
                        "referrerName" to displayName.take(60).ifBlank { "Chroma Player" },
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "referral_codes/$code")
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
        target: WinGoBetTarget,
        stakeCoins: Long
    ): Result<Unit> {
        var path = "users/predictions"
        return try {
            val uid = requireUserId()
            require(stakeCoins in ColorPredictionEngine.MIN_STAKE_COINS..ColorPredictionEngine.MAX_STAKE_COINS) {
                "Bet amount must be between ${ColorPredictionEngine.MIN_STAKE_COINS} and ${ColorPredictionEngine.MAX_STAKE_COINS} VC."
            }
            // Unique doc ID per bet enables unlimited multiple bets in the SAME round (same number, color, or Big/Small)
            val uniqueSuffix = "${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            val predId = "${ColorPredictionEngine.buildRoundDocId(mode, periodId)}_$uniqueSuffix"
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
                if (user.virtualCoins < stakeCoins) {
                    throw IllegalStateException("Insufficient balance! Please Deposit or claim Daily Bonus.")
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
                val predPayload = buildMap<String, Any?> {
                    put("userId", uid)
                    put("modeId", mode.id)
                    put("periodId", periodId)
                    put("betType", target.betType.name)
                    put("betLabel", target.label.take(40))
                    put("selectedColor", target.color.name)
                    put("selectedDigit", target.digit)
                    put("selectedSize", target.sizeChoice?.name)
                    put("stakeCoins", stakeCoins)
                    put("status", "PENDING")
                    put("rewardCoins", 0L)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }.filterValues { it != null }
                tx.set(predRef, predPayload)

                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "PREDICTION_ENTRY",
                        "amount" to -stakeCoins,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to periodId,
                        "description" to "${mode.shortLabel} bet: ${target.label} ($periodId)".take(220),
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

    fun resolveEffectiveOutcome(
        outcome: VerifiableRoundOutcome,
        config: AppConfigEntity
    ): VerifiableRoundOutcome {
        val forcedDigit = config.forcedDigitForMode(outcome.mode)
        if (forcedDigit != null) {
            return outcome.copy(
                winningColor = ColorPredictionEngine.digitToColor(forcedDigit),
                resultDigit = forcedDigit
            )
        }
        if (!config.forcedNextColor.isNullOrBlank()) {
            val forcedColor = PredictionColor.fromRaw(config.forcedNextColor)
            val fallbackDigit = when (forcedColor) {
                PredictionColor.VIOLET -> 5
                PredictionColor.GREEN -> 7
                PredictionColor.RED -> 4
            }
            return outcome.copy(
                winningColor = forcedColor,
                resultDigit = fallbackDigit
            )
        }
        return outcome
    }

    suspend fun recordCompletedRoundIfAbsent(
        outcome: VerifiableRoundOutcome,
        applyAdminConfig: Boolean = true
    ): Result<VerifiableRoundOutcome> {
        val path = "game_rounds/${outcome.roundDocId}"
        return try {
            requireUserId()
            val roundRef = db.collection("game_rounds").document(outcome.roundDocId)
            val existingSnap = roundRef.get().await()
            if (existingSnap.exists()) {
                val existingDigit = existingSnap.getLong("resultDigit")?.toInt()?.coerceIn(0, 9) ?: outcome.resultDigit
                val existingColor = existingSnap.getString("winningColor")?.let { PredictionColor.fromRaw(it) }
                    ?: ColorPredictionEngine.digitToColor(existingDigit)
                val existingHash = existingSnap.getString("resultHash").orEmpty().ifBlank { outcome.resultHash }
                return Result.success(
                    outcome.copy(
                        winningColor = existingColor,
                        resultDigit = existingDigit,
                        resultHash = existingHash
                    )
                )
            }

            val effectiveOutcome = if (applyAdminConfig) {
                val configSnap = db.collection("app_config").document("global").get().await()
                val config = configSnap.toObject(AppConfigEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: AppConfigEntity()
                resolveEffectiveOutcome(outcome, config)
            } else {
                outcome
            }

            roundRef.set(
                mapOf(
                    "modeId" to effectiveOutcome.mode.id,
                    "periodId" to effectiveOutcome.periodId,
                    "startTimeMs" to effectiveOutcome.startTimeMs,
                    "endTimeMs" to effectiveOutcome.endTimeMs,
                    "winningColor" to effectiveOutcome.winningColor.name,
                    "resultDigit" to effectiveOutcome.resultDigit.toLong(),
                    "resultHash" to effectiveOutcome.resultHash,
                    "status" to "COMPLETED",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(effectiveOutcome)
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

            // First honor the shared cloud game_rounds record (which captures Admin's exact forced number)
            val roundSnap = db.collection("game_rounds").document(outcome.roundDocId).get().await()
            val effectiveOutcome = if (roundSnap.exists()) {
                val cloudDigit = roundSnap.getLong("resultDigit")?.toInt()?.coerceIn(0, 9) ?: outcome.resultDigit
                val cloudColor = roundSnap.getString("winningColor")?.let { PredictionColor.fromRaw(it) }
                    ?: ColorPredictionEngine.digitToColor(cloudDigit)
                val cloudHash = roundSnap.getString("resultHash").orEmpty().ifBlank { outcome.resultHash }
                outcome.copy(winningColor = cloudColor, resultDigit = cloudDigit, resultHash = cloudHash)
            } else {
                val resolved = resolveEffectiveOutcome(outcome, config)
                // Persist authoritative round so Game history & all players see the exact same digit
                runCatching {
                    db.collection("game_rounds").document(resolved.roundDocId).set(
                        mapOf(
                            "modeId" to resolved.mode.id,
                            "periodId" to resolved.periodId,
                            "startTimeMs" to resolved.startTimeMs,
                            "endTimeMs" to resolved.endTimeMs,
                            "winningColor" to resolved.winningColor.name,
                            "resultDigit" to resolved.resultDigit.toLong(),
                            "resultHash" to resolved.resultHash,
                            "status" to "COMPLETED",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                resolved
            }

            val userRef = db.collection("users").document(uid)
            val predRef = userRef.collection("predictions").document(predId)
            val winTxRef = userRef.collection("wallet_transactions").document("win_$predId")
            val notifRef = userRef.collection("notifications").document("res_$predId")
            val lbRef = db.collection("leaderboard").document(uid)

            val reward = db.runTransaction { tx ->
                val predSnap = tx.get(predRef)
                val currentPred = parsePredictionDoc(predSnap) ?: return@runTransaction 0L
                if (currentPred.status != "PENDING") return@runTransaction 0L

                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: return@runTransaction 0L
                val lbSnap = tx.get(lbRef)

                val selectedColor = PredictionColor.fromRaw(currentPred.selectedColor)
                val betType = BetType.fromRaw(currentPred.betType)
                val selectedSize = SizeChoice.fromRaw(currentPred.selectedSize)
                val wonCoins = ColorPredictionEngine.calculateBetReward(
                    betType = betType,
                    selectedColor = selectedColor,
                    selectedDigit = currentPred.selectedDigit,
                    selectedSize = selectedSize,
                    winningColor = effectiveOutcome.winningColor,
                    winningDigit = effectiveOutcome.resultDigit,
                    stakeCoins = currentPred.stakeCoins,
                    redGreenMultiplier = config.redGreenMultiplier,
                    violetMultiplier = config.violetMultiplier
                )
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
                        "winningDigit" to effectiveOutcome.resultDigit,
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
                            "description" to "Won ${effectiveOutcome.mode.shortLabel} (${currentPred.displayPickLabel} -> #${effectiveOutcome.resultDigit})".take(220),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (user.notifyResults) {
                    val sizeLabel = if (effectiveOutcome.resultDigit >= 5) "Big" else "Small"
                    tx.set(
                        notifRef,
                        mapOf(
                            "userId" to uid,
                            "category" to if (didWin) "REWARD" else "ROUND_RESULT",
                            "title" to if (didWin) "+$wonCoins VC Won!" else "Round ${effectiveOutcome.periodId} Settled",
                            "message" to if (didWin) {
                                "Your ${currentPred.displayPickLabel} bet won! Result: #${effectiveOutcome.resultDigit} (${effectiveOutcome.winningColor.displayName} • $sizeLabel)."
                            } else {
                                "${effectiveOutcome.mode.title} landed on #${effectiveOutcome.resultDigit} (${effectiveOutcome.winningColor.displayName} • $sizeLabel)."
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
                put("forced30sDigit", config.forced30sDigit?.takeIf { it in 0..9 })
                put("forced60sDigit", config.forced60sDigit?.takeIf { it in 0..9 })
                put("forced3mDigit", config.forced3mDigit?.takeIf { it in 0..9 })
                put("forced5mDigit", config.forced5mDigit?.takeIf { it in 0..9 })
                put("forcedSpinSliceIndex", config.forcedSpinSliceIndex?.takeIf { it in 0..7 })
                put("adminQrLabel", config.adminQrLabel.trim().take(80).ifBlank { "ChromaPulse Counter" })
                put("adminQrPayload", config.adminQrPayload.trim().take(200).ifBlank { "CHROMAPULSE:DEMO" })
                put("updatedByEmail", currentUserEmail())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            if (cfgRef.get().await().exists()) {
                cfgRef.update(payload).await()
            } else {
                cfgRef.set(payload).await()
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

    // ---- Deposit (Admin QR) & Withdrawal (UPI / Paytm) Request Workflows ----

    suspend fun submitDepositRequest(amountCoins: Long, utrReference: String): Result<Unit> {
        var path = "wallet_requests"
        return try {
            val uid = requireUserId()
            require(amountCoins in 10L..500_000L) { "Deposit amount must be between 10 and 500,000 VC." }
            val cleanRef = utrReference.trim().take(150)
            require(cleanRef.length >= 4) { "Enter a valid UTR / Transaction Reference ID (min 4 chars)." }

            val userSnap = db.collection("users").document(uid).get().await()
            val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Player wallet not initialized yet.")

            val reqId = "dep_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            path = "wallet_requests/$reqId"
            db.collection("wallet_requests").document(reqId).set(
                mapOf(
                    "userId" to uid,
                    "userName" to user.displayName.take(60),
                    "userEmail" to user.email.take(120),
                    "publicUserId" to user.publicUserId.take(32),
                    "requestType" to "DEPOSIT",
                    "amountCoins" to amountCoins,
                    "paymentMethod" to "QR_UPI",
                    "paymentDetails" to cleanRef,
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        amountCoins: Long,
        paymentMethod: String,
        paymentDetails: String
    ): Result<Unit> {
        var path = "wallet_requests"
        return try {
            val uid = requireUserId()
            require(amountCoins in 50L..500_000L) { "Withdrawal amount must be between 50 and 500,000 VC." }
            val method = if (paymentMethod == "PAYTM") "PAYTM" else "UPI_ID"
            val cleanDetails = paymentDetails.trim().take(150)
            require(cleanDetails.length >= 5) {
                if (method == "PAYTM") "Enter a valid Paytm mobile number." else "Enter a valid UPI ID (e.g. name@upi)."
            }

            val reqId = "wd_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
            path = "wallet_requests/$reqId"
            val userRef = db.collection("users").document(uid)
            val reqRef = db.collection("wallet_requests").document(reqId)
            val txRef = userRef.collection("wallet_transactions").document("tx_$reqId")

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not initialized yet.")
                if (user.accountStatus == "SUSPENDED") {
                    throw IllegalStateException("This account is currently paused by Admin.")
                }
                if (user.virtualCoins < amountCoins) {
                    throw IllegalStateException("Insufficient balance for withdrawal.")
                }

                val newBalance = user.virtualCoins - amountCoins
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    reqRef,
                    mapOf(
                        "userId" to uid,
                        "userName" to user.displayName.take(60),
                        "userEmail" to user.email.take(120),
                        "publicUserId" to user.publicUserId.take(32),
                        "requestType" to "WITHDRAWAL",
                        "amountCoins" to amountCoins,
                        "paymentMethod" to method,
                        "paymentDetails" to cleanDetails,
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "WITHDRAWAL_REQUEST",
                        "amount" to -amountCoins,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to reqId,
                        "description" to "Withdrawal request ($method: $cleanDetails)".take(220),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminApproveDepositRequest(
        requestId: String,
        adminNote: String,
        actorRole: String
    ): Result<Long> {
        val path = "wallet_requests/$requestId"
        return try {
            requireUserId()
            val reqRef = db.collection("wallet_requests").document(requestId)
            val cleanNote = adminNote.trim().take(180).ifBlank { "QR deposit verified & coins credited" }
            val adminEmail = currentUserEmail()

            val credited = db.runTransaction { tx ->
                val reqSnap = tx.get(reqRef)
                val req = reqSnap.toObject(WalletRequestEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Request not found.")
                if (req.status != "PENDING" || req.requestType != "DEPOSIT") {
                    throw IllegalStateException("Deposit request already processed.")
                }

                val userRef = db.collection("users").document(req.userId)
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Target player wallet not found.")

                val newBalance = (user.virtualCoins + req.amountCoins).coerceAtMost(100_000_000L)
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    reqRef,
                    mapOf(
                        "status" to "APPROVED",
                        "adminNote" to cleanNote,
                        "processedByEmail" to adminEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("wallet_transactions").document("dep_ok_$requestId"),
                    mapOf(
                        "userId" to req.userId,
                        "txType" to "DEPOSIT_CREDIT",
                        "amount" to req.amountCoins,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to req.paymentDetails.take(120),
                        "description" to "Deposit Approved (+${req.amountCoins} VC • Ref: ${req.paymentDetails})".take(220),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("notifications").document("notif_dep_$requestId"),
                    mapOf(
                        "userId" to req.userId,
                        "category" to "REWARD",
                        "title" to "Deposit Approved! +${req.amountCoins} VC",
                        "message" to "Admin verified your QR payment (${req.paymentDetails}) and credited +${req.amountCoins} VC.",
                        "referenceId" to requestId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                req.amountCoins
            }.await()

            writeAdminAuditLog("DEPOSIT_APPROVE", "Approved deposit $requestId (+$credited VC)", actorRole)
            Result.success(credited)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminVerifyWithdrawalRequest(
        requestId: String,
        adminNote: String,
        actorRole: String
    ): Result<Unit> {
        val path = "wallet_requests/$requestId"
        return try {
            requireUserId()
            val reqRef = db.collection("wallet_requests").document(requestId)
            val cleanNote = adminNote.trim().take(180).ifBlank { "Withdrawal verified & completed by Admin" }
            val adminEmail = currentUserEmail()

            val summary = db.runTransaction { tx ->
                val reqSnap = tx.get(reqRef)
                val req = reqSnap.toObject(WalletRequestEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Request not found.")
                if (req.status != "PENDING" || req.requestType != "WITHDRAWAL") {
                    throw IllegalStateException("Withdrawal request already processed.")
                }

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to "VERIFIED",
                        "adminNote" to cleanNote,
                        "processedByEmail" to adminEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    db.collection("users").document(req.userId).collection("notifications").document("notif_wd_$requestId"),
                    mapOf(
                        "userId" to req.userId,
                        "category" to "ANNOUNCEMENT",
                        "title" to "Withdrawal Verified (${req.amountCoins} VC)",
                        "message" to "Admin verified your ${req.paymentMethod} withdrawal to ${req.paymentDetails}. $cleanNote".take(380),
                        "referenceId" to requestId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                "${req.amountCoins} VC to ${req.paymentDetails} (${req.userName})"
            }.await()

            writeAdminAuditLog("WITHDRAWAL_VERIFY", "Verified withdrawal $requestId: $summary", actorRole)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adminRejectWalletRequest(
        requestId: String,
        reason: String,
        actorRole: String
    ): Result<Unit> {
        val path = "wallet_requests/$requestId"
        return try {
            requireUserId()
            val reqRef = db.collection("wallet_requests").document(requestId)
            val cleanReason = reason.trim().take(180).ifBlank { "Request declined by Admin" }
            val adminEmail = currentUserEmail()

            val details = db.runTransaction { tx ->
                val reqSnap = tx.get(reqRef)
                val req = reqSnap.toObject(WalletRequestEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Request not found.")
                if (req.status != "PENDING") {
                    throw IllegalStateException("Request already processed.")
                }

                val userRef = db.collection("users").document(req.userId)
                if (req.requestType == "WITHDRAWAL") {
                    val userSnap = tx.get(userRef)
                    val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (user != null) {
                        val refundedBalance = (user.virtualCoins + req.amountCoins).coerceAtMost(100_000_000L)
                        tx.update(
                            userRef,
                            mapOf(
                                "virtualCoins" to refundedBalance,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        tx.set(
                            userRef.collection("wallet_transactions").document("wd_ref_$requestId"),
                            mapOf(
                                "userId" to req.userId,
                                "txType" to "WITHDRAWAL_REFUND",
                                "amount" to req.amountCoins,
                                "previousBalance" to user.virtualCoins,
                                "newBalance" to refundedBalance,
                                "referenceId" to requestId,
                                "description" to "Withdrawal refunded: $cleanReason".take(220),
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                tx.update(
                    reqRef,
                    mapOf(
                        "status" to "REJECTED",
                        "adminNote" to cleanReason,
                        "processedByEmail" to adminEmail,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef.collection("notifications").document("notif_rej_$requestId"),
                    mapOf(
                        "userId" to req.userId,
                        "category" to "ANNOUNCEMENT",
                        "title" to "${req.requestType.lowercase().replaceFirstChar { it.uppercase() }} Declined",
                        "message" to if (req.requestType == "WITHDRAWAL") {
                            "Your withdrawal of ${req.amountCoins} VC was declined ($cleanReason) and refunded to your wallet."
                        } else {
                            "Your deposit request of ${req.amountCoins} VC was declined: $cleanReason"
                        }.take(380),
                        "referenceId" to requestId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                "${req.requestType} ${req.amountCoins} VC (${req.userName})"
            }.await()

            writeAdminAuditLog("WALLET_REQ_REJECT", "Rejected $requestId ($details): $cleanReason", actorRole)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ---- Promotion & Referral Program (+500 VC Collect) ----

    suspend fun applyReferralCode(rawInput: String): Result<String> {
        var path = "referral_codes"
        return try {
            val uid = requireUserId()
            // Accept either raw code ("CP-C03732") or full invite URL ("https://chromapulse.app/invite?ref=CP-C03732")
            val extracted = rawInput.trim().let { input ->
                val refParam = Regex("[?&]ref=([A-Za-z0-9\\-]+)", RegexOption.IGNORE_CASE)
                    .find(input)?.groupValues?.getOrNull(1)
                (refParam ?: input).trim().uppercase(Locale.US)
            }
            require(extracted.matches(Regex("^[A-Z0-9\\-]{3,32}$"))) {
                "Enter a valid Referral Code (e.g. CP-C03732) or paste a Promotion Invite Link."
            }

            path = "referral_codes/$extracted"
            val codeSnap = db.collection("referral_codes").document(extracted).get().await()
            if (!codeSnap.exists()) {
                throw IllegalStateException("Referral code '$extracted' not found. Ask your friend to open the app once.")
            }
            val referrerUid = codeSnap.getString("referrerUid").orEmpty()
            val referrerName = codeSnap.getString("referrerName").orEmpty().ifBlank { "Chroma Friend" }.take(60)
            if (referrerUid.isBlank() || referrerUid == uid) {
                throw IllegalStateException("You cannot use your own referral code.")
            }

            val bonusCoins = REFERRAL_BONUS_COINS
            val userRef = db.collection("users").document(uid)
            val referralRef = db.collection("referrals").document(uid)
            val txRef = userRef.collection("wallet_transactions").document("ref_join_$extracted")
            val notifRef = userRef.collection("notifications").document("notif_ref_join")
            path = "referrals/$uid"

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not initialized yet.")
                if (!user.referredByCode.isNullOrBlank() || tx.get(referralRef).exists()) {
                    throw IllegalStateException("You have already claimed a referral welcome bonus.")
                }

                val newBalance = (user.virtualCoins + bonusCoins).coerceAtMost(100_000_000L)

                tx.set(
                    referralRef,
                    mapOf(
                        "referredUid" to uid,
                        "referredName" to user.displayName.take(60).ifBlank { "Chroma Player" },
                        "referrerUid" to referrerUid,
                        "referrerCode" to extracted,
                        "referrerName" to referrerName,
                        "bonusCoins" to bonusCoins,
                        "referrerRewardClaimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "referredByCode" to extracted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "REFERRAL_BONUS",
                        "amount" to bonusCoins,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to extracted,
                        "description" to "Referral Welcome Bonus (+$bonusCoins VC via $extracted)".take(220),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "category" to "REWARD",
                        "title" to "+$bonusCoins VC Referral Bonus!",
                        "message" to "You joined with $referrerName's code ($extracted). +$bonusCoins VC added to your wallet!",
                        "referenceId" to extracted,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(referrerName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun collectReferralBonus(referredUid: String): Result<Long> {
        val path = "referrals/$referredUid"
        return try {
            val uid = requireUserId()
            val referralRef = db.collection("referrals").document(referredUid)
            val userRef = db.collection("users").document(uid)
            val txRef = userRef.collection("wallet_transactions").document("ref_col_$referredUid")
            val notifRef = userRef.collection("notifications").document("notif_ref_col_$referredUid")

            val credited = db.runTransaction { tx ->
                val refSnap = tx.get(referralRef)
                val claim = refSnap.toObject(ReferralClaimEntity::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Referral record not found.")
                if (claim.referrerUid != uid) {
                    throw IllegalStateException("This referral reward belongs to another account.")
                }
                if (claim.referrerRewardClaimed) {
                    throw IllegalStateException("Referral reward already collected.")
                }

                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player wallet not ready.")

                val bonus = claim.bonusCoins.coerceIn(50L, 5000L)
                val newBalance = (user.virtualCoins + bonus).coerceAtMost(100_000_000L)

                tx.update(
                    referralRef,
                    mapOf(
                        "referrerRewardClaimed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "virtualCoins" to newBalance,
                        "referralCount" to (user.referralCount + 1L),
                        "referralEarnedCoins" to (user.referralEarnedCoins + bonus).coerceAtMost(100_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "txType" to "REFERRAL_BONUS",
                        "amount" to bonus,
                        "previousBalance" to user.virtualCoins,
                        "newBalance" to newBalance,
                        "referenceId" to referredUid.take(64),
                        "description" to "Collected +$bonus VC referral reward (${claim.referredName})".take(220),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    notifRef,
                    mapOf(
                        "userId" to uid,
                        "category" to "REWARD",
                        "title" to "+$bonus VC Referral Collected!",
                        "message" to "Thank you for inviting ${claim.referredName}! +$bonus VC credited to your wallet.",
                        "referenceId" to referredUid.take(64),
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                bonus
            }.await()

            Result.success(credited)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val SUPER_ADMIN_EMAIL = "buildolive4@gmail.com"
        const val REFERRAL_BONUS_COINS = 500L

        fun sha256Hex(raw: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
