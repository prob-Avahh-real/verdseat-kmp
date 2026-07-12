package com.verdseat.data

import com.verdseat.model.*
import com.verdseat.platform.BleAdapter
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock

/// 坐垫数据仓库 — BLE → 压力数据流
class VerdRepository(private val ble: BleAdapter) : IVerdRepository {
    private var lastRxTime = 0L

    override fun pressureStream(): Flow<PressureSnapshot> = flow {
        ble.observeNotifications(
            VerdBleProtocol.PRESSURE_SERVICE,
            VerdBleProtocol.PRESSURE_CHAR
        ).collect { bytes ->
            val now = currentTimeMillis()
            if (now - lastRxTime >= VerdBleProtocol.DOWNSAMPLE_MS) {
                lastRxTime = now
                PressureFrameParser.parse(bytes, now)
                    .onSuccess { emit(it) }
                    .onFailure { /* 跳过损坏帧 */ }
            }
        }
    }

    override suspend fun setMassageMode(mode: MassageMode) =
        sendCommand(VerdBleProtocol.massageCommand(mode))

    override suspend fun setHeater(enabled: Boolean, tempC: Int) =
        sendCommand(VerdBleProtocol.heaterCommand(enabled, tempC))

    override suspend fun triggerSpotMassage(zone: Int, durationSec: Int) =
        sendCommand(VerdBleProtocol.spotMassageCommand(zone, durationSec))

    private suspend fun sendCommand(cmd: ByteArray) {
        ble.writeCharacteristic(VerdBleProtocol.DEVICE_SERVICE, VerdBleProtocol.COMMAND_CHAR, cmd)
    }

    companion object {
        fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    }
}
