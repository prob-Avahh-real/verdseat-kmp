package com.verdseat.model

import kotlin.test.*

/// 数据模型完整性测试
class EntitiesTest {

    @Test
    fun `DiscSegment has 6 members`() {
        assertEquals(6, DiscSegment.entries.size)
    }

    @Test
    fun `L4_L5 and L5_S1 have highest typical pressure`() {
        val pressures = DiscSegment.entries.map { it.typicalIntradiscalPressureKPa }
        val max = pressures.max()
        assertTrue(DiscSegment.L5_S1.typicalIntradiscalPressureKPa >= max - 1)
        assertTrue(DiscSegment.L4_L5.typicalIntradiscalPressureKPa >= DiscSegment.L3_L4.typicalIntradiscalPressureKPa)
    }

    @Test
    fun `PostureState has 6 members`() {
        assertEquals(6, PostureState.entries.size)
    }

    @Test
    fun `MassageMode has 7 modes`() {
        assertEquals(7, MassageMode.entries.size)
    }

    @Test
    fun `BodyType multipliers are monotonic`() {
        val multipliers = BodyType.entries.map { it.discLoadMultiplier }
        for (i in 1 until multipliers.size) {
            assertTrue(multipliers[i] >= multipliers[i - 1], "体型系数应单调递增")
        }
    }

    @Test
    fun `CorrectionGuidance voice prompt for anterior tilt`() {
        val guide = CorrectionGuidance(
            timestamp = 0,
            currentState = PostureState.ANTERIOR_TILT,
            pelvicTiltAdjustDeg = -5.0
        )
        val prompt = guide.toVoicePrompt()
        assertTrue(prompt.contains("后倾") || prompt.contains("骨盆"), "应含骨盆指令: $prompt")
    }

    @Test
    fun `CorrectionGuidance voice prompt for lateral shift`() {
        val guide = CorrectionGuidance(
            timestamp = 0,
            currentState = PostureState.LATERAL_SHIFT,
            lateralShiftAdjustCm = 2.0
        )
        assertTrue(guide.toVoicePrompt().contains("右"))
    }

    @Test
    fun `UserProfile has reasonable defaults`() {
        val p = UserProfile()
        assertEquals(170, p.heightCm)
        assertEquals(65, p.weightKg)
        assertEquals(BodyType.AVERAGE, p.bodyType)
    }

    @Test
    fun `SpineAnalysis isCritical flag`() {
        val critical = SpineAnalysis(
            timestamp = 0, segments = emptyMap(),
            overallRisk = SpinePressureLevel.CRITICAL,
            postureState = PostureState.NEUTRAL,
            peakPressureSegment = DiscSegment.L5_S1,
            peakPressureKPa = 600.0,
            leftRightImbalancePercent = 0.0,
            anteriorShiftPercent = 0.0,
            recommendedMassageMode = MassageMode.SPOT
        )
        assertTrue(critical.isCritical)

        val low = critical.copy(overallRisk = SpinePressureLevel.LOW)
        assertFalse(low.isCritical)
    }
}
