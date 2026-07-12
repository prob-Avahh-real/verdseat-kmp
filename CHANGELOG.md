# VERD Seat (翠座) KMP — Engineering Hardening v2

**日期:** 2026-06-07

## 抽象重构
- ✅ IDiscLoadEngine (策略模式, Nachemson 生物力学模型)
- ✅ IPostureCorrectionEngine (坐姿纠正)
- ✅ ISeatActuator (按摩 + 加热控制)
- ✅ IVerdRepository 接口
- ✅ ISessionStore / InMemorySessionStore
- ✅ BleAdapter expect 独立到 platform/
- ✅ VerdBleProtocol 独立
- ✅ PressureFrameParser 独立 (Result 语义, 28 字节帧)
- ✅ VerdViewModel 层
- ✅ 4 Tab UI (含脊椎压力可视化 + 纠正指导卡片)

## 安全修复
- ✅ AndroidManifest BLE 权限完整
- ✅ iOS Info.plist 蓝牙描述
- ✅ 加热温度钳制 35-45°C

## 测试
- ✅ 7 测试类, 47 用例

## 审计
- [安全审计报告](code-review-reports/verdseat-kmp-security-audit-2026-06-07.md)

## CI/CD
- ✅ assembleRelease + allTests
