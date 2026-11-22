package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val id: String = "local_player",
    val userId: String = "local_player",
    val displayName: String = "Chroma Player",
    val email: String = "Local Demo Account",
    val publicUserId: String = "CP-DEMO01",
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
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "predictions")
data class PredictionRecord(
    @PrimaryKey val id: String = "",
    val userId: String = "local_player",
    val modeId: String = "mode_30s",
    val periodId: String = "",
    val selectedColor: String = "GREEN",
    val stakeCoins: Long = 0L,
    val status: String = "PENDING",
    val winningColor: String? = null,
    val rewardCoins: Long = 0L,
    val resultHash: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "wallet_transactions")
data class WalletTransaction(
    @PrimaryKey val id: String = "",
    val userId: String = "local_player",
    val txType: String = "WELCOME_BONUS",
    val amount: Long = 0L,
    val previousBalance: Long = 0L,
    val newBalance: Long = 0L,
    val referenceId: String = "",
    val description: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class UserNotification(
    @PrimaryKey val id: String = "",
    val userId: String = "local_player",
    val category: String = "ANNOUNCEMENT",
    val title: String = "",
    val message: String = "",
    val referenceId: String? = null,
    val read: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "game_rounds")
data class GameRoundRecord(
    @PrimaryKey val id: String = "",
    val modeId: String = "mode_30s",
    val periodId: String = "",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val winningColor: String = "GREEN",
    val resultDigit: Long = 0L,
    val resultHash: String = "",
    val status: String = "COMPLETED",
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey val id: String = "local_player",
    val userId: String = "local_player",
    val displayName: String = "Chroma Player",
    val publicUserId: String = "CP-DEMO01",
    val photoUrl: String? = null,
    val totalVirtualPoints: Long = 0L,
    val totalGames: Long = 0L,
    val totalWins: Long = 0L,
    val bestStreak: Long = 0L,
    val badgeTitle: String = "Pulse Rookie",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: String = "global",
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
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "virtual_vouchers")
data class VirtualVoucherEntity(
    @PrimaryKey val code: String,
    val coinsAmount: Long,
    val label: String,
    val redeemed: Boolean = false,
    val redeemedAtMs: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey val id: String,
    val action: String,
    val details: String,
    val createdAtMs: Long = System.currentTimeMillis()
)
