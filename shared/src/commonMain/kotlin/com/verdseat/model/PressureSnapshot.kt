package com.verdseat.model

/// 单个压力传感点 — 坐垫 12 区域网格 (3 列 × 4 行)
///   col: 0=左臀, 1=正中(尾骨), 2=右臀
///   row: 0=后(腰椎L5-S1区), 1=中(坐骨结节), 2=前(大腿后侧), 3=最前
data class PressurePoint(val row: Int, val col: Int, val value: Double) {
    init {
        require(value in 0.0..1.0) { "压力值应在 [0,1] 范围, 实际: $value" }
    }
}

/// 12 区域压力快照
data class PressureSnapshot(
    val timestamp: Long,
    val grid: List<PressurePoint>,  // 3×4 = 12 points
    val rawBytes: ByteArray? = null  // 原始 BLE 帧, 用于调试
) {
    init {
        require(grid.size == 12) { "压力网格需 12 个点 (3×4), 实际: ${grid.size}" }
    }

    /// 左臀总压 (col=0)
    val leftGlutePressure: Double get() = grid.filter { it.col == 0 }.sumOf { it.value }

    /// 右臀总压 (col=2)
    val rightGlutePressure: Double get() = grid.filter { it.col == 2 }.sumOf { it.value }

    /// 臀压对称指数: 0=完全不对称, 1=完全对称
    val gluteSymmetryIndex: Double get() {
        val left = leftGlutePressure
        val right = rightGlutePressure
        val total = left + right
        return if (total < 1e-10) 1.0 else 1.0 - kotlin.math.abs(left - right) / total
    }

    /// 尾骨区压力 (col=1, row=0..1)
    val coccyxPressure: Double get() =
        grid.filter { it.col == 1 && it.row <= 1 }.sumOf { it.value }

    /// 坐骨结节区压力 (row=1, 所有列)
    val ischialPressure: Double get() =
        grid.filter { it.row == 1 }.sumOf { it.value }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PressureSnapshot) return false
        return timestamp == other.timestamp && grid == other.grid
    }

    override fun hashCode(): Int = timestamp.hashCode() * 31 + grid.hashCode()
}
