# 欢乐斗地主（Happy Poker）

一款使用 Kotlin + Jetpack Compose 开发的 Android 横屏斗地主游戏，支持单机人机对战、局域网联机对战、音效/震动设置、头像昵称配置和豆子结算。

当前版本：`1.0.3`

## 功能特性

- 经典斗地主流程：发牌、叫地主、底牌、出牌、不出、结算。
- 单机模式：支持本地人机对战和 AI 自动行动。
- 联机模式：基于 MQTT 的局域网创建房间、加入房间、等待开局和牌局同步。
- 两人联机：两名真人进入后自动补一个电脑玩家，组成标准三人牌局。
- 玩家系统：支持昵称、头像、豆子余额、胜负结算和主页展示。
- 视听反馈：支持按钮音效、出牌语音、炸弹/火箭/春天特效、胜负音效和震动开关。
- 横屏界面：启动页、主页、单机牌局、联机大厅、创建房间、等待房、设置页和结算页均按横屏牌桌布局适配。

## 技术栈

| 模块 | 技术 | 说明 |
| --- | --- | --- |
| Android 客户端 | Kotlin + Jetpack Compose | 主界面、牌局界面、设置页和联机页面 |
| 核心逻辑 | Kotlin/JVM | 牌型、规则、AI、状态流转 |
| 网络通信 | MQTT / Eclipse Paho | 局域网房间和牌局消息同步 |
| 异步处理 | Kotlin Coroutines / Flow | ViewModel 状态、MQTT 消息和倒计时 |
| 序列化 | kotlinx.serialization | 联机协议消息编解码 |

## 项目结构

```text
happy-poker/
├── app/                         # Android 客户端
│   ├── src/main/kotlin/
│   │   └── com/happy/poker/app/
│   │       ├── navigation/       # 页面路由
│   │       ├── network/          # App 侧 MQTT 配置
│   │       ├── progress/         # 玩家豆子进度
│   │       ├── settings/         # 设置项和触感反馈
│   │       ├── sound/            # 音效播放
│   │       ├── ui/               # Compose UI
│   │       └── viewmodel/        # 单机/联机状态管理
│   └── src/main/res/            # 图片、音效、主题和应用图标
├── core/                        # 纯 Kotlin 核心模块
│   └── src/main/kotlin/
│       └── com/happy/poker/core/
│           ├── ai/               # AI 策略
│           ├── flow/             # 游戏流程
│           ├── model/            # 卡牌、玩家、房间模型
│           ├── network/          # MQTT 协议和客户端封装
│           └── rules/            # 发牌、叫分、出牌和牌型校验
├── docs/                        # 开发计划文档
└── publish/                     # 发布日志
```

## 环境要求

- JDK 17
- Android Studio 或 Android SDK/Gradle 命令行环境
- Android 7.0+（`minSdk = 24`）
- 局域网联机需要可访问的 MQTT Broker，例如 Mosquitto

## 构建与运行

调试包：

```bash
./gradlew assembleDebug
```

Release 包：

```bash
./gradlew --no-daemon --max-workers=2 assembleRelease
```

核心模块测试：

```bash
./gradlew :core:test
```

安装到已连接设备：

```bash
./gradlew installDebug
```

## 联机配置

联机模式通过 MQTT 在局域网内同步房间和牌局消息。默认配置位于：

```text
app/src/main/kotlin/com/happy/poker/app/network/MqttConfigManager.kt
```

当前默认值：

- Broker：`tcp://127.0.0.1:1883`
- 用户名：`mqtt`
- 密码：`123456`
- QoS：`1`
- 自动重连：开启

也可以在应用内进入“设置中心 > 网络配置”修改 Broker 地址、账号、密码、客户端 ID 前缀、QoS、连接超时和心跳间隔。

Mosquitto 示例配置要点：

```conf
listener 1883
allow_anonymous false
password_file /etc/mosquitto/passwd
```

确认服务监听：

```bash
ss -ltnp | grep 1883
```

手机真机测试时，需要确保：

- 手机和 MQTT Broker 在同一局域网。
- Broker 监听地址不是 `127.0.0.1` 或 `::1`。
- 防火墙允许访问 `1883` 端口。
- 应用网络配置中的 IP 是电脑的局域网 IPv4 地址。

## 发布说明

版本号维护在：

```text
app/build.gradle.kts
```

发布日志维护在：

```text
publish/changeLog.md
```

Release 签名支持两种方式：

- CI 环境：通过 `KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEYSTORE_KEY_ALIAS`、`KEYSTORE_KEY_PASSWORD` 环境变量读取。
- 本地环境：通过根目录 `keystore.properties` 读取。

## 常见排查

### 联机页面进入较卡

优先检查 MQTT Broker 是否可达，以及局域网内是否有多个旧版本客户端同时运行。当前实现已避免订阅整个 `happy-poker/#`，只按大厅、等待房和牌局阶段订阅必要 topic。

### 真机无法创建或加入房间

检查应用内网络配置、Broker 监听地址和端口、防火墙状态，以及手机是否能访问电脑的局域网 IP。

### 页面出现黑边或布局错位

项目按横屏适配，入口 Activity 和主 Activity 均固定横屏。真机调试时建议使用横屏方向，并优先检查不同分辨率下的顶部按钮、出牌区、底牌区和底部操作按钮。

## 相关文档

- 开发计划：`docs/DEVELOPMENT_PLAN.md`
- 发布日志：`publish/changeLog.md`
