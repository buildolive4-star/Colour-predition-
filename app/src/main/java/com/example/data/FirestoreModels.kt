package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val email: String = "",
    val publicUserId: String = "CP-000000",
    val photoUrl: String? = null,
    val virtualCoins: Long = 0L,
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
)

@Keep
@IgnoreExtraProperties
data class PredictionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val modeId: String = "mode_30s",
    val periodId: String = "",
    val selectedColor: String = "GREEN",
    val stakeCoins: Long = 0L,
    val status: String = "PENDING",
    val winningColor: String? = null,
    val rewardCoins: Long = 0L,
    val resultHash: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

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
)

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
)

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
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val publicUserId: String = "CP-000000",
    val photoUrl: String? = null,
    val totalVirtualPoints: Long = 0L,
    val totalGames: Long = 0L,
    val totalWins: Long = 0L,
    val bestStreak: Long = 0L,
    val badgeTitle: String = "Pulse Rookie",
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
