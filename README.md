# 汽水音乐广告助手

## 项目简介

汽水音乐广告助手是一款Android无障碍服务应用，可以自动检测并跳过汽水音乐（com.netease.cloudmusic）中的广告，提升用户体验。

## 主要功能

- 🎯 **智能广告检测**：自动识别广告弹窗、领取按钮、倒计时等元素
- ⚡ **一键跳过**：自动点击跳过按钮，无需手动操作
- 🔒 **后台保活**：使用前台服务+WakeLock确保稳定运行
- 📊 **统计功能**：记录广告跳过次数
- 🎨 **清新界面**：简洁美观的UI设计

## 检测优先级

1. 弹窗广告关闭
2. 领取成功关闭
3. 领取奖励按钮
4. 倒计时跳过

## 广告关键词

- 领取奖励、继续观看、再领一次
- 领取成功、已领取、可领取奖励
- 秒后可领取、倒计时

## 技术栈

- **语言**：Kotlin
- **最低SDK**：29 (Android 10)
- **目标SDK**：36
- **架构**：MVVM
- **UI**：ViewBinding + Material Design

## 文件结构

```
app/src/main/
├── java/com/gufeng/adhelper/
│   ├── service/          # 服务类
│   │   ├── AdAccessibilityService.kt  # 无障碍服务
│   │   ├── KeepAliveService.kt        # 前台服务
│   │   └── FloatWindowService.kt     # 悬浮窗服务
│   ├── detector/         # 广告检测
│   │   ├── AdDetector.kt   # 广告检测器
│   │   ├── StateMachine.kt # 状态机
│   │   └── NodeClicker.kt # 节点点击
│   ├── keepalive/       # 保活工具
│   │   ├── BatteryOptimizationHelper.kt
│   │   └── ManufacturerAdapter.kt
│   ├── ui/              # 界面
│   │   └── MainActivity.kt
│   ├── viewmodel/       # ViewModel
│   ├── receiver/        # 广播接收器
│   ├── utils/           # 工具类
│   └── AdHelperApplication.kt
└── res/                 # 资源文件
```

## 权限说明

- `BIND_ACCESSIBILITY_SERVICE`：无障碍服务核心权限
- `FOREGROUND_SERVICE`：前台服务
- `SYSTEM_ALERT_WINDOW`：悬浮窗
- `WAKE_LOCK`：保持唤醒
- `RECEIVE_BOOT_COMPLETED`：开机自启

## 使用方法

1. 安装应用
2. 开启无障碍服务
3. 开启后台保活（推荐）
4. 授权悬浮窗权限（可选）
5. 关闭电池优化（推荐）
6. 打开汽水音乐，应用会自动跳过广告

## 作者

**古封**

## 许可证

MIT License
