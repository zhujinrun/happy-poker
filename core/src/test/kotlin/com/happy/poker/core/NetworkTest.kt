package com.happy.poker.core

import com.happy.poker.core.network.*
import com.happy.poker.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue

class NetworkTest {
    private lateinit var mqttClient: PahoMqttClient
    private lateinit var gameMqttClient: GameMqttClient
    private var brokerAvailable = false

    @BeforeEach
    fun setup() {
        mqttClient = PahoMqttClient()
        gameMqttClient = GameMqttClient(mqttClient)

        // 检查broker是否可用
        brokerAvailable = try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress("localhost", 1883), 1000)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }

        // 尝试MQTT连接验证认证是否正确
        if (brokerAvailable) {
            try {
                runBlocking {
                    mqttClient.connect(MqttConnectionConfig())
                    mqttClient.disconnect()
                }
                brokerAvailable = true
            } catch (e: Exception) {
                println("MQTT broker authentication failed: ${e.message}")
                brokerAvailable = false
            }
        }
    }

    @Test
    fun testProtocolTopics() {
        val roomId = "test-room-123"
        val playerId = "player-456"

        assertEquals("happy-poker/room/test-room-123", Protocol.roomTopic(roomId))
        assertEquals("happy-poker/game/test-room-123", Protocol.gameTopic(roomId))
        assertEquals("happy-poker/player/test-room-123/player-456", Protocol.playerTopic(roomId, playerId))
    }

    @Test
    fun testMessageSerialization() {
        val message = RoomCreateMessage(
            playerName = "测试玩家",
            playerId = "player-123",
            maxPlayers = 3
        )

        val serialized = MessageSerializer.serialize(message)
        assertNotNull(serialized)
        assertTrue(serialized.contains("ROOM_CREATE"))
        assertTrue(serialized.contains("测试玩家"))

        val deserialized = MessageSerializer.deserialize(serialized)
        assertNotNull(deserialized)
        assertTrue(deserialized is RoomCreateMessage)
    }

    @Test
    fun testCardInfoConversion() {
        val card = Card(Rank.Ace, Suit.Spades)
        val cardInfo = CardInfo.fromCard(card)

        assertEquals(Rank.Ace, cardInfo.rank)
        assertEquals(Suit.Spades, cardInfo.suit)

        val convertedCard = cardInfo.toCard()
        assertEquals(card, convertedCard)
    }

    @Test
    fun testPatternInfoConversion() {
        val pattern = HandPattern.bomb(Rank.Seven)
        val patternInfo = PatternInfo.fromHandPattern(pattern)

        assertEquals(PatternType.Bomb, patternInfo.type)
        assertEquals(Rank.Seven, patternInfo.mainRank)
        assertEquals(4, patternInfo.cardCount)

        val convertedPattern = patternInfo.toHandPattern()
        assertEquals(PatternType.Bomb, convertedPattern.type)
        assertEquals(Rank.Seven, convertedPattern.mainRank)
    }

    @Test
    fun testMqttConfig() {
        val config = MqttConnectionConfig()
        assertEquals("tcp://localhost:1883", config.brokerUrl)
        assertEquals("mqtt", config.username)
        assertEquals("123456", config.password)
        assertTrue(config.clientId.startsWith("happy-poker-"))
    }

    @Test
    fun testPahoMqttClientConnection() = runBlocking {
        assumeTrue(brokerAvailable, "MQTT broker not available on localhost:1883")

        assertEquals(ConnectionState.DISCONNECTED, mqttClient.connectionState.value)

        mqttClient.connect(MqttConnectionConfig())

        assertEquals(ConnectionState.CONNECTED, mqttClient.connectionState.value)
        assertTrue(mqttClient.isConnected())
        assertNotNull(mqttClient.getCurrentConfig())

        mqttClient.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, mqttClient.connectionState.value)
        assertFalse(mqttClient.isConnected())
    }

    @Test
    fun testPahoMqttClientPublish() = runBlocking {
        assumeTrue(brokerAvailable, "MQTT broker not available on localhost:1883")

        mqttClient.connect(MqttConnectionConfig())

        // 发布消息不应该抛出异常
        mqttClient.publish("test/topic", "test payload")

        mqttClient.disconnect()
    }

    @Test
    fun testPahoMqttClientSubscribe() {
        mqttClient.subscribe("test/topic")
        assertTrue(mqttClient.getSubscriptions().contains("test/topic"))

        mqttClient.unsubscribe("test/topic")
        assertFalse(mqttClient.getSubscriptions().contains("test/topic"))
    }

    @Test
    fun testPahoMqttClientMessageReception() = runBlocking {
        val receivedMessages = mutableListOf<Pair<String, String>>()

        val job = launch {
            mqttClient.messages.collect { message ->
                receivedMessages.add(message)
            }
        }

        delay(100) // 等待订阅完成

        mqttClient.simulateMessage("test/topic", "test payload")
        delay(100) // 等待消息接收

        assertEquals(1, receivedMessages.size)
        assertEquals("test/topic", receivedMessages[0].first)
        assertEquals("test payload", receivedMessages[0].second)

        job.cancel()
    }

    @Test
    fun testGameMqttClientConnection() = runBlocking {
        assumeTrue(brokerAvailable, "MQTT broker not available on localhost:1883")

        gameMqttClient.connect(MqttConnectionConfig())

        assertEquals(ConnectionState.CONNECTED, gameMqttClient.getConnectionState())
        assertTrue(gameMqttClient.isConnected())

        gameMqttClient.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, gameMqttClient.getConnectionState())
    }

    @Test
    fun testGameMqttClientSendMessage() = runBlocking {
        assumeTrue(brokerAvailable, "MQTT broker not available on localhost:1883")

        gameMqttClient.connect(MqttConnectionConfig())

        val message = GameBidMessage(
            roomId = "test-room",
            playerId = "player-123",
            bid = 2
        )

        // 发送消息不应该抛出异常
        gameMqttClient.sendMessage(Protocol.gameTopic("test-room"), message)

        gameMqttClient.disconnect()
    }

    @Test
    fun testRoomMessageHandler() = runBlocking {
        assumeTrue(brokerAvailable, "MQTT broker not available on localhost:1883")

        gameMqttClient.connect(MqttConnectionConfig())

        val handler = RoomMessageHandler(gameMqttClient, "player-123")
        val receivedUpdates = mutableListOf<RoomStateMessage>()

        val job = launch {
            handler.roomUpdates.collect { update ->
                receivedUpdates.add(update)
            }
        }

        delay(100)

        // 模拟接收房间状态消息
        val roomState = RoomStateMessage(
            roomId = "test-room",
            roomName = "测试房间",
            players = listOf(PlayerInfo("player-123", "测试玩家")),
            state = RoomState.Waiting,
            hostId = "player-123"
        )

        handler.handleMessage(roomState)
        delay(100)

        assertEquals(1, receivedUpdates.size)
        assertEquals("test-room", receivedUpdates[0].roomId)

        job.cancel()
        gameMqttClient.disconnect()
    }

    @Test
    fun testGameMessageHandler() = runBlocking {
        assumeTrue(brokerAvailable, "MQTT broker not available on localhost:1883")

        gameMqttClient.connect(MqttConnectionConfig())

        val handler = GameMessageHandler(gameMqttClient, "player-123")
        val receivedEvents = mutableListOf<GameEvent>()

        val job = launch {
            handler.gameEvents.collect { event ->
                receivedEvents.add(event)
            }
        }

        delay(100)

        // 模拟接收游戏开始消息
        val gameStart = GameStartMessage(
            roomId = "test-room",
            players = listOf(PlayerInfo("player-123", "测试玩家")),
            bottomCards = listOf(CardInfo(Rank.Ace, Suit.Spades))
        )

        handler.handleMessage(gameStart)
        delay(100)

        assertEquals(1, receivedEvents.size)
        assertTrue(receivedEvents[0] is GameEvent.GameStarted)

        job.cancel()
        gameMqttClient.disconnect()
    }

    @Test
    fun testGameEventTypes() {
        val gameStarted = GameEvent.GameStarted(
            roomId = "room-1",
            players = emptyList(),
            bottomCards = emptyList()
        )

        val bidMade = GameEvent.BidMade(
            roomId = "room-1",
            playerId = "player-1",
            playerName = "玩家1",
            bid = 2,
            isPass = false,
            nextBidderId = "player-2",
            currentBid = 2
        )

        val cardPlayed = GameEvent.CardPlayed(
            roomId = "room-1",
            playerId = "player-1",
            playerName = "玩家1",
            cards = listOf(Card(Rank.Three, Suit.Spades)),
            pattern = HandPattern.single(Rank.Three),
            isPass = false,
            nextPlayerId = "player-2",
            handSize = 16
        )

        val gameEnded = GameEvent.GameEnded(
            roomId = "room-1",
            winnerId = "player-1",
            winnerRole = PlayerRole.Landlord,
            scores = mapOf("player-1" to 10, "player-2" to -5, "player-3" to -5),
            multiplier = 2
        )

        assertNotNull(gameStarted)
        assertNotNull(bidMade)
        assertNotNull(cardPlayed)
        assertNotNull(gameEnded)
    }
}
