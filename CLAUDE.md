# CLAUDE.md — VERD Seat (翠座) KMP

智能坐垫 KMP 应用。BLE 连接 VERD 设备 → 12 区域压力矩阵 → 椎间盘压力估算 → 坐姿纠正。

## 命令

```bash
./gradlew :composeApp:assembleRelease
./gradlew :shared:allTests
```

## 架构

```
shared/commonMain/
├── platform/BleAdapter.kt        ← expect BLE 适配器
├── VerdConfig.kt                 ← 全局配置
├── model/
│   ├── Enums.kt                  ← DiscSegment, PostureState, MassageMode, BodyType
│   ├── Entities.kt               ← SpineAnalysis, CorrectionGuidance, UserProfile
│   └── PressureSnapshot.kt       ← 12 区域压力 (3×4)
├── engine/
│   ├── EngineInterfaces.kt       ← IDiscLoadEngine, IPostureCorrectionEngine, ISeatActuator
│   ├── DiscLoadEngine.kt         ← Nachemson 生物力学模型
│   └── PostureCorrectionEngine.kt ← 坐姿纠正算法
└── data/
    ├── VerdBleProtocol.kt        ← BLE 协议 + 命令编码
    ├── PressureFrameParser.kt    ← 28 字节帧解析 (Result 语义)
    ├── IVerdRepository.kt        ← 仓库接口
    ├── VerdRepository.kt         ← BLE → 压力流
    └── AppState.kt               ← ISessionStore + InMemorySessionStore
```

## 核心算法

椎间盘压力: discPressure = basePressure × sittingFactor × weightFactor × imbalanceFactor
坐姿分类: 尾骨压 + 坐骨结节压 + 臀对称度 三重判断
测试: 7 类 47 用例
