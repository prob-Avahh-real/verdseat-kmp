package com.verdseat.data

import kotlin.test.*

/// PressureFrameParser 测试 — 28 字节帧解析
class PressureFrameParserTest {

    @Test
    fun `parses valid 28-byte frame`() {
        val bytes = ByteArray(28) { i ->
            when {
                i < 24 && i % 2 == 0 -> 0x07  // hi byte = 7
                i < 24 && i % 2 == 1 -> 0xD0.toByte()  // lo byte = 208 → raw = 2000
                i == 24 -> 0x27  // temp hi
                i == 25 -> 0x10  // temp lo = 10000 → 100.00°C? weird but test
                i == 26 -> 85    // battery 85%
                i == 27 -> 42    // sequence 42
                else -> 0
            }
        }

        val result = PressureFrameParser.parse(bytes, 5000L)
        assertTrue(result.isSuccess, "解析应成功: ${result.exceptionOrNull()}")

        val snapshot = result.getOrThrow()
        assertEquals(5000L, snapshot.timestamp)
        assertEquals(12, snapshot.grid.size)
        // raw = (7<<8)|208 = 2000, value = 2000/4095 ≈ 0.4884
        assertTrue((snapshot.grid.first().value - 0.4884) < 0.01)
    }

    @Test
    fun `rejects frame smaller than 28 bytes`() {
        val bytes = ByteArray(16)
        val result = PressureFrameParser.parse(bytes, 0L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("帧大小不足"))
    }

    @Test
    fun `all zero frame gives all zero pressures`() {
        val bytes = ByteArray(28)
        val result = PressureFrameParser.parse(bytes, 0L)
        val snapshot = result.getOrThrow()
        assertTrue(snapshot.grid.all { it.value == 0.0 })
    }

    @Test
    fun `all max frame gives all 1_0 pressures`() {
        val bytes = ByteArray(28) { 0xFF.toByte() }
        val result = PressureFrameParser.parse(bytes, 0L)
        val snapshot = result.getOrThrow()
        assertTrue(snapshot.grid.all { it.value == 1.0 }, "全 0xFF 应全部为 1.0")
    }

    @Test
    fun `temperature extraction works`() {
        val bytes = ByteArray(28)
        bytes[24] = 0x09  // hi
        bytes[25] = 0xC4.toByte()  // lo = 196 → 0x09C4 = 2500 → 25.00°C
        assertEquals(25.0, PressureFrameParser.extractTemperature(bytes), 0.1)
    }

    @Test
    fun `battery extraction works`() {
        val bytes = ByteArray(28)
        bytes[26] = 72
        assertEquals(72, PressureFrameParser.extractBattery(bytes))
    }

    @Test
    fun `sequence number extraction works`() {
        val bytes = ByteArray(28)
        bytes[27] = 99
        assertEquals(99, PressureFrameParser.extractSequence(bytes))
    }
}
