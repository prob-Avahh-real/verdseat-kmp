package com.verdseat.engine

import com.verdseat.model.*
import kotlin.test.*

/// DiscLoadEngine 单元测试 — 覆盖 6 节段、4 风险等级、5 坐姿状态
class DiscLoadEngineTest {
    private val engine = DiscLoadEngine()
    private val defaultProfile = UserProfile(
        heightCm = 170, weightKg = 70, bodyType = BodyType.AVERAGE
    )

    // ─── 正常坐姿 — 低风险 ────────────────────────────

    @Test
    fun `neutral posture with symmetric pressure produces low risk`() {
        val snapshot = symmetricSnapshot()
        val result = engine.estimate(snapshot, defaultProfile)

        assertEquals(PostureState.NEUTRAL, result.postureState)
        assertEquals(SpinePressureLevel.LOW, result.overallRisk)
        assertTrue(result.leftRightImbalancePercent < 10.0)
    }

    @Test
    fun `all 6 disc segments present in analysis`() {
        val result = engine.estimate(symmetricSnapshot(), defaultProfile)
        assertEquals(DiscSegment.entries.size, result.segments.size)
        for (seg in DiscSegment.entries) {
            assertTrue(result.segments.containsKey(seg), "缺少段: ${seg.label}")
        }
    }

    // ─── 骨盆前倾 — 高风险 ────────────────────────────

    @Test
    fun `anterior tilt increases L5_S1 pressure`() {
        val snapshot = anteriorTiltSnapshot()
        val result = engine.estimate(snapshot, defaultProfile)

        assertEquals(PostureState.ANTERIOR_TILT, result.postureState)
        val l5s1 = result.segments[DiscSegment.L5_S1]!!
        assertTrue(l5s1.estimatedPressureKPa > MODERATE_DISC_PRESSURE_KPA,
            "L5-S1 压力 ${l5s1.estimatedPressureKPa} 应 > ${MODERATE_DISC_PRESSURE_KPA}")
    }

    @Test
    fun `anterior tilt triggers high or critical risk`() {
        val snapshot = anteriorTiltSnapshot()
        val result = engine.estimate(snapshot, defaultProfile)
        assertTrue(result.overallRisk >= SpinePressureLevel.HIGH)
    }

    // ─── 左右不对称 ────────────────────────────────────

    @Test
    fun `lateral shift is detected from asymmetric pressure`() {
        val snapshot = lateralShiftSnapshot()
        val result = engine.estimate(snapshot, defaultProfile)

        assertEquals(PostureState.LATERAL_SHIFT, result.postureState)
        assertTrue(result.leftRightImbalancePercent > 15.0,
            "左右偏差 ${result.leftRightImbalancePercent}% 应 > 15%")
    }

    // ─── 体重影响 ──────────────────────────────────────

    @Test
    fun `overweight user has higher disc pressure`() {
        val snapshot = symmetricSnapshot()
        val light = engine.estimate(snapshot, defaultProfile.copy(weightKg = 60))
        val heavy = engine.estimate(snapshot, defaultProfile.copy(weightKg = 90))

        val lightMax = light.segments.values.maxOf { it.estimatedPressureKPa }
        val heavyMax = heavy.segments.values.maxOf { it.estimatedPressureKPa }
        assertTrue(heavyMax > lightMax, "体重大会增加椎间盘压力: light=$lightMax, heavy=$heavyMax")
    }

    @Test
    fun `body type multiplier affects pressure`() {
        val snapshot = symmetricSnapshot()
        val slim = engine.estimate(snapshot, defaultProfile.copy(bodyType = BodyType.SLIM))
        val obese = engine.estimate(snapshot, defaultProfile.copy(bodyType = BodyType.OBESE))

        val slimMax = slim.segments.values.maxOf { it.estimatedPressureKPa }
        val obeseMax = obese.segments.values.maxOf { it.estimatedPressureKPa }
        assertTrue(obeseMax > slimMax, "体型系数应增加压力")
    }

    // ─── 临界压力 ──────────────────────────────────────

    @Test
    fun `critical pressure never exceeds 800 kPa clamp`() {
        val snapshot = extremePressureSnapshot()
        val heavy = UserProfile(weightKg = 120, bodyType = BodyType.OBESE)
        val result = engine.estimate(snapshot, heavy)

        for ((_, data) in result.segments) {
            assertTrue(data.estimatedPressureKPa <= 800.0,
                "${data.segment.label} 压力 ${data.estimatedPressureKPa} > 800")
        }
    }

    @Test
    fun `peak pressure segment is L5_S1 under heavy load`() {
        val snapshot = anteriorTiltSnapshot()
        val heavy = UserProfile(weightKg = 100, bodyType = BodyType.OVERWEIGHT)
        val result = engine.estimate(snapshot, heavy)

        assertTrue(result.peakPressureKPa > 0)
        assertTrue(
            result.peakPressureSegment in listOf(DiscSegment.L4_L5, DiscSegment.L5_S1),
            "最重段应为 L4-L5 或 L5-S1, 实际: ${result.peakPressureSegment.label}"
        )
    }

    // ─── 推荐按摩模式 ──────────────────────────────────

    @Test
    fun `low risk recommends relax massage`() {
        val result = engine.estimate(symmetricSnapshot(), defaultProfile)
        assertEquals(MassageMode.RELAX, result.recommendedMassageMode)
    }

    @Test
    fun `critical risk recommends spot or acupressure`() {
        val snapshot = extremePressureSnapshot()
        val result = engine.estimate(snapshot, defaultProfile)
        assertTrue(result.recommendedMassageMode in listOf(MassageMode.SPOT, MassageMode.ACUPRESSURE),
            "临界状态应推荐深层按摩")
    }

    // ─── 已知脊椎问题放大风险 ──────────────────────────

    @Test
    fun `herniated disc lowers risk threshold`() {
        val profile = defaultProfile.copy(
            knownConditions = listOf(SpineCondition.HERNIATED_DISC)
        )
        val snapshot = moderateAsymmetrySnapshot()
        val normal = engine.estimate(snapshot, defaultProfile)
        val patient = engine.estimate(snapshot, profile)

        assertTrue(patient.overallRisk >= normal.overallRisk,
            "椎间盘突出史应有更高风险: normal=${normal.overallRisk}, patient=${patient.overallRisk}")
    }

    // ─── 边界条件 ──────────────────────────────────────

    @Test
    fun `empty pressure snapshot handled gracefully`() {
        val snapshot = PressureSnapshot(
            timestamp = 0,
            grid = (0 until 12).map { PressurePoint(it / 3, it % 3, 0.0) }
        )
        val result = engine.estimate(snapshot, defaultProfile)
        assertNotNull(result)
        assertEquals(PostureState.NEUTRAL, result.postureState)
    }

    @Test
    fun `cumulative load index is positive`() {
        val result = engine.estimate(symmetricSnapshot(), defaultProfile)
        assertTrue(result.cumulativeLoadIndex > 0, "累积负荷应为正")
    }

    companion object {
        const val MODERATE_DISC_PRESSURE_KPA = 250.0
    }
}
