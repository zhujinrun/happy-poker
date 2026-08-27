package com.happy.poker.core.network

import com.happy.poker.core.model.*
import com.happy.poker.core.flow.GameFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 消息处理器接口
 */
interface MessageHandler {
    suspend fun handleMessage(message: Message)
}

/**
 * 房间消息处理器
 */
class RoomMessageHandler(
    private val mqttClient: GameMqttClient,
    private val playerId: String
) : MessageHandler {

    private val _roomUpdates = MutableSharedFlow<RoomStateMessage>()
    val roomUpdates: SharedFlow<RoomStateMessage> = _roomUpdates.asSharedFlow()

    private val _errors = MutableSharedFlow<ErrorMessage>()
    val errors: SharedFlow<ErrorMessage> = _errors.asSharedFlow()

    override suspend fun handleMessage(message: Message) {
        when (message) {
            is RoomStateMessage -> _roomUpdates.emit(message)
            is RoomJoinedMessage -> handleRoomJoined(message)
            is RoomCreatedMessage -> handleRoomCreated(message)
            is ErrorMessage -> _errors.emit(message)
            else -> {}
        }
    }

    private suspend fun handleRoomJoined(message: RoomJoinedMessage) {
        // 加入房间成功，订阅房间topic
        mqttClient.subscribe(Protocol.roomTopic(message.roomId))
    }

    private suspend fun handleRoomCreated(message: RoomCreatedMessage) {
        // 创建房间成功，订阅房间topic
        mqttClient.subscribe(Protocol.roomTopic(message.roomId))
    }

    /**
     * 创建房间
     */
    suspend fun createRoom(roomName: String, maxPlayers: Int = 3) {
        val message = RoomCreateMessage(
            playerName = "Player", // TODO: 从用户设置获取
            playerId = playerId,
            maxPlayers = maxPlayers
        )
        mqttClient.sendMessage(Protocol.TOPIC_ROOM_CREATE, message)
    }

    /**
     * 加入房间
     */
    suspend fun joinRoom(roomId: String) {
        val message = RoomJoinMessage(
            roomId = roomId,
            playerName = "Player", // TODO: 从用户设置获取
            playerId = playerId
        )
        mqttClient.sendMessage(Protocol.TOPIC_ROOM_JOIN, message)
    }

    /**
     * 离开房间
     */
    suspend fun leaveRoom(roomId: String) {
        val message = RoomLeaveMessage(
            roomId = roomId,
            playerId = playerId
        )
        mqttClient.sendMessage(Protocol.TOPIC_ROOM_LEAVE, message)
        mqttClient.unsubscribe(Protocol.roomTopic(roomId))
    }

    /**
     * 请求房间列表
     */
    suspend fun requestRoomList() {
        val message = RoomListMessage()
        mqttClient.sendMessage(Protocol.TOPIC_ROOM_LIST, message)
    }
}

/**
 * 游戏消息处理器
 */
class GameMessageHandler(
    private val mqttClient: GameMqttClient,
    private val playerId: String
) : MessageHandler {

    private val _gameStateUpdates = MutableSharedFlow<GameStateMessage>()
    val gameStateUpdates: SharedFlow<GameStateMessage> = _gameStateUpdates.asSharedFlow()

    private val _gameEvents = MutableSharedFlow<GameEvent>()
    val gameEvents: SharedFlow<GameEvent> = _gameEvents.asSharedFlow()

    private val _errors = MutableSharedFlow<ErrorMessage>()
    val errors: SharedFlow<ErrorMessage> = _errors.asSharedFlow()

    override suspend fun handleMessage(message: Message) {
        when (message) {
            is GameStartMessage -> handleGameStart(message)
            is GameBidResultMessage -> handleBidResult(message)
            is GamePlayResultMessage -> handlePlayResult(message)
            is GameStateMessage -> _gameStateUpdates.emit(message)
            is GameEndMessage -> handleGameEnd(message)
            is ErrorMessage -> _errors.emit(message)
            else -> {}
        }
    }

    private suspend fun handleGameStart(message: GameStartMessage) {
        _gameEvents.emit(GameEvent.GameStarted(
            roomId = message.roomId,
            players = message.players.map { it.toPlayerInfo() },
            bottomCards = message.bottomCards.map { it.toCard() }
        ))
    }

    private suspend fun handleBidResult(message: GameBidResultMessage) {
        _gameEvents.emit(GameEvent.BidMade(
            roomId = message.roomId,
            playerId = message.playerId,
            playerName = message.playerName,
            bid = message.bid,
            isPass = message.isPass,
            nextBidderId = message.nextBidderId,
            currentBid = message.currentBid
        ))
    }

    private suspend fun handlePlayResult(message: GamePlayResultMessage) {
        _gameEvents.emit(GameEvent.CardPlayed(
            roomId = message.roomId,
            playerId = message.playerId,
            playerName = message.playerName,
            cards = message.cards.map { it.toCard() },
            pattern = message.pattern.toHandPattern(),
            isPass = message.isPass,
            nextPlayerId = message.nextPlayerId,
            handSize = message.handSize
        ))
    }

    private suspend fun handleGameEnd(message: GameEndMessage) {
        _gameEvents.emit(GameEvent.GameEnded(
            roomId = message.roomId,
            winnerId = message.winnerId,
            winnerRole = message.winnerRole,
            scores = message.scores,
            multiplier = message.multiplier
        ))
    }

    /**
     * 叫地主
     */
    suspend fun sendBid(roomId: String, bid: Int) {
        val message = GameBidMessage(
            roomId = roomId,
            playerId = playerId,
            bid = bid
        )
        mqttClient.sendMessage(Protocol.gameTopic(roomId), message)
    }

    /**
     * 出牌
     */
    suspend fun sendPlay(roomId: String, cards: List<Card>) {
        val message = GamePlayMessage(
            roomId = roomId,
            playerId = playerId,
            cards = cards.map { CardInfo.fromCard(it) }
        )
        mqttClient.sendMessage(Protocol.gameTopic(roomId), message)
    }

    /**
     * 过牌
     */
    suspend fun sendPass(roomId: String) {
        val message = GamePassMessage(
            roomId = roomId,
            playerId = playerId
        )
        mqttClient.sendMessage(Protocol.gameTopic(roomId), message)
    }
}

/**
 * 游戏事件
 */
sealed class GameEvent {
    data class GameStarted(
        val roomId: String,
        val players: List<PlayerInfo>,
        val bottomCards: List<Card>
    ) : GameEvent()

    data class BidMade(
        val roomId: String,
        val playerId: String,
        val playerName: String,
        val bid: Int,
        val isPass: Boolean,
        val nextBidderId: String?,
        val currentBid: Int
    ) : GameEvent()

    data class CardPlayed(
        val roomId: String,
        val playerId: String,
        val playerName: String,
        val cards: List<Card>,
        val pattern: HandPattern,
        val isPass: Boolean,
        val nextPlayerId: String?,
        val handSize: Int
    ) : GameEvent()

    data class GameEnded(
        val roomId: String,
        val winnerId: String,
        val winnerRole: PlayerRole,
        val scores: Map<String, Int>,
        val multiplier: Int
    ) : GameEvent()

    data class PlayerJoined(
        val roomId: String,
        val player: PlayerInfo
    ) : GameEvent()

    data class PlayerLeft(
        val roomId: String,
        val playerId: String
    ) : GameEvent()

    data class PlayerReadyChanged(
        val roomId: String,
        val playerId: String,
        val isReady: Boolean
    ) : GameEvent()
}

/**
 * PlayerInfo扩展函数
 */
fun PlayerInfo.toPlayerInfo(): com.happy.poker.core.network.PlayerInfo = this
