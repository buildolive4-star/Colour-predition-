package com.example.domain

enum class PredictionColor(
    val displayName: String,
    val multiplier: Double,
    val multiplierLabel: String
) {
    RED("Red", 2.0, "2.0x"),
    GREEN("Green", 2.0, "2.0x"),
    VIOLET("Violet", 4.5, "4.5x");

    companion object {
        fun fromRaw(raw: String?): PredictionColor =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GREEN
    }
}

enum class GameMode(
    val id: String,
    val title: String,
    val shortLabel: String,
    val code: String,
    val durationSeconds: Long,
    val lockBeforeEndSeconds: Long,
    val subtitle: String
) {
    MODE_30S(
        id = "mode_30s",
        title = "Turbo 30s",
        shortLabel = "30s",
        code = "30S",
        durationSeconds = 30L,
        lockBeforeEndSeconds = 5L,
        subtitle = "Rapid-fire 30 second pulse rounds"
    ),
    MODE_60S(
        id = "mode_60s",
        title = "Rapid 60s",
        shortLabel = "60s",
        code = "60S",
        durationSeconds = 60L,
        lockBeforeEndSeconds = 8L,
        subtitle = "Fast 1-minute tactical color cycles"
    ),
    MODE_3M(
        id = "mode_3m",
        title = "Classic 3m",
        shortLabel = "3m",
        code = "3M",
        durationSeconds = 180L,
        lockBeforeEndSeconds = 15L,
        subtitle = "Balanced 3-minute arena rounds"
    ),
    MODE_5M(
        id = "mode_5m",
        title = "Grand 5m",
        shortLabel = "5m",
        code = "5M",
        durationSeconds = 300L,
        lockBeforeEndSeconds = 20L,
        subtitle = "High-anticipation 5-minute sessions"
    );

    val durationMs: Long get() = durationSeconds * 1000L
    val lockBeforeEndMs: Long get() = lockBeforeEndSeconds * 1000L

    companion object {
        fun fromId(id: String?): GameMode =
            entries.firstOrNull { it.id == id } ?: MODE_30S
    }
}

enum class RoundPhase(val label: String) {
    WAITING("Waiting"),
    ACTIVE("Open"),
    LOCKED("Locked"),
    RESULT_PENDING("Verifying"),
    COMPLETED("Completed")
}

data class ActiveRoundSnapshot(
    val mode: GameMode,
    val periodId: String,
    val roundDocId: String,
    val startTimeMs: Long,
    val lockTimeMs: Long,
    val endTimeMs: Long,
    val remainingMs: Long,
    val progress: Float,
    val phase: RoundPhase
) {
    val canPredict: Boolean get() = phase == RoundPhase.ACTIVE
}

data class VerifiableRoundOutcome(
    val mode: GameMode,
    val periodId: String,
    val roundDocId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val winningColor: PredictionColor,
    val resultDigit: Int,
    val resultHash: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HistoryDateFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("7 Days")
}

enum class LeaderboardPeriodFilter(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ALL_TIME("All Time")
}
