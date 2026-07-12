package com.verdseat

/// VERD Seat 绿色智能坐垫 — 全局配置
object VerdConfig {
    const val APP_NAME = "VERD Seat"
    const val APP_NAME_CN = "翠座"
    const val BUNDLE_ID = "com.verdseat.app"

    object Ble {
        const val DEVICE_PREFIX = "VERD"
        const val DOWNSAMPLE_HZ = 10
        const val DOWNSAMPLE_MS = 100L
    }

    object Pressure {
        const val GRID_ROWS = 4
        const val GRID_COLS = 3
        const val GRID_SIZE = 12       // 3×4

        // 体重归一化参考
        const val REFERENCE_WEIGHT_KG = 70.0
    }

    object Disc {
        // 坐姿压力 → 椎间盘内压换算系数 (Nachemson 经典研究)
        const val SITTING_DISC_PRESSURE_FACTOR = 1.4    // 坐姿比站立高 40%
        const val STANDING_DISC_PRESSURE_FACTOR = 1.0
        const val LYING_DISC_PRESSURE_FACTOR = 0.25

        // L4-L5 和 L5-S1 是最关键的监控段
        const val CRITICAL_DISC_PRESSURE_KPA = 530.0     // 超过此值危险
        const val HIGH_DISC_PRESSURE_KPA = 400.0
        const val MODERATE_DISC_PRESSURE_KPA = 250.0

        // 左右不平衡阈值
        const val LATERAL_IMBALANCE_THRESHOLD = 0.15     // >15% 触发警告
    }

    object Correction {
        const val MAX_PELVIC_ADJUST_DEG = 15.0
        const val MAX_LATERAL_ADJUST_CM = 3.0
        const val GUIDANCE_UPDATE_INTERVAL_S = 5.0
    }

    object Session {
        const val MAX_RECORDS = 70000
        const val SAMPLE_INTERVAL_S = 2.0
    }
}
