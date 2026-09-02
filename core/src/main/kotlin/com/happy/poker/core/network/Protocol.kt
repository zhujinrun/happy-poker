package com.happy.poker.core.network

import com.happy.poker.core.model.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * MQTT通信协议
 */
object Protocol {
    const val TOPIC_PREFIX = "happy-poker"
    const val VERSION = "1.0"

    // Topic patterns
    const val TOPIC_ROOM_CREATE = "$TOPIC_PREFIX/room/create"
    const val TOPIC_ROOM_JOIN = "$TOPIC_PREFIX/room/join"
    const val TOPIC_ROOM_LEAVE = "$TOPIC_PREFIX/room/leave"
    const val TOPIC_ROOM_LIST = "$TOPIC_PREFIX/room/list"
    const val TOPIC_ROOM_UPDATE = "$TOPIC_PREFIX/room/update"

    const val TOPIC_GAME_START = "$TOPIC_PREFIX/game/start"
    const val TOPIC_GAME_BID = "$TOPIC_PREFIX/game/bid"
    const val TOPIC_GAME_PLAY = "$TOPIC_PREFIX/game/play"
    const val TOPIC_GAME_PASS = "$TOPIC_PREFIX/game/pass"
    const val TOPIC_GAME_STATE = "$TOPIC_PREFIX/game/state"
    const val TOPIC_GAME_END = "$TOPIC_PREFIX/game/end"

    const val TOPIC_PLAYER_READY = "$TOPIC_PREFIX/player/ready"
    const val TOPIC_PLAYER_STATUS = "$TOPIC_PREFIX/player/status"

    /**
     * 获取房间专属topic
     */
    fun roomTopic(roomId: String): String = "$TOPIC_PREFIX/room/$roomId"

    /**
     * 获取游戏专属topic
     */
    fun gameTopic(roomId: String): String = "$TOPIC_PREFIX/game/$roomId"

    /**
     * 获取玩家专属topic
     */
    fun playerTopic(roomId: String, playerId: String): String = "$TOPIC_PREFIX/player/$roomId/$playerId"
}

/**
 * 消息类型
 */
@Serializable
enum class MessageType {
    // 房间相关
    ROOM_CREATE,
    ROOM_CREATED,
    ROOM_JOIN,
    ROOM_JOINED,
    ROOM_LEAVE,
    ROOM_LEFT,
    ROOM_LIST,
    ROOM_LIST_RESPONSE,
    ROOM_UPDATE,
    ROOM_STATE,

    // 游戏相关
    GAME_START,
    GAME_STARTED,
    GAME_BID,
    GAME_BID_RESULT,
    GAME_PLAY,
    GAME_PLAY_RESULT,
    GAME_PASS,
    GAME_PASS_RESULT,
    GAME_STATE,
    GAME_STATE_UPDATE,
    GAME_END,
    GAME_ENDED,

    // 玩家相关
    PLAYER_READY,
    PLAYER_STATUS,
    PLAYER_TURN,

    // 系统
    ERROR,
    PING,
    PONG
}

/**
 * 基础消息
 */
interface Message {
    val type: MessageType
    val timestamp: Long
}

/**
 * 房间创建请求
 */
@Serializable
data class RoomCreateMessage(
    val playerName: String,
    val playerId: String,
    val maxPlayers: Int = 3,
    val isPrivate: Boolean = false,
    val avatarKey: String? = null
) : Message {
    override val type: MessageType = MessageType.ROOM_CREATE
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 房间创建响应
 */
@Serializable
data class RoomCreatedMessage(
    val roomId: String,
    val roomName: String
) : Message {
    override val type: MessageType = MessageType.ROOM_CREATED
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 加入房间请求
 */
@Serializable
data class RoomJoinMessage(
    val roomId: String,
    val playerName: String,
    val playerId: String,
    val avatarKey: String? = null
) : Message {
    override val type: MessageType = MessageType.ROOM_JOIN
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 加入房间响应
 */
@Serializable
data class RoomJoinedMessage(
    val roomId: String,
    val players: List<PlayerInfo>
) : Message {
    override val type: MessageType = MessageType.ROOM_JOINED
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 离开房间
 */
@Serializable
data class RoomLeaveMessage(
    val roomId: String,
    val playerId: String
) : Message {
    override val type: MessageType = MessageType.ROOM_LEAVE
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 房间状态更新
 */
@Serializable
data class RoomStateMessage(
    val roomId: String,
    val roomName: String,
    val players: List<PlayerInfo>,
    val state: RoomState,
    val hostId: String,
    val maxPlayers: Int = 3
) : Message {
    override val type: MessageType = MessageType.ROOM_STATE
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 房间列表请求
 */
@Serializable
data class RoomListMessage(
    val includePrivate: Boolean = false
) : Message {
    override val type: MessageType = MessageType.ROOM_LIST
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 房间列表响应
 */
@Serializable
data class RoomListResponseMessage(
    val rooms: List<RoomInfo>
) : Message {
    override val type: MessageType = MessageType.ROOM_LIST_RESPONSE
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 游戏开始
 */
@Serializable
data class GameStartMessage(
    val roomId: String,
    val players: List<PlayerInfo>,
    val bottomCards: List<CardInfo>
) : Message {
    override val type: MessageType = MessageType.GAME_START
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 叫地主消息
 */
@Serializable
data class GameBidMessage(
    val roomId: String,
    val playerId: String,
    val bid: Int
) : Message {
    override val type: MessageType = MessageType.GAME_BID
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 叫地主结果
 */
@Serializable
data class GameBidResultMessage(
    val roomId: String,
    val playerId: String,
    val playerName: String,
    val bid: Int,
    val isPass: Boolean,
    val nextBidderId: String?,
    val currentBid: Int
) : Message {
    override val type: MessageType = MessageType.GAME_BID_RESULT
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 出牌消息
 */
@Serializable
data class GamePlayMessage(
    val roomId: String,
    val playerId: String,
    val cards: List<CardInfo>
) : Message {
    override val type: MessageType = MessageType.GAME_PLAY
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 出牌结果
 */
@Serializable
data class GamePlayResultMessage(
    val roomId: String,
    val playerId: String,
    val playerName: String,
    val cards: List<CardInfo>,
    val pattern: PatternInfo,
    val isPass: Boolean,
    val nextPlayerId: String?,
    val handSize: Int
) : Message {
    override val type: MessageType = MessageType.GAME_PLAY_RESULT
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 过牌消息
 */
@Serializable
data class GamePassMessage(
    val roomId: String,
    val playerId: String
) : Message {
    override val type: MessageType = MessageType.GAME_PASS
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 游戏状态更新
 */
@Serializable
data class GameStateMessage(
    val roomId: String,
    val state: RoomState,
    val currentPlayerId: String?,
    val landlordId: String?,
    val multiplier: Int,
    val lastPlayedCards: List<CardInfo>?,
    val lastPlayedPlayerId: String?,
    val players: List<PlayerStateInfo>,
    val currentBid: Int = 0,
    val bottomCards: List<CardInfo> = emptyList()
) : Message {
    override val type: MessageType = MessageType.GAME_STATE_UPDATE
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 游戏结束
 */
@Serializable
data class GameEndMessage(
    val roomId: String,
    val winnerId: String,
    val winnerRole: PlayerRole,
    val scores: Map<String, Int>,
    val multiplier: Int
) : Message {
    override val type: MessageType = MessageType.GAME_END
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 玩家准备
 */
@Serializable
data class PlayerReadyMessage(
    val roomId: String,
    val playerId: String,
    val isReady: Boolean
) : Message {
    override val type: MessageType = MessageType.PLAYER_READY
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 玩家状态
 */
@Serializable
data class PlayerStatusMessage(
    val roomId: String,
    val playerId: String,
    val isOnline: Boolean,
    val isReady: Boolean
) : Message {
    override val type: MessageType = MessageType.PLAYER_STATUS
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 错误消息
 */
@Serializable
data class ErrorMessage(
    val code: Int,
    val message: String,
    val details: String? = null
) : Message {
    override val type: MessageType = MessageType.ERROR
    override val timestamp: Long = System.currentTimeMillis()
}

/**
 * 玩家信息
 */
@Serializable
data class PlayerInfo(
    val id: String,
    val name: String,
    val isAI: Boolean = false,
    val isOnline: Boolean = true,
    val isReady: Boolean = false,
    val avatarKey: String? = null
)

/**
 * 房间信息
 */
@Serializable
data class RoomInfo(
    val id: String,
    val name: String,
    val hostId: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val state: RoomState,
    val isPrivate: Boolean = false
)

/**
 * 牌信息
 */
@Serializable
data class CardInfo(
    val rank: Rank,
    val suit: Suit
) {
    fun toCard(): Card = Card(rank, suit)

    companion object {
        fun fromCard(card: Card): CardInfo = CardInfo(card.rank, card.suit)
    }
}

/**
 * 牌型信息
 */
@Serializable
data class PatternInfo(
    val type: PatternType,
    val mainRank: Rank,
    val cardCount: Int,
    val groupCount: Int = 0,
    val description: String = ""
) {
    fun toHandPattern(): HandPattern = when (type) {
        PatternType.Single -> HandPattern.single(mainRank)
        PatternType.Pair -> HandPattern.pair(mainRank)
        PatternType.Triple -> HandPattern.triple(mainRank)
        PatternType.Bomb -> HandPattern.bomb(mainRank)
        PatternType.Rocket -> HandPattern.rocket()
        PatternType.Straight -> HandPattern.straight(mainRank, cardCount)
        PatternType.ConsecutivePairs -> HandPattern.consecutivePairs(mainRank, groupCount)
        PatternType.Plane -> HandPattern.plane(mainRank, groupCount)
        PatternType.TripleWithOne -> HandPattern.tripleWithOne(mainRank)
        PatternType.TripleWithPair -> HandPattern.tripleWithPair(mainRank)
        PatternType.FourWithTwo -> HandPattern.fourWithTwo(mainRank)
        PatternType.FourWithPairs -> HandPattern.fourWithPairs(mainRank)
        PatternType.PlaneWithWings -> HandPattern.planeWithWings(mainRank, groupCount, cardCount - groupCount * 3)
        PatternType.Invalid -> HandPattern.Invalid
    }

    companion object {
        fun fromHandPattern(pattern: HandPattern): PatternInfo = PatternInfo(
            type = pattern.type,
            mainRank = pattern.mainRank,
            cardCount = pattern.cardCount,
            groupCount = pattern.groupCount,
            description = pattern.description
        )
    }
}

/**
 * 玩家状态信息
 */
@Serializable
data class PlayerStateInfo(
    val id: String,
    val name: String,
    val role: PlayerRole,
    val handSize: Int,
    val isOnline: Boolean,
    val isReady: Boolean,
    val handCards: List<CardInfo> = emptyList(),
    val avatarKey: String? = null
)

/**
 * 消息序列化工具
 */
object MessageSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun serialize(message: Message): String {
        val element = when (message) {
            is RoomCreateMessage -> json.encodeToJsonElement(message)
            is RoomCreatedMessage -> json.encodeToJsonElement(message)
            is RoomJoinMessage -> json.encodeToJsonElement(message)
            is RoomJoinedMessage -> json.encodeToJsonElement(message)
            is RoomLeaveMessage -> json.encodeToJsonElement(message)
            is RoomStateMessage -> json.encodeToJsonElement(message)
            is RoomListMessage -> json.encodeToJsonElement(message)
            is RoomListResponseMessage -> json.encodeToJsonElement(message)
            is GameStartMessage -> json.encodeToJsonElement(message)
            is GameBidMessage -> json.encodeToJsonElement(message)
            is GameBidResultMessage -> json.encodeToJsonElement(message)
            is GamePlayMessage -> json.encodeToJsonElement(message)
            is GamePlayResultMessage -> json.encodeToJsonElement(message)
            is GamePassMessage -> json.encodeToJsonElement(message)
            is GameStateMessage -> json.encodeToJsonElement(message)
            is GameEndMessage -> json.encodeToJsonElement(message)
            is PlayerReadyMessage -> json.encodeToJsonElement(message)
            is PlayerStatusMessage -> json.encodeToJsonElement(message)
            is ErrorMessage -> json.encodeToJsonElement(message)
            else -> return ""
        }
        return element.toString()
    }

    fun deserialize(jsonString: String): Message? {
        return try {
            val element = json.parseToJsonElement(jsonString)
            val type = element.jsonObject["type"]?.jsonPrimitive?.content

            when (MessageType.valueOf(type ?: "")) {
                MessageType.ROOM_CREATE -> json.decodeFromString<RoomCreateMessage>(jsonString)
                MessageType.ROOM_CREATED -> json.decodeFromString<RoomCreatedMessage>(jsonString)
                MessageType.ROOM_JOIN -> json.decodeFromString<RoomJoinMessage>(jsonString)
                MessageType.ROOM_JOINED -> json.decodeFromString<RoomJoinedMessage>(jsonString)
                MessageType.ROOM_LEAVE -> json.decodeFromString<RoomLeaveMessage>(jsonString)
                MessageType.ROOM_STATE -> json.decodeFromString<RoomStateMessage>(jsonString)
                MessageType.ROOM_LIST -> json.decodeFromString<RoomListMessage>(jsonString)
                MessageType.ROOM_LIST_RESPONSE -> json.decodeFromString<RoomListResponseMessage>(jsonString)
                MessageType.GAME_START -> json.decodeFromString<GameStartMessage>(jsonString)
                MessageType.GAME_BID -> json.decodeFromString<GameBidMessage>(jsonString)
                MessageType.GAME_BID_RESULT -> json.decodeFromString<GameBidResultMessage>(jsonString)
                MessageType.GAME_PLAY -> json.decodeFromString<GamePlayMessage>(jsonString)
                MessageType.GAME_PLAY_RESULT -> json.decodeFromString<GamePlayResultMessage>(jsonString)
                MessageType.GAME_PASS -> json.decodeFromString<GamePassMessage>(jsonString)
                MessageType.GAME_STATE_UPDATE -> json.decodeFromString<GameStateMessage>(jsonString)
                MessageType.GAME_END -> json.decodeFromString<GameEndMessage>(jsonString)
                MessageType.PLAYER_READY -> json.decodeFromString<PlayerReadyMessage>(jsonString)
                MessageType.PLAYER_STATUS -> json.decodeFromString<PlayerStatusMessage>(jsonString)
                MessageType.ERROR -> json.decodeFromString<ErrorMessage>(jsonString)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
