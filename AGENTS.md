# verdseat-kmp

VERD Seat (翠座) KMP 绿色智能坐垫 — Kotlin Multiplatform + Compose Multiplatform 功能 - BLE 连接 VERD 智能坐垫 (TI CC254x, 0xFFE0-0xFFE3 UUID) - 12 区域压力矩阵 (3×4 臀部分布) - 椎间盘压力估算 (6 节段: L1-S1, Nachemson 生物力学模型) - 坐姿分类 (中立/前倾/后倾/侧移/驼背/脊柱侧弯) - 实时坐姿纠正指导 (骨盆角度 + 重心偏移 + 语音提示)

## 用途与 Target 判定

Kotlin Multiplatform / Compose 工程（已归档）

- 移动端：需要
- 智能硬件：需要

## 目标平台

| 平台 | 方式 | 产出 |
|------|------|------|
| 移动端 | KMP | APK / 既有 iOS |
| 手表 / 戒指 / 传感器 | 按原设计 | — |

## 技术栈信号

- 检测到：ios, kmp

## 命令

```bash
./gradlew :composeApp:assembleDebug
```

## 状态

已归档。默认只做必要修复与知识库同步，不新开大范围需求。

## 助手约束

- Loop：先定可验证 Done 与停止条件；每切片过验证环；失败回灌；详见 docs/LOOP_ENGINEERING.md
- Harness：遵循根 AGENTS「Harness Engineering」；Guide=本文件，Sensor=测试/CI；同类失误二次须沉淀规则或测试（棘轮）。详见 docs/HARNESS_ENGINEERING.md
- 遵循仓库根 `/Users/skat/AGENTS.md` 与本文件的用途判定
- 可选 Target 可搁置；不适用形态不要硬加
- 改动聚焦；公共逻辑抽离复用
