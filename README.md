# VERD Seat (翠座) KMP

绿色智能坐垫 — Kotlin Multiplatform + Compose Multiplatform

## 功能

- BLE 连接 VERD 智能坐垫 (TI CC254x, 0xFFE0-0xFFE3 UUID)
- 12 区域压力矩阵 (3×4 臀部分布)
- 椎间盘压力估算 (6 节段: L1-S1, Nachemson 生物力学模型)
- 坐姿分类 (中立/前倾/后倾/侧移/驼背/脊柱侧弯)
- 实时坐姿纠正指导 (骨盆角度 + 重心偏移 + 语音提示)
- 按摩模式控制 (7 种: 舒缓/标准/深层/穴位/波浪/定点)
- 加热控制 (35-45°C) + 定点按摩
- 体质适配 (5 种体型系数 + 已知脊椎疾病风险调整)

## 架构

```
verdseat-kmp/
├── shared/
│   ├── commonMain/
│   │   ├── platform/BleAdapter.kt        ← expect BLE 适配器
│   │   ├── VerdConfig.kt                 ← 全局配置
│   │   ├── model/
│   │   │   ├── Enums.kt                  ← 枚举 (DiscSegment, PostureState, etc.)
│   │   │   ├── Entities.kt               ← 数据模型 (SpineAnalysis, etc.)
│   │   │   └── PressureSnapshot.kt       ← 压力快照 (12 点)
│   │   ├── engine/
│   │   │   ├── EngineInterfaces.kt       ← IDiscLoadEngine, IPostureCorrectionEngine
│   │   │   ├── DiscLoadEngine.kt         ← Nachemson 椎间盘压力模型
│   │   │   └── PostureCorrectionEngine.kt ← 坐姿纠正算法
│   │   └── data/
│   │       ├── VerdBleProtocol.kt        ← BLE 协议 + 命令编码
│   │       ├── PressureFrameParser.kt    ← 28 字节帧解析
│   │       ├── IVerdRepository.kt        ← 仓库接口
│   │       ├── VerdRepository.kt         ← BLE → 压力流
│   │       └── AppState.kt               ← 状态 + ISessionStore
│   ├── androidMain/ — BLE via Kable
│   └── iosMain/ — BLE via Kable (CoreBluetooth)
├── composeApp/
│   ├── ui/viewmodel/VerdViewModel.kt     ← ViewModel 编排层
│   └── ui/screens/VerdApp.kt             ← 4 Tab UI + 脊椎可视化
└── .github/workflows/ci.yml
```

## 接口抽象

| 接口 | 实现 | 可替换策略 |
|------|------|-----------|
| `IDiscLoadEngine` | `DiscLoadEngine` (Nachemson) | ML 模型, 有限元分析 |
| `IPostureCorrectionEngine` | `PostureCorrectionEngine` | AI 教练, 物理治疗师 |
| `IVerdRepository` | `VerdRepository` | 模拟仓库, 云端同步 |
| `ISessionStore` | `InMemorySessionStore` | SQLite, 远程存储 |

## 构建

```bash
./gradlew :composeApp:assembleRelease    # Android APK
./gradlew :shared:allTests               # 测试 (6 测试类, 47 用例)
```

## 测试覆盖

| 模块 | 测试数 | 覆盖点 |
|------|--------|--------|
| DiscLoadEngine | 14 | 6 节段, 5 坐姿, 体重, 体型, 临界值 |
| PostureCorrectionEngine | 8 | 中立/前倾/后倾/侧移, 钳制, 语音提示 |
| PressureFrameParser | 7 | 28 字节帧, 温度/电池/序列号解析 |
| InMemorySessionStore | 5 | CRUD, 淘汰, 今日筛选 |
| PressureSnapshot | 7 | 计算属性, 对称度, 尾骨压, 输入校验 |
| VerdBleProtocol | 6 | 命令编码, 温度/zone/duration 钳制 |
| Entities | 9 | 枚举完整性, 默认值, VoicePrompt |

## 与 lumiposture 联动 (可选)

通过本地 BLE 或共享 AppState，翠座可为腰托提供坐姿分析输入，
实现坐垫检测 + 腰托纠正的闭环。
