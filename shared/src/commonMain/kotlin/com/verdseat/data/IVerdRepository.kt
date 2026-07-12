package com.verdseat.data

import com.verdseat.model.*
import kotlinx.coroutines.flow.Flow

/// 坐垫数据仓库接口
interface IVerdRepository {
    fun pressureStream(): Flow<PressureSnapshot>
    suspend fun setMassageMode(mode: MassageMode)
    suspend fun setHeater(enabled: Boolean, tempC: Int)
    suspend fun triggerSpotMassage(zone: Int, durationSec: Int)
}
