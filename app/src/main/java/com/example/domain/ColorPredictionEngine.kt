package com.example.domain

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToLong

object ColorPredictionEngine {

    const val INITIAL_DEMO_COINS = 2500L
    const val DAILY_BONUS_COINS = 500L
    const val DEMO_REFILL_THRESHOLD = 100L
    const val DEMO_REFILL_COINS = 1000L
    const val MIN_STAKE_COINS = 10L
    const val MAX_STAKE_COINS = 100000L

    private const val SEED_VERSION = "CHROMAPULSE_FAIR_V1"
    private const val MILLIS_PER_DAY = 86_400_000L

    fun formatUtcDateKey(epochMs: Long): String {
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(epochMs))
    }

    fun formatUtcIsoDate(epochMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(epochMs))
    }

    fun buildPeriodId(mode: GameMode, startTimeMs: Long): String {
        val dayKey = formatUtcDateKey(startTimeMs)
        val msIntoDay = Math.floorMod(startTimeMs, MILLIS_PER_DAY)
        val periodIndex = (msIntoDay / mode.durationMs) + 1L
        return String.format(Locale.US, "%s-%s-%05d", dayKey, mode.code, periodIndex)
    }

    fun buildRoundDocId(mode: GameMode, periodId: String): String =
        "${mode.id}_$periodId"

    fun computeActiveRound(mode: GameMode, nowMs: Long): ActiveRoundSnapshot {
        val durationMs = mode.durationMs
        val periodStartMs = (nowMs / durationMs) * durationMs
        val periodEndMs = periodStartMs + durationMs
        val lockTimeMs = periodEndMs - mode.lockBeforeEndMs
        val remainingMs = (periodEndMs - nowMs).coerceAtLeast(0L)
        val elapsedMs = (nowMs - periodStartMs).coerceIn(0L, durationMs)
        val progress = (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        val phase = when {
            nowMs < periodStartMs -> RoundPhase.WAITING
            remainingMs <= 800L -> RoundPhase.RESULT_PENDING
            nowMs >= lockTimeMs -> RoundPhase.LOCKED
            else -> RoundPhase.ACTIVE
        }

        val periodId = buildPeriodId(mode, periodStartMs)
        return ActiveRoundSnapshot(
            mode = mode,
            periodId = periodId,
            roundDocId = buildRoundDocId(mode, periodId),
            startTimeMs = periodStartMs,
            lockTimeMs = lockTimeMs,
            endTimeMs = periodEndMs,
            remainingMs = remainingMs,
            progress = progress,
            phase = phase
        )
    }

    fun computePreviousCompletedRound(mode: GameMode, nowMs: Long): VerifiableRoundOutcome {
        val durationMs = mode.durationMs
        val currentStartMs = (nowMs / durationMs) * durationMs
        val prevStartMs = currentStartMs - durationMs
        val prevEndMs = currentStartMs
        return generateVerifiableOutcome(mode, prevStartMs, prevEndMs)
    }

    fun computeRecentCompletedRounds(
        mode: GameMode,
        nowMs: Long,
        count: Int = 10
    ): List<VerifiableRoundOutcome> {
        val durationMs = mode.durationMs
        val currentStartMs = (nowMs / durationMs) * durationMs
        return (1..count).map { offset ->
            val startMs = currentStartMs - (offset * durationMs)
            val endMs = startMs + durationMs
            generateVerifiableOutcome(mode, startMs, endMs)
        }
    }

    fun generateVerifiableOutcome(
        mode: GameMode,
        startTimeMs: Long,
        endTimeMs: Long
    ): VerifiableRoundOutcome {
        val periodId = buildPeriodId(mode, startTimeMs)
        return generateVerifiableOutcomeForPeriod(mode, periodId, startTimeMs, endTimeMs)
    }

    fun generateVerifiableOutcomeForPeriod(
        mode: GameMode,
        periodId: String,
        startTimeMs: Long,
        endTimeMs: Long
    ): VerifiableRoundOutcome {
        val canonicalSeed = "$SEED_VERSION:${mode.id}:$periodId:$endTimeMs"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonicalSeed.toByteArray(Charsets.UTF_8))
        val hexHash = digest.joinToString("") { byte -> "%02x".format(byte) }

        val firstByteUnsigned = digest[0].toInt() and 0xFF
        val secondByteUnsigned = digest[1].toInt() and 0xFF
        val combined = (firstByteUnsigned * 256) + secondByteUnsigned
        val digit = combined % 10

        val winningColor = digitToColor(digit)

        return VerifiableRoundOutcome(
            mode = mode,
            periodId = periodId,
            roundDocId = buildRoundDocId(mode, periodId),
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            winningColor = winningColor,
            resultDigit = digit,
            resultHash = hexHash
        )
    }

    fun digitToColor(digit: Int): PredictionColor = when (digit) {
        0, 5 -> PredictionColor.VIOLET
        1, 3, 7, 9 -> PredictionColor.GREEN
        else -> PredictionColor.RED
    }

    fun parsePeriodEndTimeMs(mode: GameMode, periodId: String): Long? {
        val parts = periodId.split("-")
        if (parts.size != 3) return null
        val dayKey = parts[0]
        val index = parts[2].toLongOrNull() ?: return null
        if (index < 1L) return null
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dayStartMs = formatter.parse(dayKey)?.time ?: return null
        return dayStartMs + (index * mode.durationMs)
    }

    fun calculateRewardCoins(
        selectedColor: PredictionColor,
        winningColor: PredictionColor,
        stakeCoins: Long
    ): Long {
        if (selectedColor != winningColor) return 0L
        return (stakeCoins.toDouble() * selectedColor.multiplier).roundToLong()
    }

    fun calculateBetReward(
        betType: BetType,
        selectedColor: PredictionColor,
        selectedDigit: Int?,
        selectedSize: SizeChoice?,
        winningColor: PredictionColor,
        winningDigit: Int,
        stakeCoins: Long,
        redGreenMultiplier: Double = 2.0,
        violetMultiplier: Double = 4.5
    ): Long {
        val raw = when (betType) {
            BetType.COLOR -> {
                val mult = if (selectedColor == PredictionColor.VIOLET) violetMultiplier else redGreenMultiplier
                if (selectedColor == winningColor) stakeCoins.toDouble() * mult else 0.0
            }
            BetType.NUMBER -> {
                if (selectedDigit != null && selectedDigit == winningDigit) stakeCoins.toDouble() * 9.0 else 0.0
            }
            BetType.SIZE -> {
                val isBig = winningDigit >= 5
                val matched = (selectedSize == SizeChoice.BIG && isBig) || (selectedSize == SizeChoice.SMALL && !isBig)
                if (matched) stakeCoins.toDouble() * 2.0 else 0.0
            }
        }
        return raw.roundToLong().coerceIn(0L, 1_000_000L)
    }

    fun formatCountdown(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999L) / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun buildPublicUserId(uid: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uid.toByteArray(Charsets.UTF_8))
        val shortHex = digest.take(3).joinToString("") { "%02X".format(it) }
        return "CP-$shortHex"
    }

    fun resolveBadgeTitle(totalVirtualPoints: Long, totalWins: Long, bestStreak: Long): String =
        when {
            totalVirtualPoints >= 50000L || bestStreak >= 10L -> "Grand Oracle"
            totalVirtualPoints >= 20000L || totalWins >= 35L -> "Chroma Master"
            totalVirtualPoints >= 7500L || totalWins >= 15L -> "Prism Tactician"
            totalVirtualPoints >= 1500L || totalWins >= 4L -> "Pulse Striker"
            else -> "Pulse Rookie"
        }

    fun resolveNextBadgeProgress(totalVirtualPoints: Long): Pair<String, Float> =
        when {
            totalVirtualPoints >= 50000L -> "Max Tier Reached" to 1f
            totalVirtualPoints >= 20000L -> "Next: Grand Oracle (50,000 pts)" to ((totalVirtualPoints - 20000L) / 30000f).coerceIn(0f, 1f)
            totalVirtualPoints >= 7500L -> "Next: Chroma Master (20,000 pts)" to ((totalVirtualPoints - 7500L) / 12500f).coerceIn(0f, 1f)
            totalVirtualPoints >= 1500L -> "Next: Prism Tactician (7,500 pts)" to ((totalVirtualPoints - 1500L) / 6000f).coerceIn(0f, 1f)
            else -> "Next: Pulse Striker (1,500 pts)" to (totalVirtualPoints / 1500f).coerceIn(0f, 1f)
        }
}
