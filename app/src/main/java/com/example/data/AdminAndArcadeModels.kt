package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ArcadeGameId(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val maxMultiplierLabel: String
) {
    WINGO_COLOR(
        id = "WINGO_COLOR",
        title = "WinGo Color",
        subtitle = "30s • 60s • 3m • 5m Live Timers",
        badge = "HOT",
        maxMultiplierLabel = "Up to 4.5x"
    ),
    PULSE_CRASH(
        id = "PULSE_CRASH",
        title = "Pulse Crash",
        subtitle = "Rocket rises — Cash out before crash!",
        badge = "LIVE",
        maxMultiplierLabel = "Up to 25x"
    ),
    NEON_MINES(
        id = "NEON_MINES",
        title = "Neon Mines",
        subtitle = "5x5 Gem Grid — Dodge bombs & cash out",
        badge = "POPULAR",
        maxMultiplierLabel = "Up to 18x"
    ),
    CYBER_DICE(
        id = "CYBER_DICE",
        title = "Cyber Dice",
        subtitle = "Roll Over or Under target 10–90",
        badge = "INSTANT",
        maxMultiplierLabel = "Up to 9.5x"
    ),
    CHROMA_WHEEL(
        id = "CHROMA_WHEEL",
        title = "Chroma Wheel",
        subtitle = "Spin the neon prize wheel for VC",
        badge = "BONUS",
        maxMultiplierLabel = "Up to 10x"
    ),
    DRAGON_TIGER(
        id = "DRAGON_TIGER",
        title = "Dragon vs Tiger",
        subtitle = "Fast 2-card showdown • Tie pays 8x",
        badge = "FAST",
        maxMultiplierLabel = "Up to 8x"
    );

    companion object {
        fun fromId(raw: String): ArcadeGameId =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: WINGO_COLOR
    }
}

@Entity(
    tableName = "admins",
    indices = [Index(value = ["email"], unique = true)]
)
data class AdminAccount(
    @PrimaryKey val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = ROLE_CO_ADMIN, // SUPER_ADMIN | CO_ADMIN
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val isActive: Boolean = true,
    val canManagePlayers: Boolean = true,
    val canManageGames: Boolean = true,
    val canSendBroadcasts: Boolean = true,
    val createdByEmail: String = MAIN_ADMIN_EMAIL,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val lastLoginAtMs: Long = 0L
) {
    val isSuperAdmin: Boolean get() = role == ROLE_SUPER_ADMIN

    companion object {
        const val MAIN_ADMIN_EMAIL = "buildolive4@gmail.com"
        const val ROLE_SUPER_ADMIN = "SUPER_ADMIN"
        const val ROLE_CO_ADMIN = "CO_ADMIN"
    }
}

@Entity(tableName = "platform_config")
data class PlatformConfig(
    @PrimaryKey val id: String = DEFAULT_ID,
    val maintenanceMode: Boolean = false,
    val colorGameEnabled: Boolean = true,
    val crashGameEnabled: Boolean = true,
    val minesGameEnabled: Boolean = true,
    val diceGameEnabled: Boolean = true,
    val wheelGameEnabled: Boolean = true,
    val dragonTigerEnabled: Boolean = true,
    val dailyBonusCoins: Long = 500L,
    val welcomeBonusCoins: Long = 2500L,
    val announcementBanner: String = "Welcome to ChromaPulse Arcade! 6 Virtual Coin Games Live — Entertainment Only.",
    val updatedByEmail: String = AdminAccount.MAIN_ADMIN_EMAIL,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun isGameEnabled(game: ArcadeGameId): Boolean {
        if (maintenanceMode) return false
        return when (game) {
            ArcadeGameId.WINGO_COLOR -> colorGameEnabled
            ArcadeGameId.PULSE_CRASH -> crashGameEnabled
            ArcadeGameId.NEON_MINES -> minesGameEnabled
            ArcadeGameId.CYBER_DICE -> diceGameEnabled
            ArcadeGameId.CHROMA_WHEEL -> wheelGameEnabled
            ArcadeGameId.DRAGON_TIGER -> dragonTigerEnabled
        }
    }

    companion object {
        const val DEFAULT_ID = "default"
    }
}

@Entity(tableName = "arcade_plays")
data class ArcadeGameRecord(
    @PrimaryKey val id: String = "",
    val userId: String = "local_player",
    val gameId: String = ArcadeGameId.PULSE_CRASH.id,
    val gameTitle: String = "",
    val stakeCoins: Long = 0L,
    val multiplier: Double = 0.0,
    val rewardCoins: Long = 0L,
    val outcomeSummary: String = "",
    val didWin: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)
