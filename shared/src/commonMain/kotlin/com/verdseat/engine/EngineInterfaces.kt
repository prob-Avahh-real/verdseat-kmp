package com.verdseat.engine

import com.verdseat.model.*

/// 椎间盘负荷计算引擎接口
interface IDiscLoadEngine {
    fun estimate(snapshot: PressureSnapshot, profile: UserProfile): SpineAnalysis
}

/// 坐姿纠正引擎接口
interface IPostureCorrectionEngine {
    fun computeGuidance(analysis: SpineAnalysis): CorrectionGuidance
}

/// 坐垫驱动控制接口
interface ISeatActuator {
    suspend fun setMassageMode(mode: MassageMode)
    suspend fun setHeater(enabled: Boolean, temp: Int)
    suspend fun triggerSpotMassage(segment: DiscSegment, durationSec: Int = 30)
}
