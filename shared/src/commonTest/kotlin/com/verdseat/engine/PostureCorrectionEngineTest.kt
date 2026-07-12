package com.verdseat.engine

import com.verdseat.model.*
import kotlin.test.*

/// PostureCorrectionEngine 单元测试
class PostureCorrectionEngineTest {
    private val engine = PostureCorrectionEngine()

    @Test
    fun `neutral posture requires no correction`() {
        val analysis = makeAnalysis(PostureState.NEUTRAL, SpinePressureLevel.LOW)
        val guide = engine.computeGuidance(analysis)

        assertEquals(0.0, guide.pelvicTiltAdjustDeg, 0.01)
        assertEquals(0.0, guide.lateralShiftAdjustCm, 0.01)
        assertEquals(0.0, guide.shoulderAlignDeg, 0.01)
        assertTrue(guide.verbalGuidance.contains("良好"))
    }

    @Test
    fun `anterior tilt suggests posterior pelvic adjustment`() {
        val analysis = makeAnalysis(PostureState.ANTERIOR_TILT, SpinePressureLevel.HIGH,
            anteriorShift = 30.0)  // 30% 前倾
        val guide = engine.computeGuidance(analysis)

        assertTrue(guide.pelvicTiltAdjustDeg < 0, "前倾应建议向后(负值): ${guide.pelvicTiltAdjustDeg}")
        assertTrue(kotlin.math.abs(guide.pelvicTiltAdjustDeg) <= 15.0, "调整角度应在 ±15 度内")
    }

    @Test
    fun `posterior tilt suggests anterior pelvic adjustment`() {
        val analysis = makeAnalysis(PostureState.POSTERIOR_TILT, SpinePressureLevel.MODERATE,
            anteriorShift = -20.0)
        val guide = engine.computeGuidance(analysis)

        assertTrue(guide.pelvicTiltAdjustDeg > 0, "后倾应建议向前(正值)")
    }

    @Test
    fun `lateral imbalance produces shift guidance`() {
        val analysis = makeAnalysis(PostureState.LATERAL_SHIFT, SpinePressureLevel.HIGH,
            lateralImbalance = 30.0)  // 30% 偏差
        val guide = engine.computeGuidance(analysis)

        assertTrue(kotlin.math.abs(guide.lateralShiftAdjustCm) > 0, "应有侧移建议")
        assertTrue(kotlin.math.abs(guide.lateralShiftAdjustCm) <= 3.0, "侧移 ≤ 3cm")
    }

    @Test
    fun `small imbalance below threshold produces no lateral guidance`() {
        val analysis = makeAnalysis(PostureState.LATERAL_SHIFT, SpinePressureLevel.MODERATE,
            lateralImbalance = 10.0)  // 10% 低于 15% 阈值
        val guide = engine.computeGuidance(analysis)
        assertEquals(0.0, guide.lateralShiftAdjustCm, 0.01)
    }

    @Test
    fun `critical risk generates urgent voice prompt`() {
        val analysis = makeAnalysis(PostureState.SLOUCHING, SpinePressureLevel.CRITICAL,
            peakSegment = DiscSegment.L4_L5)
        val guide = engine.computeGuidance(analysis)

        assertTrue(guide.toVoicePrompt().isNotEmpty())
        assertTrue(guide.verbalGuidance.contains("⚠️") || guide.verbalGuidance.contains("立即"))
    }

    @Test
    fun `pelvic adjustment clamped to max`() {
        val analysis = makeAnalysis(PostureState.ANTERIOR_TILT, SpinePressureLevel.CRITICAL,
            anteriorShift = 50.0)  // 极端前倾
        val guide = engine.computeGuidance(analysis)
        assertTrue(kotlin.math.abs(guide.pelvicTiltAdjustDeg) <= 15.0,
            "骨盆调整不应超过 ±15 度: ${guide.pelvicTiltAdjustDeg}")
    }

    private fun makeAnalysis(
        posture: PostureState,
        risk: SpinePressureLevel,
        anteriorShift: Double = 0.0,
        lateralImbalance: Double = 0.0,
        peakSegment: DiscSegment = DiscSegment.L4_L5
    ): SpineAnalysis {
        return SpineAnalysis(
            timestamp = 0,
            segments = emptyMap(),
            overallRisk = risk,
            postureState = posture,
            peakPressureSegment = peakSegment,
            peakPressureKPa = if (risk == SpinePressureLevel.CRITICAL) 600.0 else 300.0,
            leftRightImbalancePercent = lateralImbalance,
            anteriorShiftPercent = anteriorShift,
            recommendedMassageMode = MassageMode.STANDARD
        )
    }
}
