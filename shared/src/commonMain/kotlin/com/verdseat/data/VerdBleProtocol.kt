package com.verdseat.data

import com.verdseat.model.MassageMode

/// BLE 协议常量 — VERD Seat TI CC254x 自定义 GATT
///
/// 12 区域压力帧格式 (28 bytes):
///   bytes[0..23]: 12 个 uint16 压力值 (每点 0-4095, 12-bit ADC)
///   bytes[24..25]: 温度 (uint16, 0.01°C 精度)
///   bytes[26]: 电池电量 (0-100%)
///   bytes[27]: 帧序号 (0-255 循环)
///
/// 命令帧格式:
///   byte[0]: 命令头
///     - 0xB0: 按摩模式
///     - 0xB1: 加热控制
///     - 0xB2: 定点按摩 (区域 0-11)
///   byte[1]: 参数
///   byte[2..]: 额外参数
object VerdBleProtocol {
    const val DEVICE_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb"
    const val COMMAND_CHAR = "0000ffe1-0000-1000-8000-00805f9b34fb"
    const val PRESSURE_SERVICE = "0000ffe2-0000-1000-8000-00805f9b34fb"
    const val PRESSURE_CHAR = "0000ffe3-0000-1000-8000-00805f9b34fb"
    const val DEVICE_NAME_PREFIX = "VERD"
    const val FRAME_SIZE = 28
    const val DOWNSAMPLE_MS = 100L

    fun massageCommand(mode: MassageMode): ByteArray =
        byteArrayOf(0xB0.toByte(), mode.command.toByte())

    fun heaterCommand(enabled: Boolean, tempC: Int): ByteArray =
        byteArrayOf(0xB1.toByte(), (if (enabled) 1 else 0).toByte(), tempC.coerceIn(35, 45).toByte())

    fun spotMassageCommand(zoneIndex: Int, durationSec: Int): ByteArray =
        byteArrayOf(0xB2.toByte(), zoneIndex.coerceIn(0, 11).toByte(), durationSec.coerceIn(10, 120).toByte())
}
