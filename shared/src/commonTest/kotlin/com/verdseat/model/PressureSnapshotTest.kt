package com.verdseat.model

import kotlin.test.*

/// 压力快照计算属性测试
class PressureSnapshotTest {

    @Test
    fun `grid size is exactly 12`() {
        val snapshot = symmetricSnapshot()
        assertEquals(12, snapshot.grid.size)
    }

    @Test
    fun `left and right glute pressures are correct`() {
        // left col=0, right col=2
        val grid = listOf(
            PressurePoint(0, 0, 0.8), PressurePoint(0, 1, 0.3), PressurePoint(0, 2, 0.2),
            PressurePoint(1, 0, 0.9), PressurePoint(1, 1, 0.4), PressurePoint(1, 2, 0.1),
            PressurePoint(2, 0, 0.1), PressurePoint(2, 1, 0.1), PressurePoint(2, 2, 0.1),
            PressurePoint(3, 0, 0.1), PressurePoint(3, 1, 0.1), PressurePoint(3, 2, 0.1),
        )
        val snapshot = PressureSnapshot(0, grid)

        assertEquals(0.8 + 0.9 + 0.1 + 0.1, snapshot.leftGlutePressure, 0.01)
        assertEquals(0.2 + 0.1 + 0.1 + 0.1, snapshot.rightGlutePressure, 0.01)
    }

    @Test
    fun `symmetric pressure gives symmetry index near 1_0`() {
        val snapshot = symmetricSnapshot()
        assertTrue(snapshot.gluteSymmetryIndex > 0.95)
    }

    @Test
    fun `fully asymmetric gives symmetry index near 0_0`() {
        val grid = (0 until 12).map {
            PressurePoint(it / 3, it % 3, if (it % 3 == 0) 1.0 else 0.0)  // 仅左列有压力
        }
        val snapshot = PressureSnapshot(0, grid)
        assertTrue(snapshot.gluteSymmetryIndex < 0.05)
    }

    @Test
    fun `coccyx pressure is sum of col=1 row 0 and 1`() {
        val grid = listOf(
            PressurePoint(0, 0, 0.1), PressurePoint(0, 1, 0.5), PressurePoint(0, 2, 0.1),
            PressurePoint(1, 0, 0.1), PressurePoint(1, 1, 0.6), PressurePoint(1, 2, 0.1),
            PressurePoint(2, 0, 0.1), PressurePoint(2, 1, 0.1), PressurePoint(2, 2, 0.1),
            PressurePoint(3, 0, 0.1), PressurePoint(3, 1, 0.1), PressurePoint(3, 2, 0.1),
        )
        val snapshot = PressureSnapshot(0, grid)
        assertEquals(1.1, snapshot.coccyxPressure, 0.01)
    }

    @Test
    fun `ischial pressure is row 1 total`() {
        val snapshot = symmetricSnapshot()
        val expected = snapshot.grid.filter { it.row == 1 }.sumOf { it.value }
        assertEquals(expected, snapshot.ischialPressure, 0.01)
    }

    @Test
    fun `Validation rejects out of range pressure values`() {
        assertFailsWith<IllegalArgumentException> {
            PressurePoint(0, 0, 1.5)
        }
        assertFailsWith<IllegalArgumentException> {
            PressurePoint(0, 0, -0.1)
        }
    }

    private fun symmetricSnapshot() = PressureSnapshot(
        timestamp = 0,
        grid = (0 until 12).map { PressurePoint(it / 3, it % 3, 0.5) }
    )
}
