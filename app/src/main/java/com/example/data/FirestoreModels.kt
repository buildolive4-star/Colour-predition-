package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Chroma Player",
    val email: String = "",
    val publicUserId: String = "CP-000000",
    val photoUrl: String? = null,
    val virtualCoins: Long = 2500L,
    val totalGames: Long = 0L,
    val totalWins: Long = 0L,
    val totalVirtualPoints: Long = 0L,
    val currentStreak: Long = 0L,
    val bestStreak: Long = 0L,
    val lastDailyBonusDate: String? = null,
    val notifyResults: Boolean = true,
    val notifyRoundClosing: Boolean = true,
    val notifyBonuses: Boolean = true,
    val accountStatus: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class PredictionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val modeId: String = "mode_30s",
    val periodId: String = "",
    val betType: String = "COLOR",
    val betLabel: String = "",
    val selectedColor: String = "GREEN",
    val selectedDigit: Int? = null,
    val selectedSize: String? = null,
    val stakeCoins: Long = 0L,
    val status: String = "PENDING",
    val winningColor: String? = null,
    val winningDigit: Int? = null,
    val rewardCoins: Long = 0L,
    val resultHash: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: 0L

    @get:Exclude
    val displayPickLabel: String
        get() = betLabel.ifBlank {
            when (betType) {
                "NUMBER" -> "Number ${selectedDigit ?: 0}"
                "SIZE" -> if (selectedSize == "SMALL") "Small" else "Big"
                else -> selectedColor.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
}

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val txType: String = "WELCOME_BONUS",
    val amount: Long = 0L,
    val previousBalance: Long = 0L,
    val newBalance: Long = 0L,
    val referenceId: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "ANNOUNCEMENT",
    val title: String = "",
    val message: String = "",
    val referenceId: String? = null,
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class GameRoundRecord(
    @DocumentId val id: String = "",
    val modeId: String = "mode_30s",
    val periodId: String = "",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val winningColor: String = "GREEN",
    val resultDigit: Long = 0L,
    val resultHash: String = "",
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Chroma Player",
    val publicUserId: String = "CP-000000",
    val photoUrl: String? = null,
    val totalVirtualPoints: Long = 0L,
    val totalGames: Long = 0L,
    val totalWins: Long = 0L,
    val bestStreak: Long = 0L,
    val badgeTitle: String = "Pulse Rookie",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class AppConfigEntity(
    @DocumentId val id: String = "global",
    val announcementText: String = "Virtual Coins Entertainment Arena — Pick Green, Violet, or Red with SHA-256 verifiable results.",
    val redGreenMultiplier: Double = 2.0,
    val violetMultiplier: Double = 4.5,
    val mode30sEnabled: Boolean = true,
    val mode60sEnabled: Boolean = true,
    val mode3mEnabled: Boolean = true,
    val mode5mEnabled: Boolean = true,
    val forcedNextColor: String? = null,
    val adminQrLabel: String = "ChromaPulse Virtual Counter",
    val adminQrPayload: String = "CHROMAPULSE:VIRTUAL-DEMO:COUNTER-01",
    val updatedByEmail: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class VirtualVoucherEntity(
    @DocumentId val id: String = "",
    val code: String = "",
    val coinsAmount: Long = 500L,
    val label: String = "Virtual Gift QR",
    val publicVisible: Boolean = true,
    val redeemed: Boolean = false,
    val redeemedByUid: String? = null,
    @ServerTimestamp val redeemedAt: Timestamp? = null,
    val createdByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val redeemedAtMs: Long? get() = redeemedAt?.toDate()?.time
}

@Keep
@IgnoreExtraProperties
data class AdminAuditLogEntity(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actorRole: String = "SUPER_ADMIN",
    val action: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class CoAdminInviteEntity(
    @DocumentId val inviteHash: String = "",
    val label: String = "",
    val active: Boolean = true,
    val createdByEmail: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class CoAdminGrantEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val label: String = "",
    val inviteHash: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L
}

@Keep
@IgnoreExtraProperties
data class WalletRequestEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val publicUserId: String = "",
    val requestType: String = "DEPOSIT", // DEPOSIT or WITHDRAWAL
    val amountCoins: Long = 0L,
    val paymentMethod: String = "QR_UPI", // QR_UPI, UPI_ID, PAYTM
    val paymentDetails: String = "", // UTR reference for deposit, UPI ID or Paytm No. for withdrawal
    val status: String = "PENDING", // PENDING, APPROVED, VERIFIED, REJECTED
    val adminNote: String? = null,
    val processedByEmail: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val createdAtMs: Long get() = createdAt?.toDate()?.time ?: 0L

    @get:Exclude
    val updatedAtMs: Long get() = updatedAt?.toDate()?.time ?: 0L
}
