# 欢乐斗地主 - 开发计划

## 一、项目概述

### 1.1 项目名称
欢乐斗地主（Happy Poker）

### 1.2 项目目标
开发一款支持局域网多人对战 + 人机对战的经典欢乐斗地主游戏。

### 1.3 核心功能
- 经典斗地主玩法（叫地主、底牌、地主1v2农民）
- 单机人机对战（AI）
- 局域网多人对战
- 混合模式（人+机组队）
- 断线重连
- 倍数系统（炸弹/春天翻倍）

### 1.5 UI设计原则
**界面完全仿照欢乐斗地主经典模式**，包括：
- 牌桌布局（玩家位置、手牌区域）
- 牌面样式（花色、点数显示）
- 操作按钮（叫地主、出牌、不出等）
- 动画效果（出牌、炸弹、春天等）
- 音效提示（可选）

由于没有UI素材，需要：
1. 参考欢乐斗地主截图/视频设计UI
2. 使用Compose Canvas绘制牌面
3. 使用矢量图形或自绘UI元素
4. 保持与原版相似的配色和布局

### 1.4 目标平台
- Android（主要）

---

## 二、技术架构

### 2.1 技术栈
| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 客户端 | Kotlin + Jetpack Compose | 现代Android UI |
| 核心逻辑 | 纯 Kotlin/JVM | 可复用到服务器 |
| 网络通信 | MQTT | 轻量级，适合移动端 |
| MQTT Broker | EMQX / Mosquitto | 消息中间件 |
| 依赖注入 | Koin | 轻量级DI |
| 异步处理 | Kotlin Coroutines | 协程 |

### 2.2 项目结构
```
happy-poker/
├── core/                  # 核心逻辑层（纯Kotlin）
│   ├── model/            # 数据模型
│   ├── rules/            # 游戏规则
│   ├── ai/               # AI策略
│   └── network/          # 协议定义
│
├── app/                  # Android客户端
│   ├── ui/               # Compose界面
│   ├── viewmodel/        # 状态管理
│   └── network/          # MQTT客户端
│
└── docs/                 # 文档
```

---

## 三、核心数据模型

### 3.1 牌模型

```kotlin
enum class Suit(val symbol: String) {
    Spades("♠"), Hearts("♥"), Diamonds("♦"), Clubs("♣"), Joker("🃏")
}

enum class Rank(val value: Int, val label: String) {
    Three(3, "3"), Four(4, "4"), Five(5, "5"), Six(6, "6"),
    Seven(7, "7"), Eight(8, "8"), Nine(9, "9"), Ten(10, "10"),
    Jack(11, "J"), Queen(12, "Q"), King(13, "K"), Ace(14, "A"),
    Two(15, "2"),
    SmallJoker(16, "小王"), BigJoker(17, "大王")
}

data class Card(
    val rank: Rank,
    val suit: Suit,
    val id: String  // 唯一标识，如 "3♠"
)
```

### 3.2 玩家模型

```kotlin
enum class PlayerRole {
    Unknown,    // 未定
    Landlord,   // 地主
    Farmer      // 农民
}

data class Player(
    val id: String,
    val name: String,
    val role: PlayerRole = PlayerRole.Unknown,
    val hand: MutableList<Card> = mutableListOf(),
    val isReady: Boolean = false,
    val isOnline: Boolean = true,
    val score: Int = 0
)
```

### 3.3 房间模型

```kotlin
enum class RoomState {
    Waiting,    // 等待玩家
    Bidding,    // 叫地主中
    Playing,    // 游戏中
    Finished    // 已结束
}

data class Room(
    val id: String,
    val name: String,
    val hostId: String,
    val state: RoomState = RoomState.Waiting,
    val players: List<Player> = emptyList(),
    val maxPlayers: Int = 3,
    val currentBid: Int = 0,
    val landlordId: String? = null,
    val deck: List<Card> = emptyList(),
    val bottomCards: List<Card> = emptyList(),
    val multiplier: Int = 1,
    val turnHistory: List<TurnRecord> = emptyList()
)
```

### 3.4 牌型定义

```kotlin
enum class PatternType {
    Single,           // 单张
    Pair,             // 对子
    Triple,           // 三条
    TripleWithOne,    // 三带一
    TripleWithPair,   // 三带二
    Straight,         // 顺子（>=5张）
    ConsecutivePairs, // 连对（>=3对）
    Plane,            // 飞机（>=2个三条）
    PlaneWithWings,   // 飞机带翅膀
    Bomb,             // 炸弹（4张）
    Rocket,           // 火箭（大小王）
    FourWithTwo,      // 四带二
    FourWithPairs     // 四带两对
}

data class HandPattern(
    val type: PatternType,
    val mainRank: Rank,
    val cardCount: Int,
    val groupCount: Int = 0
)
```

---

## 四、MQTT 通信协议

### 4.1 Topic 结构

```
happy-poker/
├── room/{roomId}/
│   ├── join               # 加入房间
│   ├── leave              # 离开房间
│   ├── ready              # 准备
│   ├── bid                # 叫地主
│   ├── play               # 出牌
│   ├── pass               # 不出
│   ├── state              # 状态同步
│   ├── game_start         # 游戏开始
│   ├── bid_turn           # 轮到叫地主
│   ├── play_turn          # 轮到出牌
│   ├── result             # 游戏结果
│   └── error              # 错误信息
│
├── lobby/
│   ├── rooms              # 房间列表
│   ├── create_room        # 创建房间
│   └── refresh            # 刷新列表
│
└── user/{userId}/
    └── private            # 私有消息
```

### 4.2 消息格式

```kotlin
// 基础消息
data class MqttMessage(
    val type: String,
    val roomId: String,
    val playerId: String,
    val timestamp: Long,
    val data: Any? = null
)

// 加入房间
data class JoinMessage(
    val playerName: String,
    val isAI: Boolean = false
)

// 叫地主
data class BidMessage(
    val bid: Int  // 0=不叫, 1/2/3 = 分数
)

// 出牌
data class PlayMessage(
    val cards: List<String>,
    val pattern: String? = null
)

// 状态同步
data class StateMessage(
    val room: Room,
    val currentPlayerId: String,
    val lastPattern: HandPattern?,
    val lastCards: List<Card>?
)

// 游戏结果
data class ResultMessage(
    val winner: PlayerRole,
    val landlordId: String,
    val farmerIds: List<String>,
    val scores: Map<String, Int>,
    val multiplier: Int
)
```

### 4.3 消息流程

```
【叫地主流程】
Broker -> Player1/bid_turn (轮到你叫地主)
Player1 -> Room/bid (我叫3分)
Broker -> 所有Client/state (Player1叫3分)
... 重复直到有人叫3分或轮完
Broker -> 所有Client/game_start (游戏开始)

【出牌流程】
Broker -> Landlord/play_turn (轮到地主出牌)
Landlord -> Room/play (出牌)
Broker -> 所有Client/state (地主出了什么牌)
Broker -> Farmer1/play_turn (轮到农民1)
... 循环直到有人出完
Broker -> 所有Client/result (游戏结束)
```

---

## 五、开发计划

### Phase 1: 核心规则层（第1-2周）
- [ ] 1.1 项目结构搭建
- [ ] 1.2 Card/Player/Room 数据模型
- [ ] 1.3 牌组管理（54张牌）
- [ ] 1.4 发牌逻辑（17+17+20）
- [ ] 1.5 牌型识别（13种牌型）
- [ ] 1.6 牌型比较规则
- [ ] 1.7 单元测试

### Phase 2: 游戏流程（第3周）
- [ ] 2.1 叫地主流程
- [ ] 2.2 底牌分配
- [ ] 2.3 地主/农民身份确定
- [ ] 2.4 出牌流程控制
- [ ] 2.5 胜负判定
- [ ] 2.6 倍数计算（炸弹/春天）

### Phase 3: AI 策略（第4-5周）
- [ ] 3.1 AI接口设计
- [ ] 3.2 基础AI（出牌选择）
- [ ] 3.3 强AI（评估函数）
- [ ] 3.4 地主AI策略
- [ ] 3.5 农民配合AI
- [ ] 3.6 AI测试

### Phase 4: MQTT 通信层（第6周）
- [ ] 4.1 MQTT协议定义
- [ ] 4.2 客户端MQTT封装
- [ ] 4.3 消息处理器
- [ ] 4.4 连接管理（断线重连）
- [ ] 4.5 房间管理逻辑

### Phase 5: Android UI（第7-9周）
**UI设计原则：完全仿照欢乐斗地主经典模式**

- [ ] 5.1 主题和样式（仿照欢乐斗地主配色）
- [ ] 5.2 首页界面（仿照欢乐斗地主大厅）
- [ ] 5.3 房间列表（仿照欢乐斗地主房间列表）
- [ ] 5.4 创建房间（仿照欢乐斗地主创建房间）
- [ ] 5.5 牌桌组件（仿照欢乐斗地主牌桌布局）
- [ ] 5.6 手牌组件（仿照欢乐斗地主手牌显示和交互）
- [ ] 5.7 叫地主面板（仿照欢乐斗地主叫地主界面）
- [ ] 5.8 操作面板（仿照欢乐斗地主出牌/不出按钮）
- [ ] 5.9 游戏主界面（仿照欢乐斗地主游戏界面）
- [ ] 5.10 结算界面（仿照欢乐斗地主结算界面）

### Phase 6: 集成测试（第10周）
- [ ] 6.1 单机人机对战测试
- [ ] 6.2 局域网多人测试
- [ ] 6.3 混合模式测试
- [ ] 6.4 Bug修复

### Phase 7: 优化完善（第11-12周）
- [ ] 7.1 音效集成
- [ ] 7.2 动画效果
- [ ] 7.3 断线重连优化
- [ ] 7.4 UI美化
- [ ] 7.5 打包发布

---

## 六、里程碑

| 里程碑 | 时间 | 交付物 |
|--------|------|--------|
| M1 | 第2周末 | 核心规则层完成，单元测试通过 |
| M2 | 第3周末 | 单机游戏可玩（命令行） |
| M3 | 第5周末 | AI对战完成 |
| M4 | 第6周末 | MQTT通信完成 |
| M5 | 第9周末 | UI界面完成 |
| M6 | 第10周末 | 多人游戏可玩 |
| M7 | 第12周末 | 完整版本发布 |

---

## 七、技术依赖

```kotlin
// core/build.gradle.kts
dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.13.2")
}

// app/build.gradle.kts
dependencies {
    implementation(project(":core"))
    
    // Compose
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material3:material3:1.1.1")
    implementation("androidx.compose.foundation:foundation:1.5.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    
    // MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
    
    // 依赖注入
    implementation("io.insert-koin:koin-android:3.4.3")
    
    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
```

---

## 八、附录

### 8.1 参考资料
- [欢乐斗地主规则](https://baike.baidu.com/item/欢乐斗地主)
- [MQTT协议规范](https://mqtt.org/)
- [Jetpack Compose文档](https://developer.android.com/jetpack/compose)
- [pdk-android 项目](https://github.com/sdcb/pdk-android)（AI策略参考）

### 8.2 UI参考
欢乐斗地主经典模式UI特点：
- **牌桌**：绿色背景，椭圆形牌桌
- **玩家位置**：下方（自己）、左侧、右侧
- **手牌**：扇形排列，可点击选择
- **牌面**：白底黑字，左上角显示点数和花色
- **操作区**：底部中央，显示出牌/不出/提示按钮
- **叫地主区**：底部中央，显示叫分按钮（1分/2分/3分/不叫）
- **信息区**：顶部显示倍数、底牌等信息

### 8.3 变更记录
| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-08-27 | v1.0 | 初始版本 |
