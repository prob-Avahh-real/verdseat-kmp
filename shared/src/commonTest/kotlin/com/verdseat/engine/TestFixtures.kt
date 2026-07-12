package com.verdseat.engine

import com.verdseat.model.*

/// 测试夹具 — 生成各种坐姿的压力快照
object TestFixtures {

    /// 完全对称的中立坐姿
    fun symmetricSnapshot() = pressureSnapshot(0.5, 0.5)

    /// 骨盆前倾 (尾骨区压力大)
    fun anteriorTiltSnapshot() = pressureSnapshot(
        leftGlute = 0.4, rightGlute = 0.4,
        coccyx = 1.0, ischial = 0.8
    )

    /// 左右不对称 (左重右轻)
    fun lateralShiftSnapshot() = pressureSnapshot(
        leftGlute = 1.0, rightGlute = 0.2,
        coccyx = 0.5, ischial = 0.7
    )

    /// 高压极端
    fun extremePressureSnapshot() = pressureSnapshot(
        leftGlute = 1.0, rightGlute = 1.0,
        coccyx = 1.0, ischial = 1.0
    )

    /// 中等不对称
    fun moderateAsymmetrySnapshot() = pressureSnapshot(
        leftGlute = 0.7, rightGlute = 0.3,
        coccyx = 0.6, ischial = 0.5
    )

    /// 生成 3×4 压力网格
    ///   row 0=后(尾骨/L5-S1), 1=中(坐骨结节), 2=前(大腿后侧), 3=最前
    ///   col 0=左臀, 1=正中, 2=右臀
    fun pressureSnapshot(
        leftGlute: Double = 0.5,
        rightGlute: Double = 0.5,
        coccyx: Double = 0.5,
        ischial: Double = 0.5
    ): PressureSnapshot {
        val grid = mutableListOf<PressurePoint>()
        for (row in 0 until 4) {
            for (col in 0 until 3) {
                val value = when {
                    col == 0 -> leftGlute * (1.0 - row * 0.15)
                    col == 2 -> rightGlute * (1.0 - row * 0.15)
                    row <= 1 -> coccyx * (1.0 - row * 0.1)
                    else -> ischial * (1.0 - row * 0.1) * 0.5
                }.coerceIn(0.0, 1.0)
                grid.add(PressurePoint(row, col, value))
            }
        }
        return PressureSnapshot(timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(), grid = grid)
    }
}
