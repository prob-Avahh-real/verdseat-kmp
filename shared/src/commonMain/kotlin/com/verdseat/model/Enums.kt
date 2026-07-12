package com.verdseat.model

/// 腰椎间盘节段 — L1 到 S1 共 6 节
enum class DiscSegment(val label: String, val index: Int, val typicalIntradiscalPressureKPa: Double) {
    L1_L2("L1-L2", 0, 200.0),
    L2_L3("L2-L3", 1, 230.0),
    L3_L4("L3-L4", 2, 280.0),
    L4_L5("L4-L5", 3, 350.0),  // ← 最常见突出部位
    L5_S1("L5-S1", 4, 400.0),  // ← 承重最大
    SACRUM("骶骨", 5, 100.0)    // ← 不直接承压, 但参与骨盆平衡
}

/// 脊椎压力等级
enum class SpinePressureLevel(val label: String, val emoji: String, val suggestion: String) {
    LOW("低压", "🟢", "当前椎间盘压力正常"),
    MODERATE("中压", "🟡", "建议微调骨盆前倾角"),
    HIGH("高压", "🟠", "建议起身活动并伸展腰椎"),
    CRITICAL("危险", "🔴", "请立即调整坐姿或站立！")
}

/// 坐姿状态 — 骨盆倾斜 + 胸椎旋转综合评估
enum class PostureState(val label: String, val description: String) {
    NEUTRAL("中立位", "骨盆中正, 胸椎竖直, 椎间盘压力均匀分布"),
    ANTERIOR_TILT("骨盆前倾", "腰椎曲度过大, L4-S1 压力集中"),
    POSTERIOR_TILT("骨盆后倾", "腰椎曲度消失, 椎间盘前部压力增大"),
    LATERAL_SHIFT("侧移", "骨盆左右不对称, 一侧椎间盘受压"),
    SCOLIOTIC("脊柱侧弯", "胸椎/腰椎 C 型或 S 型弯曲"),
    SLOUCHING("驼背", "胸椎后凸过度, 颈椎前探")
}

/// 坐垫按摩模式
enum class MassageMode(val label: String, val command: Int, val intensityPercent: Int) {
    OFF("关闭", 0, 0),
    RELAX("舒缓", 1, 30),
    STANDARD("标准", 2, 50),
    DEEP_TISSUE("深层", 3, 70),
    ACUPRESSURE("穴位", 4, 85),
    WAVE("波浪", 5, 60),
    SPOT("定点", 6, 90)
}

/// 用户体质类型 — 影响压力阈值
enum class BodyType(val label: String, val discLoadMultiplier: Double) {
    SLIM("偏瘦", 0.85),
    AVERAGE("标准", 1.0),
    ATHLETIC("运动型", 1.1),
    OVERWEIGHT("偏重", 1.25),
    OBESE("肥胖", 1.4)
}
