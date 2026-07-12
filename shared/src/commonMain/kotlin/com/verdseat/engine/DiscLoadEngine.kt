package com.verdseat.engine

import com.verdseat.VerdConfig.Disc.*
import com.verdseat.model.*
import kotlin.math.abs
import kotlin.math.min

/// 椎间盘负荷计算器 — 基于 Nachemson 经典生物力学模型
///
/// 核心公式:
///   discPressure = basePressure × sittingFactor × weightFactor × imbalanceFactor
///
/// 参考文献:
///   Nachemson A. "The load on lumbar disks in different positions of the body."
///   Clinical Orthopaedics, 1966
class DiscLoadEngine : IDiscLoadEngine {

    override fun estimate(snapshot: PressureSnapshot, profile: UserProfile): SpineAnalysis {
        val weightFactor = profile.weightKg / 70.0
        val bodyTypeFactor = profile.bodyType.discLoadMultiplier

        // 从坐垫压力推导骨盆状态
        val postureState = classifyPosture(snapshot)
        val anteriorShift = computeAnteriorShift(snapshot)
        val lateralImbalance = 1.0 - snapshot.gluteSymmetryIndex

        // 为每个椎间盘段估算压力
        val segmentPressures = DiscSegment.entries.map { segment ->
            val basePressure = estimateBasePressure(segment, snapshot, postureState)
            val adjusted = clampPressure(basePressure * weightFactor * bodyTypeFactor)
            val shearForce = estimateShearForce(segment, snapshot, postureState)

            segment to DiscPressureData(
                segment = segment,
                estimatedPressureKPa = adjusted,
                intraDiscLoadPercent = adjusted / segment.typicalIntradiscalPressureKPa * 100,
                shearForceN = shearForce,
                riskLevel = classifyPressure(adjusted, profile)
            )
        }.toMap()

        val peak = segmentPressures.values.maxBy { it.estimatedPressureKPa }
        val overallRisk = segmentPressures.values.maxOf { it.riskLevel }.let { maxRisk ->
            if (lateralImbalance > LATERAL_IMBALANCE_THRESHOLD) escalate(maxRisk)
            else maxRisk
        }

        return SpineAnalysis(
            timestamp = snapshot.timestamp,
            segments = segmentPressures,
            overallRisk = overallRisk,
            postureState = postureState,
            peakPressureSegment = peak.segment,
            peakPressureKPa = peak.estimatedPressureKPa,
            leftRightImbalancePercent = lateralImbalance * 100,
            anteriorShiftPercent = anteriorShift * 100,
            recommendedMassageMode = recommendMassage(overallRisk, peak.segment),
            estimatedBmi = profile.weightKg / ((profile.heightCm / 100.0) * (profile.heightCm / 100.0)),
            cumulativeLoadIndex = computeCumulativeLoad(segmentPressures.values)
        )
    }

    // ─── 坐姿分类 ──────────────────────────────────────

    private fun classifyPosture(snapshot: PressureSnapshot): PostureState {
        val symmetry = snapshot.gluteSymmetryIndex
        val ischial = snapshot.ischialPressure / 4.0  // 归一化到单点
        val coccyx = snapshot.coccyxPressure / 2.0

        if (symmetry < 0.6) return PostureState.LATERAL_SHIFT
        if (ischial > 2.0 && coccyx < 1.0) return PostureState.POSTERIOR_TILT
        if (coccyx > 2.5) return PostureState.ANTERIOR_TILT
        if (ischial < 1.5 && coccyx < 1.0) return PostureState.SLOUCHING
        return PostureState.NEUTRAL
    }

    private fun computeAnteriorShift(snapshot: PressureSnapshot): Double {
        val front = snapshot.grid.filter { it.row >= 2 }.sumOf { it.value }
        val back = snapshot.grid.filter { it.row <= 1 }.sumOf { it.value }
        val total = front + back
        return if (total < 1e-10) 0.0 else (front - back) / total
    }

    // ─── 椎间盘压力估算 ────────────────────────────────

    private fun estimateBasePressure(segment: DiscSegment, snapshot: PressureSnapshot, posture: PostureState): Double {
        // 从坐垫压力映射到椎间盘段的生物力学模型
        val sittingPressure = snapshot.grid.sumOf { it.value } / 12.0
        val postureMultiplier = when (posture) {
            PostureState.NEUTRAL -> 1.0
            PostureState.ANTERIOR_TILT -> 1.3
            PostureState.POSTERIOR_TILT -> 1.15
            PostureState.LATERAL_SHIFT -> 1.25
            PostureState.SLOUCHING -> 1.4
            PostureState.SCOLIOTIC -> 1.35
        }

        // 不同节段的基准压力分布 (L5-S1 最大, 向上递减)
        val segmentMultiplier = when (segment) {
            DiscSegment.L1_L2 -> 0.55
            DiscSegment.L2_L3 -> 0.65
            DiscSegment.L3_L4 -> 0.80
            DiscSegment.L4_L5 -> 1.0
            DiscSegment.L5_S1 -> 1.15
            DiscSegment.SACRUM -> 0.30
        }

        return segment.typicalIntradiscalPressureKPa * sittingPressure * postureMultiplier * segmentMultiplier
    }

    private fun estimateShearForce(segment: DiscSegment, snapshot: PressureSnapshot, posture: PostureState): Double {
        val asymmetry = 1.0 - snapshot.gluteSymmetryIndex
        val baseShear = segment.typicalIntradiscalPressureKPa * 0.03  // ~3% 转为剪切力
        return baseShear * (1.0 + asymmetry * 2.0) * when (posture) {
            PostureState.ANTERIOR_TILT -> 1.5
            PostureState.LATERAL_SHIFT -> 2.0
            PostureState.SCOLIOTIC -> 2.5
            else -> 1.0
        }
    }

    // ─── 风险分级 ──────────────────────────────────────

    private fun classifyPressure(kpa: Double, profile: UserProfile): SpinePressureLevel {
        val conditionFactor = if (profile.knownConditions.isNotEmpty()) 0.85 else 1.0
        val adjustedKpa = kpa * conditionFactor

        return when {
            adjustedKpa > CRITICAL_DISC_PRESSURE_KPA -> SpinePressureLevel.CRITICAL
            adjustedKpa > HIGH_DISC_PRESSURE_KPA -> SpinePressureLevel.HIGH
            adjustedKpa > MODERATE_DISC_PRESSURE_KPA -> SpinePressureLevel.MODERATE
            else -> SpinePressureLevel.LOW
        }
    }

    private fun escalate(current: SpinePressureLevel): SpinePressureLevel = when (current) {
        SpinePressureLevel.LOW -> SpinePressureLevel.MODERATE
        SpinePressureLevel.MODERATE -> SpinePressureLevel.HIGH
        else -> SpinePressureLevel.CRITICAL
    }

    private fun clampPressure(kpa: Double): Double = kpa.coerceIn(0.0, 800.0)

    private fun recommendMassage(risk: SpinePressureLevel, peakSegment: DiscSegment): MassageMode = when (risk) {
        SpinePressureLevel.LOW -> MassageMode.RELAX
        SpinePressureLevel.MODERATE -> MassageMode.STANDARD
        SpinePressureLevel.HIGH -> MassageMode.DEEP_TISSUE
        SpinePressureLevel.CRITICAL -> when (peakSegment) {
            DiscSegment.L4_L5, DiscSegment.L5_S1 -> MassageMode.SPOT
            else -> MassageMode.ACUPRESSURE
        }
    }

    private fun computeCumulativeLoad(segments: Collection<DiscPressureData>): Double {
        return segments.sumOf { it.estimatedPressureKPa * it.segment.typicalIntradiscalPressureKPa / 100.0 }
    }
}
