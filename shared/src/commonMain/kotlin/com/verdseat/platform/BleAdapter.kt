package com.verdseat.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/// BLE 扫描结果
data class ScanResult(val name: String?, val address: String, val rssi: Int)

/// BLE 适配器接口 — expect/actual 跨平台实现
expect class BleAdapter {
    fun scan(): Flow<ScanResult>
    suspend fun connect(address: String)
    fun disconnect()
    fun observeNotifications(serviceUuid: String, charUuid: String): Flow<ByteArray>
    suspend fun writeCharacteristic(serviceUuid: String, charUuid: String, data: ByteArray)
    val connectedDevice: StateFlow<String?>
}
