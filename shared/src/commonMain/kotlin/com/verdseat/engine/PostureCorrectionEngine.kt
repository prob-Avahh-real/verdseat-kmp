package com.verdseat.engine

import com.verdseat.VerdConfig.Correction.*
import com.verdseat.model.*
import kotlin.math.abs
import kotlin.math.min

/// 坐姿纠正引擎 — 从 SpineAnalysis 生成 CorreectionGuidance
class PostureCorrectionEngine : IPostureCorrectionEngine {

    override fun computeGuidance(analysis: SpineAnalysis): CorrectionGuidance {
        val pelvicAdjust = computePelvicAdjustment(analysis)
        val lateralAdjust = computeLateralAdjustment(analysis)
        val shoulderAlign = computeShoulderAlignment(analysis)
        val verbal = generateGuidance(analysis, pelvicAdjust, lateralAdjust)

        return CorrectionGuidance(
            timestamp = analysis.timestamp,
            currentState = analysis.postureState,
            pelvicTiltAdjustDeg = pelvicAdjust,
            lateralShiftAdjustCm = lateralAdjust,
            shoulderAlignDeg = shoulderAlign,
            verbalGuidance = verbal
        )
    }

    private fun computePelvicAdjustment(analysis: SpineAnalysis): Double = when (analysis.postureState) {
        PostureState.ANTERIOR_TILT -> -min(abs(analysis.anteriorShiftPercent * 0.5), MAX_PELVIC_ADJUST_DEG)
        PostureState.POSTERIOR_TILT -> min(abs(analysis.anteriorShiftPercent * 0.5), MAX_PELVIC_ADJUST_DEG)
        PostureState.SLOUCHING -> -min(abs(analysis.anteriorShiftPercent * 0.3), MAX_PELVIC_ADJUST_DEG)
        else -> 0.0
    }

    private fun computeLateralAdjustment(analysis: SpineAnalysis): Double {
        val imbalance = analysis.leftRightImbalancePercent / 100.0
        if (imbalance < 0.15) return 0.0
        // 计算需移动的距离使左右平衡
        val adjust = imbalance * 10.0  // ~10cm 最大
        return adjust.coerceIn(-MAX_LATERAL_ADJUST_CM, MAX_LATERAL_ADJUST_CM)
    }

    private fun computeShoulderAlignment(analysis: SpineAnalysis): Double = when (analysis.postureState) {
        PostureState.SCOLIOTIC -> analysis.leftRightImbalancePercent * 0.3
        PostureState.LATERAL_SHIFT -> analysis.leftRightImbalancePercent * 0.2
        else -> 0.0
    }

    private fun generateGuidance(
        analysis: SpineAnalysis,
        pelvic: Double,
        lateral: Double
    ): String = when {
        analysis.isCritical -> "⚠️ ${analysis.postureState.description}。${analysis.peakPressureSegment.label} 压力过高，请立即调整坐姿！"
        abs(pelvic) > 5.0 -> "请将骨盆${if (pelvic < 0) "向后" else "向前"}倾约${"%.0f".format(abs(pelvic))}度，恢复到中立位"
        abs(lateral) > 1.0 -> "重心${if (lateral < 0) "偏左" else "偏右"}，请向${if (lateral < 0) "右" else "左"}移${"%.1f".format(abs(lateral))}cm"
        analysis.overallRisk == SpinePressureLevel.LOW -> "✓ 坐姿良好，继续保持"
        else -> "${analysis.postureState.description}，建议微调至中立位"
    }
}
