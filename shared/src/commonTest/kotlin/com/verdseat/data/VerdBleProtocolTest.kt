package com.verdseat.data

import com.verdseat.model.MassageMode
import kotlin.test.*

/// VerdBleProtocol 命令协议测试
class VerdBleProtocolTest {

    @Test
    fun `massage command encodes correctly`() {
        MassageMode.entries.forEach { mode ->
            val cmd = VerdBleProtocol.massageCommand(mode)
            assertEquals(2, cmd.size)
            assertEquals(0xB0.toByte(), cmd[0])
            assertEquals(mode.command.toByte(), cmd[1])
        }
    }

    @Test
    fun `heater command clamps temperature`() {
        val cold = VerdBleProtocol.heaterCommand(true, 10)
        assertEquals(35, cold[2].toInt() and 0xFF, "低温应钳制到 35")

        val hot = VerdBleProtocol.heaterCommand(true, 80)
        assertEquals(45, hot[2].toInt() and 0xFF, "高温应钳制到 45")

        val normal = VerdBleProtocol.heaterCommand(true, 40)
        assertEquals(40, normal[2].toInt() and 0xFF)
    }

    @Test
    fun `heater disabled sends zero enable byte`() {
        val cmd = VerdBleProtocol.heaterCommand(false, 40)
        assertEquals(0, cmd[1].toInt() and 0xFF)
    }

    @Test
    fun `spot massage clamps zone index`() {
        val overflow = VerdBleProtocol.spotMassageCommand(50, 30)
        assertEquals(11, overflow[1].toInt() and 0xFF, "zone 应钳制到 11")

        val underflow = VerdBleProtocol.spotMassageCommand(-5, 30)
        assertEquals(0, underflow[1].toInt() and 0xFF, "zone 应钳制到 0")
    }

    @Test
    fun `spot massage clamps duration`() {
        val tooShort = VerdBleProtocol.spotMassageCommand(3, 5)
        assertEquals(10, tooShort[2].toInt() and 0xFF)

        val tooLong = VerdBleProtocol.spotMassageCommand(3, 300)
        assertEquals(120, tooLong[2].toInt() and 0xFF)
    }

    @Test
    fun `UUIDs are correct TI format`() {
        assertTrue(VerdBleProtocol.DEVICE_SERVICE.contains("0000ffe0"))
        assertTrue(VerdBleProtocol.COMMAND_CHAR.contains("0000ffe1"))
        assertTrue(VerdBleProtocol.PRESSURE_SERVICE.contains("0000ffe2"))
        assertTrue(VerdBleProtocol.PRESSURE_CHAR.contains("0000ffe3"))
    }
}
