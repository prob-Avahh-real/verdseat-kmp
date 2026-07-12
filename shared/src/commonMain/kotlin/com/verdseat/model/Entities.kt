package com.verdseat.model

/// 腰椎间盘节段压力数据
data class DiscPressureData(
    val segment: DiscSegment,
    val estimatedPressureKPa: Double,        // 估算椎间盘内压 (kPa)
    val intraDiscLoadPercent: Double,        // 占体重百分比的负荷
    val shearForceN: Double,                 // 剪切力 (N)
    val riskLevel: SpinePressureLevel
)

/// 椎间盘负荷综合分析结果 — 核心输出
data class SpineAnalysis(
    val timestamp: Long,
    val segments: Map<DiscSegment, DiscPressureData>,  // 6 节段
    val overallRisk: SpinePressureLevel,
    val postureState: PostureState,
    val peakPressureSegment: DiscSegment,
    val peakPressureKPa: Double,
    val leftRightImbalancePercent: Double,    // 左右臀偏差百分比
    val anteriorShiftPercent: Double,          // 前倾偏移百分比
    val recommendedMassageMode: MassageMode,
    val estimatedBmi: Double = 0.0,
    val cumulativeLoadIndex: Double = 0.0     // 累积负荷指数
) {
    val isCritical: Boolean get() = overallRisk == SpinePressureLevel.CRITICAL
    val postureCorrectionAngle: Double get() = when (postureState) {
        PostureState.ANTERIOR_TILT -> -anteriorShiftPercent * 0.5
        PostureState.POSTERIOR_TILT -> anteriorShiftPercent * 0.5
        else -> 0.0
    }
}

/// 坐垫设备状态
data class DeviceState(
    val deviceId: String = "",
    val name: String = "VERD Seat",
    val connected: Boolean = false,
    val batteryLevel: Int = 0,
    val rssi: Int = 0,
    val firmwareVersion: String = "1.0.0",
    val massageMode: MassageMode = MassageMode.OFF,
    val heaterEnabled: Boolean = false,
    val heaterTempC: Int = 38,
    val pressureSampleRateHz: Int = 10
)

/// 坐姿纠正指令
data class CorrectionGuidance(
    val timestamp: Long,
    val currentState: PostureState,
    val targetState: PostureState = PostureState.NEUTRAL,
    val pelvicTiltAdjustDeg: Double = 0.0,     // 正=前倾, 负=后倾
    val lateralShiftAdjustCm: Double = 0.0,    // 正=右移, 负=左移
    val shoulderAlignDeg: Double = 0.0,
    val verbalGuidance: String = "保持当前坐姿"
) {
    fun toVoicePrompt(): String = when {
        kotlin.math.abs(pelvicTiltAdjustDeg) > 2.0 -> {
            val dir = if (pelvicTiltAdjustDeg > 0) "前倾" else "后倾"
            "请将骨盆${dir}${"%.0f".format(kotlin.math.abs(pelvicTiltAdjustDeg))}度"
        }
        kotlin.math.abs(lateralShiftAdjustCm) > 0.5 -> {
            val dir = if (lateralShiftAdjustCm > 0) "右" else "左"
            "请将重心向${dir}移${"%.1f".format(kotlin.math.abs(lateralShiftAdjustCm))}厘米"
        }
        else -> verbalGuidance
    }
}

/// 用户档案
data class UserProfile(
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val age: Int = 30,
    val sex: Sex = Sex.OTHER,
    val bodyType: BodyType = BodyType.AVERAGE,
    val knownConditions: List<SpineCondition> = emptyList()
)

enum class Sex(val label: String) { MALE("男"), FEMALE("女"), OTHER("其他") }

enum class SpineCondition(val label: String, val severity: Int) {
    HERNIATED_DISC("椎间盘突出", 3),
    DEGENERATIVE_DISC("椎间盘退变", 2),
    SPONDYLOLISTHESIS("脊椎滑脱", 4),
    SCOLIOSIS("脊柱侧弯", 3),
    STENOSIS("椎管狭窄", 3)
}
