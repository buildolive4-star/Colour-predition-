package com.example

import com.example.domain.ColorPredictionEngine
import com.example.domain.GameMode
import com.example.domain.PredictionColor
import com.example.domain.RoundPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPredictionEngineTest {

    @Test
    fun `active round computes deterministic period IDs and phase transitions`() {
        val baseStart = 1_795_350_000_000L // aligned to 30s
        val openRound = ColorPredictionEngine.computeActiveRound(GameMode.MODE_30S, baseStart + 5_000L)
        assertEquals(RoundPhase.ACTIVE, openRound.phase)
        assertTrue(openRound.canPredict)
        assertEquals(25_000L, openRound.remainingMs)

        // Final 5 seconds of 30s round must be LOCKED
        val lockedRound = ColorPredictionEngine.computeActiveRound(GameMode.MODE_30S, baseStart + 26_000L)
        assertEquals(RoundPhase.LOCKED, lockedRound.phase)
        assertFalse(lockedRound.canPredict)

        // Same period ID during the same 30s window
        assertEquals(openRound.periodId, lockedRound.periodId)

        // Next 30s window produces a new period ID
        val nextRound = ColorPredictionEngine.computeActiveRound(GameMode.MODE_30S, baseStart + 31_000L)
        assertTrue(openRound.periodId != nextRound.periodId)
    }

    @Test
    fun `verifiable SHA-256 outcome is deterministic and produces valid hash and digit`() {
        val start = 1_795_350_000_000L
        val end = start + GameMode.MODE_60S.durationMs
        val first = ColorPredictionEngine.generateVerifiableOutcome(GameMode.MODE_60S, start, end)
        val second = ColorPredictionEngine.generateVerifiableOutcome(GameMode.MODE_60S, start, end)

        assertEquals(first, second)
        assertEquals(64, first.resultHash.length)
        assertTrue(first.resultDigit in 0..9)
    }

    @Test
    fun `payout calculation awards 2x for Red and Green, 4_5x for Violet, and 0 on miss`() {
        assertEquals(200L, ColorPredictionEngine.calculateRewardCoins(PredictionColor.RED, PredictionColor.RED, 100L))
        assertEquals(200L, ColorPredictionEngine.calculateRewardCoins(PredictionColor.GREEN, PredictionColor.GREEN, 100L))
        assertEquals(450L, ColorPredictionEngine.calculateRewardCoins(PredictionColor.VIOLET, PredictionColor.VIOLET, 100L))
        assertEquals(0L, ColorPredictionEngine.calculateRewardCoins(PredictionColor.RED, PredictionColor.GREEN, 100L))
    }

    @Test
    fun `period end time parser round-trips cleanly for all 4 game modes`() {
        val sampleNow = 1_795_350_123_456L
        GameMode.entries.forEach { mode ->
            val round = ColorPredictionEngine.computeActiveRound(mode, sampleNow)
            val parsedEnd = ColorPredictionEngine.parsePeriodEndTimeMs(mode, round.periodId)
            assertNotNull(parsedEnd)
            assertEquals(round.endTimeMs, parsedEnd)
        }
    }

    @Test
    fun `countdown formatting and badge tier resolution work accurately`() {
        assertEquals("00:30", ColorPredictionEngine.formatCountdown(30_000L))
        assertEquals("01:00", ColorPredictionEngine.formatCountdown(59_500L))
        assertEquals("00:00", ColorPredictionEngine.formatCountdown(0L))
        assertEquals("Pulse Rookie", ColorPredictionEngine.resolveBadgeTitle(0L, 0L, 0L))
        assertEquals("Grand Oracle", ColorPredictionEngine.resolveBadgeTitle(60_000L, 12L, 4L))
        val (nextLabel, progress) = ColorPredictionEngine.resolveNextBadgeProgress(750L)
        assertTrue(nextLabel.contains("Pulse Striker"))
        assertEquals(0.5f, progress, 0.01f)
        val (maxLabel, maxProgress) = ColorPredictionEngine.resolveNextBadgeProgress(60_000L)
        assertEquals("Max Tier Reached", maxLabel)
        assertEquals(1f, maxProgress, 0.001f)
    }
}
