package com.verdseat.data

import com.verdseat.model.PressurePoint
import com.verdseat.model.PressureSnapshot

/// BLE 帧解析器 — 28 字节 → PressureSnapshot
///
/// 帧格式 (28 bytes):
///   [0..23]: 12 × uint16 BE (每点 0-4095, 归一化 0.0-1.0)
///   [24..25]: 温度 uint16 BE, 单位 0.01°C
///   [26]: 电池 0-100
///   [27]: 帧序号 0-255
object PressureFrameParser {
    private const val PRESSURE_COUNT = 12
    private const val FRAME_SIZE = 28
    private const val ADC_MAX = 4095.0

    fun parse(bytes: ByteArray, timestampMs: Long): Result<PressureSnapshot> {
        if (bytes.size < FRAME_SIZE) {
            return Result.failure(IllegalArgumentException(
                "帧大小不足: 期望 $FRAME_SIZE, 收到 ${bytes.size}"
            ))
        }

        val grid = buildList(PRESSURE_COUNT) {
            for (i in 0 until PRESSURE_COUNT) {
                val hi = bytes[i * 2].toInt() and 0xFF
                val lo = bytes[i * 2 + 1].toInt() and 0xFF
                val raw = (hi shl 8) or lo
                val value = (raw.toDouble() / ADC_MAX).coerceIn(0.0, 1.0)
                add(PressurePoint(row = i / 3, col = i % 3, value = value))
            }
        }

        return Result.success(PressureSnapshot(
            timestamp = timestampMs,
            grid = grid,
            rawBytes = bytes.copyOf()
        ))
    }

    /// 从原始帧提取传感器数据 (不解析压力)
    fun extractTemperature(bytes: ByteArray): Double {
        if (bytes.size < 26) return 0.0
        val hi = bytes[24].toInt() and 0xFF
        val lo = bytes[25].toInt() and 0xFF
        return ((hi shl 8) or lo) * 0.01
    }

    fun extractBattery(bytes: ByteArray): Int {
        if (bytes.size < 27) return 0
        return bytes[26].toInt() and 0xFF
    }

    fun extractSequence(bytes: ByteArray): Int {
        if (bytes.size < 28) return 0
        return bytes[27].toInt() and 0xFF
    }
}
