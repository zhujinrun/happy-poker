package com.happy.poker.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.happy.poker.app.network.ConnectionState
import com.happy.poker.app.network.MqttConfigManager
import com.happy.poker.app.network.ReconnectManager
import com.happy.poker.app.progress.PlayerProgressManager
import com.happy.poker.app.settings.AppSettingsManager
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.core.ai.AiManager
import com.happy.poker.core.ai.AiEvaluator
import com.happy.poker.core.flow.GameCallback
import com.happy.poker.core.flow.GameFlow
import com.happy.poker.core.flow.GameState
import com.happy.poker.core.model.*
import com.happy.poker.core.network.*
import com.happy.poker.core.rules.Validator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private const val TURN_TIMEOUT_SECONDS = 30
private const val GAME_END_REVEAL_DELAY_MS = 1200L
private const val ROOM_BROADCAST_INTERVAL_MS = 2500L
private const val ROOM_STALE_TIMEOUT_MS = 9000L
private const val AI_PLAYER_ID_PREFIX = "ai_player_"

private val LOBBY_MQTT_TOPICS = setOf(
    Protocol.TOPIC_ROOM_LIST,
    Protocol.TOPIC_ROOM_UPDATE
)

private val WAITING_ROOM_MQTT_TOPICS = LOBBY_MQTT_TOPICS + setOf(
    Protocol.TOPIC_ROOM_JOIN,
    Protocol.TOPIC_ROOM_LEAVE,
    Protocol.TOPIC_PLAYER_READY,
    Protocol.TOPIC_GAME_START,
    Protocol.TOPIC_GAME_STATE,
    Protocol.TOPIC_GAME_END
)

private val GAME_MQTT_TOPICS = WAITING_ROOM_MQTT_TOPICS + setOf(
    Protocol.TOPIC_GAME_BID,
    Protocol.TOPIC_GAME_PLAY,
    Protocol.TOPIC_GAME_PASS
)

private enum class MultiplayerTurnPhase {
    Bidding,
    Playing
}

data class RoomUiState(
    val roomId: String = "",
    val roomName: String = "",
    val players: List<PlayerUiState> = emptyList(),
    val maxPlayers: Int = 3,
    val state: RoomState = RoomState.Waiting,
    val isHost: Boolean = false,
    val hostId: String = ""
)

data class MultiplayerGameUiState(
    val room: RoomUiState = RoomUiState(),
    val availableRooms: List<RoomInfo> = emptyList(),
    val playerCards: List<Card> = emptyList(),
    val selectedCards: Set<String> = emptySet(),
    val bottomCards: List<Card> = emptyList(),
    val multiplier: Int = 1,
    val isPlayTurn: Boolean = false,
    val isBidTurn: Boolean = false,
    val currentBid: Int = 0,
    val currentPlayerId: String? = null,
    val lastPlayedCards: List<Card>? = null,
    val lastPlayedPattern: HandPattern? = null,
    val lastPlayedBy: String? = null,
    val turnSecondsRemaining: Int = TURN_TIMEOUT_SECONDS,
    val gameResult: GameResult? = null,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackId: Int = 0,
    val isConnected: Boolean = false,
    val isSearching: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED
)

class MultiplayerGameViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MultiplayerGameUiState())
    val uiState: StateFlow<MultiplayerGameUiState> = _uiState.asStateFlow()

    private val mqttClient = GameMqttClient()
    private val humanPlayerId: String = "human_player_${System.currentTimeMillis()}"
    val localPlayerId: String get() = humanPlayerId
    private var currentRoomId: String = ""
    private var currentRoomMaxPlayers: Int = 3
    private var currentGameSettlementKey: String = ""
    private var settledGameKey: String? = null
    private val reconnectManager = ReconnectManager()
    private val mqttConfigManager = MqttConfigManager(application)
    private val appSettingsManager = AppSettingsManager(application)
    private val playerProgressManager = PlayerProgressManager(application)
    private val aiManager = AiManager()
    private var hostRoom: Room? = null
    private var gameFlow: GameFlow? = null
    private var turnTimerJob: Job? = null
    private var turnTimerToken: Int = 0
    private var gameEndRevealJob: Job? = null
    private var aiActionJob: Job? = null
    private var messageHandlerJob: Job? = null
    private var roomBroadcastJob: Job? = null
    private val discoveredRoomLastSeen = mutableMapOf<String, Long>()
    private val activeMqttTopics = mutableSetOf<String>()
    private val connectionMutex = Mutex()

    init {
        reconnectManager.init(viewModelScope) { reconnect() }
    }

    private fun reconnect() {
        viewModelScope.launch {
            ensureBrokerConnected(
                actionName = "重新连接服务器",
                connectingState = ConnectionState.RECONNECTING,
                notifyFailure = false
            )
        }
    }

    private fun beanBalanceForPlayer(playerId: String, existing: PlayerUiState? = null): Int {
        return if (playerId == humanPlayerId) {
            playerProgressManager.getBeanBalance()
        } else {
            existing?.beanBalance ?: PlayerProgressManager.INITIAL_BEAN_BALANCE
        }
    }

    private suspend fun ensureBrokerConnected(
        actionName: String,
        connectingState: ConnectionState = ConnectionState.CONNECTING,
        notifyFailure: Boolean = true
    ): Boolean = connectionMutex.withLock {
        if (mqttClient.isConnected()) {
            reconnectManager.onConnected()
            updateUiState {
                it.copy(
                    isConnected = true,
                    isSearching = false,
                    connectionState = ConnectionState.CONNECTED
                )
            }
            syncMqttSubscriptions(desiredMqttTopics())
            setupMessageHandlers()
            return@withLock true
        }

        val config = mqttConfigManager.getMqttConnectionConfig()
        updateUiState {
            it.copy(
                isConnected = false,
                connectionState = connectingState
            )
        }

        try {
            mqttClient.connect(config)
            reconnectManager.onConnected()
            updateUiState {
                it.copy(
                    isConnected = true,
                    isSearching = false,
                    connectionState = ConnectionState.CONNECTED
                )
            }
            syncMqttSubscriptions(desiredMqttTopics(), force = true)
            setupMessageHandlers()
            true
        } catch (e: Exception) {
            reconnectManager.onError(e)
            updateUiState {
                it.copy(
                    isConnected = false,
                    isSearching = false,
                    connectionState = ConnectionState.ERROR
                )
            }
            if (notifyFailure) {
                showFeedback("${actionName}失败：无法连接 ${config.brokerUrl}，${formatConnectionError(e)}")
            }
            false
        }
    }

    private fun desiredMqttTopics(): Set<String> {
        val roomState = _uiState.value.room.state
        return when {
            currentRoomId.isBlank() -> LOBBY_MQTT_TOPICS
            roomState == RoomState.Bidding || roomState == RoomState.Playing || roomState == RoomState.Finished -> GAME_MQTT_TOPICS
            else -> WAITING_ROOM_MQTT_TOPICS
        }
    }

    private fun syncMqttSubscriptions(topics: Set<String>, force: Boolean = false) {
        (activeMqttTopics - topics).forEach(mqttClient::unsubscribe)
        val topicsToSubscribe = if (force) topics else topics - activeMqttTopics
        topicsToSubscribe.forEach(mqttClient::subscribe)
        activeMqttTopics.clear()
        activeMqttTopics.addAll(topics)
    }

    private fun setupMessageHandlers() {
        if (messageHandlerJob?.isActive == true) {
            return
        }
        messageHandlerJob?.cancel()
        messageHandlerJob = viewModelScope.launch {
            mqttClient.incomingMessages.collect { message ->
                try {
                    when (message) {
                        is RoomCreateMessage -> {}
                        is RoomJoinMessage -> handleRoomJoinRequest(message)
                        is RoomLeaveMessage -> handleRoomLeaveRequest(message)
                        is RoomListMessage -> handleRoomListRequest()
                        is RoomListResponseMessage -> handleRoomListResponse(message)
                        is RoomCreatedMessage -> handleRoomCreated(message)
                        is RoomJoinedMessage -> handleRoomJoined(message)
                        is RoomStateMessage -> handleRoomState(message)
                        is GameStartMessage -> handleGameStarted(message)
                        is GameBidMessage -> handleBidCommand(message)
                        is GameBidResultMessage -> handleBidResult(message)
                        is GamePlayMessage -> handlePlayCommand(message)
                        is GamePlayResultMessage -> handlePlayResult(message)
                        is GamePassMessage -> handlePassCommand(message)
                        is GameStateMessage -> handleGameStateUpdate(message)
                        is GameEndMessage -> handleGameEnded(message)
                        is ErrorMessage -> handleError(message)
                        else -> {}
                    }
                } catch (e: Exception) {
                    showFeedback("处理联机消息失败：${e.message ?: "数据异常"}")
                }
            }
        }
    }

    suspend fun createRoom(roomName: String, maxPlayers: Int): Boolean {
        if (maxPlayers !in Room.SUPPORTED_PLAYER_COUNTS) {
            showFeedback("当前只支持2人或3人房间")
            return false
        }
        if (!ensureBrokerConnected("创建房间")) {
            return false
        }

        val displayRoomName = roomName.ifBlank { "欢乐房间" }
        val roomId = createLocalRoomId()
        currentRoomId = roomId
        currentRoomMaxPlayers = maxPlayers
        updateUiState {
            it.copy(
                isSearching = false,
                room = RoomUiState(
                    roomId = roomId,
                    roomName = displayRoomName,
                    players = listOf(
                        PlayerUiState(
                            id = humanPlayerId,
                            name = appSettingsManager.getNickname(),
                            role = PlayerRole.Unknown,
                            handSize = 0,
                            isOnline = true,
                            beanBalance = beanBalanceForPlayer(humanPlayerId),
                            avatarKey = localAvatarKey()
                        )
                    ),
                    maxPlayers = maxPlayers,
                    state = RoomState.Waiting,
                    isHost = true,
                    hostId = humanPlayerId
                )
            )
        }
        syncMqttSubscriptions(WAITING_ROOM_MQTT_TOPICS)
        startRoomBroadcast()
        val created = broadcastCurrentRoomState("创建房间", notifyFailure = true)
        if (!created) {
            stopRoomBroadcast()
            currentRoomId = ""
            currentRoomMaxPlayers = 3
            updateUiState { it.copy(room = RoomUiState(), isSearching = false) }
            syncMqttSubscriptions(LOBBY_MQTT_TOPICS)
        }
        return created
    }

    suspend fun joinRoom(roomId: String, maxPlayers: Int = 3, roomName: String = "欢乐房间"): Boolean {
        if (maxPlayers !in Room.SUPPORTED_PLAYER_COUNTS) {
            showFeedback("当前只支持2人或3人房间")
            return false
        }
        if (!ensureBrokerConnected("加入房间")) {
            return false
        }

        currentRoomId = roomId
        currentRoomMaxPlayers = maxPlayers
        updateUiState {
            it.copy(
                isSearching = true,
                room = RoomUiState(
                    roomId = roomId,
                    roomName = roomName.ifBlank { "欢乐房间" },
                    players = listOf(
                        PlayerUiState(
                            id = humanPlayerId,
                            name = appSettingsManager.getNickname(),
                            role = PlayerRole.Unknown,
                            handSize = 0,
                            isOnline = true,
                            beanBalance = beanBalanceForPlayer(humanPlayerId),
                            avatarKey = localAvatarKey()
                        )
                    ),
                    maxPlayers = maxPlayers,
                    state = RoomState.Waiting,
                    isHost = false
                )
            )
        }
        syncMqttSubscriptions(WAITING_ROOM_MQTT_TOPICS)
        val message = RoomJoinMessage(
            roomId = roomId,
            playerName = appSettingsManager.getNickname(),
            playerId = humanPlayerId,
            avatarKey = localAvatarKey()
        )
        val joined = sendOnlineMessageNow(Protocol.TOPIC_ROOM_JOIN, message, "加入房间", ensureConnection = false)
        if (!joined) {
            currentRoomId = ""
            currentRoomMaxPlayers = 3
            updateUiState { it.copy(room = RoomUiState(), isSearching = false) }
            syncMqttSubscriptions(LOBBY_MQTT_TOPICS)
        }
        return joined
    }

    fun leaveRoom() {
        stopHumanTurnTimer()
        cancelPendingGameEndReveal()
        stopRoomBroadcast()
        clearHostGame()
        val roomId = currentRoomId
        if (roomId.isNotBlank()) {
            val room = _uiState.value.room
            if (room.isHost) {
                sendOnlineMessage(
                    Protocol.TOPIC_ROOM_UPDATE,
                    buildRoomStateMessage(room, RoomState.Finished),
                    "关闭房间",
                    notifyFailure = false
                )
            } else {
                val message = RoomLeaveMessage(
                    roomId = roomId,
                    playerId = humanPlayerId
                )
                sendOnlineMessage(Protocol.TOPIC_ROOM_LEAVE, message, "退出房间")
            }
        }
        resetCurrentRoomPreservingLobby()
    }

    private fun sendOnlineMessage(
        topic: String,
        message: Message,
        actionName: String,
        notifyFailure: Boolean = true
    ) {
        viewModelScope.launch {
            sendOnlineMessageNow(topic, message, actionName, notifyFailure = notifyFailure)
        }
    }

    private suspend fun sendOnlineMessageNow(
        topic: String,
        message: Message,
        actionName: String,
        ensureConnection: Boolean = true,
        notifyFailure: Boolean = true
    ): Boolean {
        if (ensureConnection && !ensureBrokerConnected(actionName, notifyFailure = notifyFailure)) {
            return false
        }

        return try {
            mqttClient.sendMessage(topic, message)
            true
        } catch (e: Exception) {
            handleOnlineActionFailure(actionName, e, notifyFailure)
            false
        }
    }

    private fun handleOnlineActionFailure(actionName: String, error: Throwable, notifyFailure: Boolean = true) {
        updateUiState {
            it.copy(
                isConnected = mqttClient.isConnected(),
                isSearching = false,
                connectionState = currentConnectionState()
            )
        }
        if (!notifyFailure) return

        val reason = error.message?.takeIf { it.isNotBlank() } ?: "请检查网络配置"
        showFeedback("${actionName}失败：$reason")
    }

    private fun formatConnectionError(error: Throwable): String {
        return error.message
            ?.takeIf { it.isNotBlank() }
            ?: "请确认手机和服务器在同一局域网"
    }

    private fun createLocalRoomId(): String {
        return "room-${System.currentTimeMillis().toString(36)}-${UUID.randomUUID().toString().take(4)}"
    }

    private fun isAiPlayer(playerId: String): Boolean = playerId.startsWith(AI_PLAYER_ID_PREFIX)

    private fun localAvatarKey(): String = normalizeAvatarKey(appSettingsManager.getAvatarKey()) ?: AppSettingsManager.DEFAULT_AVATAR

    private fun normalizeAvatarKey(avatarKey: String?): String? {
        return avatarKey?.takeIf { it == "daheng" || it == "luoli" || it == "yujie" }
    }

    private fun fallbackAvatarKey(playerId: String): String {
        return when {
            isAiPlayer(playerId) -> "daheng"
            (playerId.hashCode() and 1) == 0 -> "luoli"
            else -> "yujie"
        }
    }

    private fun avatarKeyForPlayer(playerId: String, avatarKey: String?, existing: PlayerUiState? = null): String {
        return normalizeAvatarKey(avatarKey)
            ?: normalizeAvatarKey(existing?.avatarKey)
            ?: fallbackAvatarKey(playerId)
    }

    private fun playerInfoFromUi(player: PlayerUiState): PlayerInfo {
        return PlayerInfo(
            id = player.id,
            name = player.name,
            isAI = isAiPlayer(player.id),
            isOnline = player.isOnline,
            isReady = false,
            avatarKey = avatarKeyForPlayer(player.id, player.avatarKey)
        )
    }

    private fun playersWithAutoAi(room: RoomUiState): List<PlayerUiState> {
        val activePlayers = room.players.distinctBy { it.id }
        if (activePlayers.size >= 3) return activePlayers

        return activePlayers + PlayerUiState(
            id = "$AI_PLAYER_ID_PREFIX${room.roomId}",
            name = "电脑玩家",
            role = PlayerRole.Unknown,
            handSize = 0,
            isOnline = true,
            beanBalance = PlayerProgressManager.INITIAL_BEAN_BALANCE,
            avatarKey = "daheng"
        )
    }

    private fun createHostGameRoom(room: RoomUiState, players: List<PlayerUiState>): Room {
        return Room(
            id = room.roomId,
            name = room.roomName.ifBlank { "欢乐房间" },
            hostId = room.hostId.ifBlank { humanPlayerId },
            maxPlayers = 3
        ).apply {
            players.take(3).forEach { uiPlayer ->
                addPlayer(
                    Player(
                        id = uiPlayer.id,
                        name = uiPlayer.name,
                        isAI = isAiPlayer(uiPlayer.id)
                    )
                )
            }
        }
    }

    private fun buildRoomStateMessage(room: RoomUiState, stateOverride: RoomState = room.state): RoomStateMessage {
        return RoomStateMessage(
            roomId = room.roomId,
            roomName = room.roomName,
            players = room.players.map(::playerInfoFromUi),
            state = stateOverride,
            hostId = room.hostId,
            maxPlayers = room.maxPlayers
        )
    }

    private suspend fun broadcastCurrentRoomState(
        actionName: String = "同步房间",
        notifyFailure: Boolean = false
    ): Boolean {
        val room = _uiState.value.room
        if (room.roomId.isBlank()) return false

        return sendOnlineMessageNow(
            topic = Protocol.TOPIC_ROOM_UPDATE,
            message = buildRoomStateMessage(room),
            actionName = actionName,
            notifyFailure = notifyFailure
        )
    }

    private fun startRoomBroadcast() {
        if (roomBroadcastJob?.isActive == true) return

        roomBroadcastJob = viewModelScope.launch {
            while (true) {
                broadcastCurrentRoomState(notifyFailure = false)
                delay(ROOM_BROADCAST_INTERVAL_MS)
            }
        }
    }

    private fun stopRoomBroadcast() {
        roomBroadcastJob?.cancel()
        roomBroadcastJob = null
    }

    private fun updateDiscoveredRoom(message: RoomStateMessage) {
        val roomInfo = RoomInfo(
            id = message.roomId,
            name = message.roomName,
            hostId = message.hostId,
            playerCount = message.players.size,
            maxPlayers = message.maxPlayers,
            state = message.state
        )

        val now = System.currentTimeMillis()
        discoveredRoomLastSeen[message.roomId] = now
        updateUiState { state ->
            val activeRooms = state.availableRooms
                .filter { room ->
                    room.id != message.roomId &&
                        now - (discoveredRoomLastSeen[room.id] ?: now) <= ROOM_STALE_TIMEOUT_MS
                }
            val shouldShow = message.hostId != humanPlayerId &&
                message.state == RoomState.Waiting &&
                message.players.size < message.maxPlayers
            val nextRooms = (if (shouldShow) activeRooms + roomInfo else activeRooms)
                .sortedWith(compareBy<RoomInfo> { it.name }.thenBy { it.id })
            if (nextRooms == state.availableRooms) state else state.copy(availableRooms = nextRooms)
        }
    }

    private fun pruneStaleRooms() {
        val now = System.currentTimeMillis()
        updateUiState { state ->
            val nextRooms = state.availableRooms.filter { room ->
                now - (discoveredRoomLastSeen[room.id] ?: now) <= ROOM_STALE_TIMEOUT_MS
            }
            if (nextRooms == state.availableRooms) state else state.copy(availableRooms = nextRooms)
        }
    }

    private fun resetCurrentRoomPreservingLobby() {
        clearHostGame()
        currentRoomId = ""
        currentRoomMaxPlayers = 3
        syncMqttSubscriptions(LOBBY_MQTT_TOPICS)
        updateUiState { state ->
            MultiplayerGameUiState(
                availableRooms = state.availableRooms,
                isConnected = mqttClient.isConnected(),
                connectionState = currentConnectionState()
            )
        }
    }

    private fun handleRoomListResponse(message: RoomListResponseMessage) {
        val now = System.currentTimeMillis()
        val visibleRooms = message.rooms.filter { room ->
            room.hostId != humanPlayerId &&
                room.state == RoomState.Waiting &&
                room.playerCount < room.maxPlayers
        }
        visibleRooms.forEach { room -> discoveredRoomLastSeen[room.id] = now }

        updateUiState { state ->
            val responseIds = visibleRooms.map { it.id }.toSet()
            val nextRooms = (state.availableRooms.filterNot { it.id in responseIds } + visibleRooms)
                .sortedWith(compareBy<RoomInfo> { it.name }.thenBy { it.id })
            if (nextRooms == state.availableRooms) state else state.copy(availableRooms = nextRooms)
        }
    }

    private fun isCurrentRoom(roomId: String): Boolean {
        return currentRoomId.isNotBlank() && currentRoomId == roomId
    }

    private fun currentConnectionState(): ConnectionState {
        return when (mqttClient.getConnectionState()) {
            com.happy.poker.core.network.ConnectionState.DISCONNECTED -> ConnectionState.DISCONNECTED
            com.happy.poker.core.network.ConnectionState.CONNECTING -> ConnectionState.CONNECTING
            com.happy.poker.core.network.ConnectionState.CONNECTED -> ConnectionState.CONNECTED
            com.happy.poker.core.network.ConnectionState.RECONNECTING -> ConnectionState.RECONNECTING
            com.happy.poker.core.network.ConnectionState.ERROR -> ConnectionState.ERROR
        }
    }

    fun setReady(isReady: Boolean) {
        val message = PlayerReadyMessage(
            roomId = currentRoomId,
            playerId = humanPlayerId,
            isReady = isReady
        )
        sendOnlineMessage(Protocol.TOPIC_PLAYER_READY, message, "准备")
    }

    fun startGame() {
        cancelPendingGameEndReveal()
        val room = _uiState.value.room
        if (!room.isHost || room.roomId.isBlank()) {
            showFeedback("等待房主开始游戏")
            return
        }
        if (room.players.size < 2) {
            showFeedback("至少需要2名真人玩家")
            return
        }

        stopRoomBroadcast()
        stopHumanTurnTimer(resetSeconds = false)
        aiActionJob?.cancel()
        aiActionJob = null
        currentRoomMaxPlayers = 3
        currentGameSettlementKey = "multi-${room.roomId}-${System.currentTimeMillis()}"
        settledGameKey = null

        val gamePlayers = playersWithAutoAi(room)
        val playingRoom = room.copy(
            players = gamePlayers,
            maxPlayers = 3,
            state = RoomState.Playing
        )
        updateUiState {
            it.copy(
                room = playingRoom,
                currentPlayerId = null,
                currentBid = 0,
                lastPlayedCards = null,
                lastPlayedPattern = null,
                lastPlayedBy = null,
                selectedCards = emptySet(),
                playerCards = emptyList(),
                bottomCards = emptyList(),
                multiplier = 1,
                gameResult = null,
                isPlayTurn = false,
                isBidTurn = false,
                turnSecondsRemaining = TURN_TIMEOUT_SECONDS
            )
        }
        syncMqttSubscriptions(GAME_MQTT_TOPICS)
        sendOnlineMessage(
            Protocol.TOPIC_ROOM_UPDATE,
            buildRoomStateMessage(playingRoom, RoomState.Playing),
            "同步房间",
            notifyFailure = false
        )

        hostRoom = createHostGameRoom(playingRoom, gamePlayers)
        aiManager.clear()
        hostRoom
            ?.players
            ?.filter { it.isAI }
            ?.forEach { aiManager.createAiPlayer(it) }
        gameFlow = hostRoom?.let { GameFlow(it, createHostGameCallback()) }
        gameFlow?.startGame()
    }

    fun selectCard(cardId: String) {
        if (!_uiState.value.isPlayTurn) return

        val currentSelected = _uiState.value.selectedCards.toMutableSet()
        if (cardId in currentSelected) {
            currentSelected.remove(cardId)
        } else {
            currentSelected.add(cardId)
        }
        updateUiState { it.copy(selectedCards = currentSelected) }
        GameAudio.cardSelect()
    }

    fun playCards() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        val selectedCards = state.playerCards.filter { it.id in state.selectedCards }
        if (selectedCards.isEmpty()) {
            showFeedback("请先选择要出的牌")
            return
        }

        val previousPattern = if (state.lastPlayedCards.isNullOrEmpty()) null else state.lastPlayedPattern
        val validation = Validator.validatePlay(selectedCards, previousPattern)
        if (!validation.isValid) {
            showFeedback(validation.reason.ifBlank { "不符合出牌规则" })
            return
        }

        val message = GamePlayMessage(
            roomId = currentRoomId,
            playerId = humanPlayerId,
            cards = selectedCards.map { CardInfo.fromCard(it) }
        )
        if (_uiState.value.room.isHost) {
            handlePlayCommand(message)
        } else {
            sendOnlineMessage(Protocol.TOPIC_GAME_PLAY, message, "出牌")
        }
    }

    fun pass() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        if (state.lastPlayedCards.isNullOrEmpty()) {
            showFeedback("本轮需要先出牌，不能不出")
            return
        }

        val message = GamePassMessage(
            roomId = currentRoomId,
            playerId = humanPlayerId
        )
        if (_uiState.value.room.isHost) {
            handlePassCommand(message)
        } else {
            sendOnlineMessage(Protocol.TOPIC_GAME_PASS, message, "不出")
        }
    }

    fun bid(bid: Int) {
        val state = _uiState.value
        if (!state.isBidTurn) return

        val message = GameBidMessage(
            roomId = currentRoomId,
            playerId = humanPlayerId,
            bid = bid
        )
        if (_uiState.value.room.isHost) {
            handleBidCommand(message)
        } else {
            sendOnlineMessage(Protocol.TOPIC_GAME_BID, message, "叫分")
        }
    }

    fun bidPass() {
        bid(0)
    }

    fun hintPlay() {
        val state = _uiState.value
        if (!state.isPlayTurn) {
            showFeedback("还没轮到你出牌")
            return
        }

        val humanPlayer = state.room.players.find { it.id == humanPlayerId }
        val isLandlord = humanPlayer?.role == PlayerRole.Landlord
        val landlordHandSize = state.room.players
            .firstOrNull { it.role == PlayerRole.Landlord }
            ?.handSize ?: 0
        val previousPattern = if (state.lastPlayedCards.isNullOrEmpty()) null else state.lastPlayedPattern
        val suggestion = AiEvaluator.suggestBestPlay(
            hand = state.playerCards,
            lastPattern = previousPattern,
            isLandlord = isLandlord,
            landlordHandSize = landlordHandSize
        )

        if (suggestion.isNullOrEmpty()) {
            pass()
            return
        }

        val validation = Validator.validatePlay(suggestion, previousPattern)
        if (!validation.isValid) {
            pass()
            return
        }

        updateUiState {
            it.copy(selectedCards = suggestion.map { card -> card.id }.toSet())
        }
        GameAudio.cardSelect()
    }

    fun refreshRoomList(notifyFailure: Boolean = false) {
        if (currentRoomId.isBlank()) {
            syncMqttSubscriptions(LOBBY_MQTT_TOPICS)
        }
        pruneStaleRooms()
        val message = RoomListMessage()
        sendOnlineMessage(Protocol.TOPIC_ROOM_LIST, message, "刷新房间", notifyFailure = notifyFailure)
    }

    fun clearError() {
        updateUiState { it.copy(errorMessage = null, feedbackMessage = null) }
    }

    private fun updateUiState(update: (MultiplayerGameUiState) -> MultiplayerGameUiState) {
        _uiState.value = update(_uiState.value)
    }

    private fun showFeedback(message: String) {
        updateUiState {
            it.copy(
                errorMessage = null,
                feedbackMessage = message,
                feedbackId = it.feedbackId + 1
            )
        }
    }

    fun cancelPendingGameEndReveal() {
        gameEndRevealJob?.cancel()
        gameEndRevealJob = null
    }

    private fun handleBidCommand(message: GameBidMessage) {
        if (!shouldHandleHostGameCommand(message.roomId)) return
        val flow = gameFlow ?: return
        val state = flow.getState()
        if (state.state != RoomState.Bidding || state.currentPlayerId != message.playerId) return

        if (!flow.playerBid(message.playerId, message.bid)) {
            publishHostGameState()
        }
    }

    private fun handlePlayCommand(message: GamePlayMessage) {
        if (!shouldHandleHostGameCommand(message.roomId)) return
        val flow = gameFlow ?: return
        val state = flow.getState()
        if (state.state != RoomState.Playing || state.currentPlayerId != message.playerId) return

        if (!flow.playerPlay(message.playerId, message.cards.map { it.toCard() })) {
            publishHostGameState()
        }
    }

    private fun handlePassCommand(message: GamePassMessage) {
        if (!shouldHandleHostGameCommand(message.roomId)) return
        val flow = gameFlow ?: return
        val state = flow.getState()
        if (state.state != RoomState.Playing || state.currentPlayerId != message.playerId) return

        if (!flow.playerPass(message.playerId)) {
            publishHostGameState()
        }
    }

    private fun shouldHandleHostGameCommand(roomId: String): Boolean {
        val room = _uiState.value.room
        return room.isHost && room.roomId == roomId && gameFlow != null
    }

    private fun publishHostGameState() {
        val flow = gameFlow ?: return
        val message = buildGameStateMessage(flow.getState())
        handleGameStateUpdate(message)
        sendOnlineMessage(Protocol.TOPIC_GAME_STATE, message, "同步牌局", notifyFailure = false)
    }

    private fun buildGameStateMessage(gameState: GameState): GameStateMessage {
        val sourceRoom = hostRoom
        val uiPlayersById = _uiState.value.room.players.associateBy { it.id }
        return GameStateMessage(
            roomId = gameState.roomId,
            state = gameState.state,
            currentPlayerId = gameState.currentPlayerId,
            landlordId = gameState.landlordId,
            multiplier = gameState.multiplier,
            lastPlayedCards = gameState.lastPlayedCards?.map { CardInfo.fromCard(it) },
            lastPlayedPlayerId = gameState.lastPlayedPlayerId,
            players = gameState.players.map { playerState ->
                PlayerStateInfo(
                    id = playerState.id,
                    name = playerState.name,
                    role = playerState.role,
                    handSize = playerState.handSize,
                    isOnline = playerState.isOnline,
                    isReady = playerState.isReady,
                    handCards = sourceRoom
                        ?.findPlayer(playerState.id)
                        ?.hand
                        ?.map { CardInfo.fromCard(it) }
                        .orEmpty(),
                    avatarKey = avatarKeyForPlayer(
                        playerState.id,
                        uiPlayersById[playerState.id]?.avatarKey
                    )
                )
            },
            currentBid = sourceRoom?.currentBid ?: _uiState.value.currentBid,
            bottomCards = gameState.bottomCards.map { CardInfo.fromCard(it) }
        )
    }

    private fun createHostGameCallback(): GameCallback {
        return object : GameCallback {
            override fun onGameStart(players: List<Player>, bottomCards: List<Card>) {
                updateUiState { it.copy(bottomCards = bottomCards, room = it.room.copy(state = RoomState.Bidding)) }
                val uiPlayersById = _uiState.value.room.players.associateBy { it.id }
                sendOnlineMessage(
                    Protocol.TOPIC_GAME_START,
                    GameStartMessage(
                        roomId = currentRoomId,
                        players = players.map { player ->
                            PlayerInfo(
                                id = player.id,
                                name = player.name,
                                isAI = player.isAI,
                                isOnline = player.isOnline,
                                avatarKey = avatarKeyForPlayer(
                                    player.id,
                                    uiPlayersById[player.id]?.avatarKey
                                )
                            )
                        },
                        bottomCards = bottomCards.map { CardInfo.fromCard(it) }
                    ),
                    "开始游戏",
                    notifyFailure = false
                )
            }

            override fun onDealCards(playerId: String, cards: List<Card>) {
                if (playerId == humanPlayerId) {
                    updateUiState { it.copy(playerCards = cards) }
                }
            }

            override fun onBidStart(firstBidderId: String) {
                publishHostGameState()
                if (isAiPlayer(firstBidderId)) {
                    scheduleHostAiBidTurn(firstBidderId)
                }
            }

            override fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean) {
                updateUiState {
                    it.copy(
                        currentBid = if (bid > it.currentBid) bid else it.currentBid,
                        isBidTurn = false
                    )
                }
                GameAudio.playBid(bid, isPass)
                stopHumanTurnTimer()
            }

            override fun onLandlordDecided(landlordId: String, bottomCards: List<Card>, multiplier: Int) {
                publishHostGameState()
            }

            override fun onPlayStart(landlordId: String, firstPlayerId: String) {
                publishHostGameState()
                if (isAiPlayer(firstPlayerId)) {
                    scheduleHostAiPlayTurn(firstPlayerId)
                }
            }

            override fun onPlayerPlay(
                playerId: String,
                playerName: String,
                cards: List<Card>,
                pattern: HandPattern,
                isPass: Boolean
            ) {
                val gameState = gameFlow?.getState()
                if (gameState != null) {
                    sendOnlineMessage(
                        Protocol.TOPIC_GAME_PLAY,
                        GamePlayResultMessage(
                            roomId = currentRoomId,
                            playerId = playerId,
                            playerName = playerName,
                            cards = cards.map { CardInfo.fromCard(it) },
                            pattern = PatternInfo.fromHandPattern(pattern),
                            isPass = isPass,
                            nextPlayerId = gameState.currentPlayerId,
                            handSize = gameState.players.firstOrNull { it.id == playerId }?.handSize ?: 0
                        ),
                        "同步出牌",
                        notifyFailure = false
                    )
                    publishHostGameState()
                    val nextPlayerId = gameState.currentPlayerId
                    if (
                        gameState.state == RoomState.Playing &&
                        gameState.players.none { it.handSize == 0 } &&
                        nextPlayerId != null &&
                        isAiPlayer(nextPlayerId)
                    ) {
                        scheduleHostAiPlayTurn(nextPlayerId)
                    }
                }
                if (isPass) {
                    GameAudio.playPass()
                } else if (!pattern.isBombOrRocket) {
                    GameAudio.playPattern(pattern, _uiState.value.multiplier)
                }
            }

            override fun onMultiplierChanged(multiplier: Int, bombCount: Int) {
                updateUiState { it.copy(multiplier = multiplier) }
                publishHostGameState()
            }

            override fun onSpring(landlordId: String, isLandlordWin: Boolean) {
                GameAudio.playSpring()
                publishHostGameState()
            }

            override fun onGameEnd(
                winnerId: String,
                winnerRole: PlayerRole,
                scores: Map<String, Int>,
                multiplier: Int
            ) {
                val message = GameEndMessage(
                    roomId = currentRoomId,
                    winnerId = winnerId,
                    winnerRole = winnerRole,
                    scores = scores,
                    multiplier = multiplier
                )
                handleGameEnded(message)
                sendOnlineMessage(Protocol.TOPIC_GAME_END, message, "同步结算", notifyFailure = false)
            }

            override fun onError(message: String) {
                showFeedback(message)
                publishHostGameState()
            }

            override fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean) = Unit
        }
    }

    private fun scheduleHostAiBidTurn(playerId: String) {
        aiActionJob?.cancel()
        aiActionJob = viewModelScope.launch {
            delay(900)
            val flow = gameFlow ?: return@launch
            if (flow.getState().state != RoomState.Bidding || flow.getState().currentPlayerId != playerId) {
                return@launch
            }
            aiManager.getAiPlayer(playerId)?.autoBid(flow, _uiState.value.currentBid)
            publishHostGameState()
        }
    }

    private fun scheduleHostAiPlayTurn(playerId: String) {
        aiActionJob?.cancel()
        aiActionJob = viewModelScope.launch {
            delay(1400)
            val flow = gameFlow ?: return@launch
            val state = flow.getState()
            if (state.state != RoomState.Playing || state.currentPlayerId != playerId) {
                return@launch
            }
            val sourceRoom = hostRoom ?: return@launch
            val ai = sourceRoom.findPlayer(playerId) ?: return@launch
            val isLandlord = state.landlordId == playerId
            val landlordHandSize = if (isLandlord) {
                ai.handSize
            } else {
                sourceRoom.landlord?.handSize ?: 0
            }
            aiManager.getAiPlayer(playerId)?.autoPlay(
                gameFlow = flow,
                lastPattern = state.activePreviousPatternFor(playerId),
                isLandlord = isLandlord,
                landlordHandSize = landlordHandSize
            )
            publishHostGameState()
        }
    }

    private fun GameState.activePreviousPatternFor(playerId: String): HandPattern? {
        if (lastPlayedCards.isNullOrEmpty()) return null
        if (lastPlayedPlayerId == playerId) return null
        return lastPlayedPattern
    }

    private fun handleRoomJoinRequest(message: RoomJoinMessage) {
        val state = _uiState.value
        val room = state.room
        if (!room.isHost || room.roomId != message.roomId || room.state != RoomState.Waiting) {
            return
        }
        if (room.players.any { it.id == message.playerId }) {
            sendOnlineMessage(Protocol.TOPIC_ROOM_UPDATE, buildRoomStateMessage(room), "同步房间", notifyFailure = false)
            return
        }
        if (room.players.size >= room.maxPlayers) {
            return
        }

        updateUiState {
            it.copy(
                room = it.room.copy(
                    players = it.room.players + PlayerUiState(
                        id = message.playerId,
                        name = message.playerName,
                        role = PlayerRole.Unknown,
                        handSize = 0,
                        isOnline = true,
                        beanBalance = PlayerProgressManager.INITIAL_BEAN_BALANCE,
                        avatarKey = avatarKeyForPlayer(message.playerId, message.avatarKey)
                    )
                )
            )
        }
        val updatedRoom = _uiState.value.room
        if (updatedRoom.maxPlayers == 2 && updatedRoom.players.size >= 2) {
            startGame()
        } else {
            sendOnlineMessage(Protocol.TOPIC_ROOM_UPDATE, buildRoomStateMessage(updatedRoom), "同步房间", notifyFailure = false)
        }
    }

    private fun handleRoomLeaveRequest(message: RoomLeaveMessage) {
        val room = _uiState.value.room
        if (!room.isHost || room.roomId != message.roomId || message.playerId == humanPlayerId) {
            return
        }

        updateUiState {
            it.copy(
                room = it.room.copy(
                    players = it.room.players.filterNot { player -> player.id == message.playerId }
                )
            )
        }
        sendOnlineMessage(Protocol.TOPIC_ROOM_UPDATE, buildRoomStateMessage(_uiState.value.room), "同步房间", notifyFailure = false)
    }

    private fun handleRoomListRequest() {
        val room = _uiState.value.room
        if (room.isHost && room.roomId.isNotBlank() && room.state == RoomState.Waiting) {
            sendOnlineMessage(Protocol.TOPIC_ROOM_UPDATE, buildRoomStateMessage(room), "同步房间", notifyFailure = false)
        }
    }

    private fun handleRoomCreated(message: RoomCreatedMessage) {
        currentRoomId = message.roomId
        updateUiState {
            it.copy(
                room = RoomUiState(
                    roomId = message.roomId,
                    roomName = message.roomName,
                    players = listOf(
                        PlayerUiState(
                            humanPlayerId,
                            appSettingsManager.getNickname(),
                            PlayerRole.Unknown,
                            0,
                            true,
                            beanBalanceForPlayer(humanPlayerId),
                            localAvatarKey()
                        )
                    ),
                    maxPlayers = currentRoomMaxPlayers,
                    state = RoomState.Waiting,
                    isHost = true,
                    hostId = humanPlayerId
                ),
                currentPlayerId = null,
                currentBid = 0,
                lastPlayedCards = null,
                lastPlayedPattern = null,
                lastPlayedBy = null,
                selectedCards = emptySet(),
                isPlayTurn = false,
                isBidTurn = false,
                bottomCards = emptyList(),
                multiplier = 1,
                gameResult = null,
                turnSecondsRemaining = TURN_TIMEOUT_SECONDS,
                isSearching = false
            )
        }
        syncMqttSubscriptions(WAITING_ROOM_MQTT_TOPICS)
    }

    private fun handleRoomJoined(message: RoomJoinedMessage) {
        if (!isCurrentRoom(message.roomId) || message.players.none { it.id == humanPlayerId }) {
            return
        }

        currentRoomId = message.roomId
        updateUiState {
            val existingPlayersById = it.room.players.associateBy { player -> player.id }
            it.copy(
                room = it.room.copy(
                    roomId = message.roomId,
                    maxPlayers = currentRoomMaxPlayers,
                    players = message.players.map { p ->
                        val existing = existingPlayersById[p.id]
                        PlayerUiState(
                            id = p.id,
                            name = p.name,
                            role = existing?.role ?: PlayerRole.Unknown,
                            handSize = existing?.handSize ?: 0,
                            isOnline = p.isOnline,
                            beanBalance = beanBalanceForPlayer(p.id, existing),
                            avatarKey = avatarKeyForPlayer(p.id, p.avatarKey, existing)
                        )
                    }
                ),
                currentPlayerId = null,
                currentBid = 0,
                lastPlayedCards = null,
                lastPlayedPattern = null,
                lastPlayedBy = null,
                selectedCards = emptySet(),
                isPlayTurn = false,
                isBidTurn = false,
                bottomCards = emptyList(),
                multiplier = 1,
                gameResult = null,
                turnSecondsRemaining = TURN_TIMEOUT_SECONDS,
                isSearching = false
            )
        }
        syncMqttSubscriptions(WAITING_ROOM_MQTT_TOPICS)
    }

    private fun handleRoomState(message: RoomStateMessage) {
        if (currentRoomId.isBlank() || currentRoomId != message.roomId) {
            updateDiscoveredRoom(message)
            return
        }

        if (message.state == RoomState.Finished && _uiState.value.gameResult == null) {
            updateDiscoveredRoom(message)
            resetCurrentRoomPreservingLobby()
            showFeedback("房间已关闭")
            return
        }

        currentRoomMaxPlayers = message.maxPlayers
        syncMqttSubscriptions(
            if (message.state == RoomState.Bidding || message.state == RoomState.Playing) {
                GAME_MQTT_TOPICS
            } else {
                WAITING_ROOM_MQTT_TOPICS
            }
        )
        updateUiState {
            val existingPlayersById = it.room.players.associateBy { player -> player.id }
            val isHost = message.hostId == humanPlayerId
            it.copy(
                room = RoomUiState(
                    roomId = message.roomId,
                    roomName = message.roomName,
                    maxPlayers = message.maxPlayers,
                    players = message.players.map { p ->
                        val existing = existingPlayersById[p.id]
                        PlayerUiState(
                            id = p.id,
                            name = p.name,
                            role = existing?.role ?: PlayerRole.Unknown,
                            handSize = existing?.handSize ?: 0,
                            isOnline = p.isOnline,
                            beanBalance = beanBalanceForPlayer(p.id, existing),
                            avatarKey = avatarKeyForPlayer(p.id, p.avatarKey, existing)
                        )
                    },
                    state = message.state,
                    isHost = isHost,
                    hostId = message.hostId
                ),
                currentPlayerId = if (message.state == RoomState.Waiting) null else it.currentPlayerId,
                currentBid = if (message.state == RoomState.Waiting) 0 else it.currentBid,
                lastPlayedCards = if (message.state == RoomState.Waiting) null else it.lastPlayedCards,
                lastPlayedPattern = if (message.state == RoomState.Waiting) null else it.lastPlayedPattern,
                lastPlayedBy = if (message.state == RoomState.Waiting) null else it.lastPlayedBy,
                selectedCards = if (message.state == RoomState.Waiting) emptySet() else it.selectedCards,
                isPlayTurn = if (message.state == RoomState.Waiting) false else it.isPlayTurn,
                isBidTurn = if (message.state == RoomState.Waiting) false else it.isBidTurn,
                gameResult = if (message.state == RoomState.Waiting) null else it.gameResult,
                turnSecondsRemaining = if (message.state == RoomState.Waiting) TURN_TIMEOUT_SECONDS else it.turnSecondsRemaining
            )
        }
    }

    private fun handleGameStarted(message: GameStartMessage) {
        if (!isCurrentRoom(message.roomId)) {
            return
        }
        if (_uiState.value.room.isHost) {
            return
        }

        stopRoomBroadcast()
        syncMqttSubscriptions(GAME_MQTT_TOPICS)
        currentRoomMaxPlayers = 3
        currentGameSettlementKey = "multi-${message.roomId}-${message.timestamp}"
        settledGameKey = null
        cancelPendingGameEndReveal()
        val currentState = _uiState.value
        val hasSynchronizedGameState = currentState.currentPlayerId != null ||
            currentState.playerCards.isNotEmpty() ||
            currentState.isBidTurn ||
            currentState.isPlayTurn
        if (hasSynchronizedGameState) {
            updateUiState {
                it.copy(
                    bottomCards = message.bottomCards
                        .takeIf { cards -> cards.isNotEmpty() }
                        ?.map { card -> card.toCard() }
                        ?: it.bottomCards
                )
            }
            return
        }
        stopHumanTurnTimer()
        updateUiState {
            val existingPlayersById = it.room.players.associateBy { player -> player.id }
            val nextPlayers = message.players.map { playerInfo ->
                val existing = existingPlayersById[playerInfo.id]
                PlayerUiState(
                    id = playerInfo.id,
                    name = playerInfo.name,
                    role = existing?.role ?: PlayerRole.Unknown,
                    handSize = existing?.handSize ?: 0,
                    isOnline = playerInfo.isOnline,
                    beanBalance = beanBalanceForPlayer(playerInfo.id, existing),
                    avatarKey = avatarKeyForPlayer(playerInfo.id, playerInfo.avatarKey, existing)
                )
            }
            it.copy(
                bottomCards = message.bottomCards.map { it.toCard() },
                room = it.room.copy(
                    state = RoomState.Bidding,
                    maxPlayers = 3,
                    players = nextPlayers
                ),
                currentPlayerId = null,
                currentBid = 0,
                lastPlayedCards = null,
                lastPlayedPattern = null,
                lastPlayedBy = null,
                selectedCards = emptySet(),
                isPlayTurn = false,
                isBidTurn = false,
                gameResult = null,
                turnSecondsRemaining = TURN_TIMEOUT_SECONDS
            )
        }
    }

    private fun handleBidResult(message: GameBidResultMessage) {
        if (!isCurrentRoom(message.roomId)) {
            return
        }

        val nextBidderId = message.nextBidderId
        updateUiState {
            it.copy(
                currentBid = message.currentBid,
                currentPlayerId = nextBidderId,
                isBidTurn = nextBidderId == humanPlayerId,
                isPlayTurn = false,
                room = it.room.copy(state = RoomState.Bidding),
                turnSecondsRemaining = if (nextBidderId == humanPlayerId) TURN_TIMEOUT_SECONDS else it.turnSecondsRemaining
            )
        }
        GameAudio.playBid(message.bid, message.isPass)
        if (nextBidderId == humanPlayerId) {
            startHumanTurnTimer(MultiplayerTurnPhase.Bidding)
        } else {
            stopHumanTurnTimer()
        }
    }

    private fun handlePlayResult(message: GamePlayResultMessage) {
        if (!isCurrentRoom(message.roomId)) {
            return
        }
        if (_uiState.value.room.isHost) {
            return
        }

        val cards = message.cards.map { it.toCard() }
        val nextPlayerId = message.nextPlayerId
        val playedPattern = message.pattern.toHandPattern()
        updateUiState {
            it.copy(
                lastPlayedCards = if (!message.isPass) cards else it.lastPlayedCards,
                lastPlayedPattern = if (!message.isPass) playedPattern else it.lastPlayedPattern,
                lastPlayedBy = if (!message.isPass) message.playerId else it.lastPlayedBy,
                currentPlayerId = nextPlayerId,
                isPlayTurn = nextPlayerId == humanPlayerId,
                isBidTurn = false,
                room = it.room.copy(state = RoomState.Playing),
                selectedCards = emptySet(),
                turnSecondsRemaining = if (nextPlayerId == humanPlayerId) TURN_TIMEOUT_SECONDS else it.turnSecondsRemaining
            )
        }
        if (message.isPass) {
            GameAudio.playPass()
        } else if (!playedPattern.isBombOrRocket) {
            GameAudio.playPattern(playedPattern, _uiState.value.multiplier)
        }
        if (nextPlayerId == humanPlayerId) {
            startHumanTurnTimer(MultiplayerTurnPhase.Playing)
        } else {
            stopHumanTurnTimer()
        }
    }

    private fun handleGameStateUpdate(message: GameStateMessage) {
        if (!isCurrentRoom(message.roomId)) {
            return
        }

        val lastPlayedCards = message.lastPlayedCards?.map { it.toCard() }
        val previousMultiplier = _uiState.value.multiplier
        val existingPlayersById = _uiState.value.room.players.associateBy { player -> player.id }
        val humanHandCards = message.players
            .firstOrNull { it.id == humanPlayerId }
            ?.handCards
            ?.map { it.toCard() }
            .orEmpty()
        updateUiState {
            val nextCurrentPlayerId = message.currentPlayerId
            val isBidTurn = message.state == RoomState.Bidding && nextCurrentPlayerId == humanPlayerId
            val isPlayTurn = message.state == RoomState.Playing && nextCurrentPlayerId == humanPlayerId
            it.copy(
                multiplier = message.multiplier,
                currentBid = message.currentBid,
                currentPlayerId = nextCurrentPlayerId,
                lastPlayedCards = lastPlayedCards,
                lastPlayedPattern = if (lastPlayedCards.isNullOrEmpty()) null else Validator.identify(lastPlayedCards).pattern,
                lastPlayedBy = message.lastPlayedPlayerId,
                isBidTurn = isBidTurn,
                isPlayTurn = isPlayTurn,
                playerCards = humanHandCards,
                bottomCards = message.bottomCards
                    .takeIf { cards -> cards.isNotEmpty() }
                    ?.map { card -> card.toCard() }
                    ?: it.bottomCards,
                room = it.room.copy(
                    state = message.state,
                    maxPlayers = 3,
                    isHost = it.room.hostId == humanPlayerId,
                    players = message.players.map { ps ->
                        val existing = existingPlayersById[ps.id]
                        PlayerUiState(
                            id = ps.id,
                            name = ps.name,
                            role = ps.role,
                            handSize = ps.handSize,
                            isOnline = ps.isOnline,
                            beanBalance = beanBalanceForPlayer(ps.id, existing),
                            avatarKey = avatarKeyForPlayer(ps.id, ps.avatarKey, existing)
                        )
                    }
                ),
                turnSecondsRemaining = when {
                    isBidTurn || isPlayTurn -> TURN_TIMEOUT_SECONDS
                    else -> it.turnSecondsRemaining
                }
            )
        }
        val identifiedPattern = lastPlayedCards?.let { Validator.identify(it).pattern }
        if (message.multiplier > previousMultiplier && identifiedPattern?.isBombOrRocket == true) {
            GameAudio.playPattern(identifiedPattern, message.multiplier)
        }
        when {
            message.state == RoomState.Bidding && message.currentPlayerId == humanPlayerId -> startHumanTurnTimer(MultiplayerTurnPhase.Bidding)
            message.state == RoomState.Playing && message.currentPlayerId == humanPlayerId -> startHumanTurnTimer(MultiplayerTurnPhase.Playing)
            else -> stopHumanTurnTimer()
        }
    }

    private fun handleGameEnded(message: GameEndMessage) {
        if (!isCurrentRoom(message.roomId)) {
            return
        }

        stopHumanTurnTimer()
        val settlementKey = if (currentGameSettlementKey.isBlank()) {
            "multi-${message.roomId}-${message.timestamp}"
        } else {
            currentGameSettlementKey
        }
        if (settledGameKey == settlementKey) {
            return
        }
        val humanScore = message.scores[humanPlayerId] ?: 0
        val beanSettlement = playerProgressManager.settleGameResult(settlementKey, humanScore)
        settledGameKey = settlementKey
        val updatedPlayers = _uiState.value.room.players.map { player ->
            val scoreChange = message.scores[player.id] ?: 0
            val nextBalance = if (player.id == humanPlayerId) {
                beanSettlement.balance
            } else {
                (player.beanBalance + scoreChange).coerceAtLeast(0)
            }
            player.copy(beanBalance = nextBalance)
        }
        val humanSideWon = _uiState.value.room.players.find { it.id == humanPlayerId }?.role == message.winnerRole
        if (humanSideWon) {
            GameAudio.playWin()
        } else {
            GameAudio.playLose()
        }
        aiActionJob?.cancel()
        aiActionJob = null
        cancelPendingGameEndReveal()
        gameEndRevealJob = viewModelScope.launch {
            delay(GAME_END_REVEAL_DELAY_MS)
            updateUiState {
                it.copy(
                    room = it.room.copy(
                        state = RoomState.Finished,
                        players = updatedPlayers
                    ),
                    isPlayTurn = false,
                    isBidTurn = false,
                    currentPlayerId = null,
                    gameResult = GameResult(
                        winnerId = message.winnerId,
                        winnerRole = message.winnerRole,
                        scores = message.scores,
                        multiplier = message.multiplier,
                        beanDelta = beanSettlement.delta,
                        beanBalance = beanSettlement.balance
                    ),
                    turnSecondsRemaining = TURN_TIMEOUT_SECONDS
                )
            }
        }
    }

    private fun handleError(message: ErrorMessage) {
        showFeedback(message.message)
    }

    private fun startHumanTurnTimer(phase: MultiplayerTurnPhase) {
        turnTimerJob?.cancel()
        val token = ++turnTimerToken
        updateUiState { it.copy(turnSecondsRemaining = TURN_TIMEOUT_SECONDS) }

        turnTimerJob = viewModelScope.launch {
            for (remaining in (TURN_TIMEOUT_SECONDS - 1) downTo 0) {
                delay(1000)
                if (token != turnTimerToken) return@launch

                val state = _uiState.value
                val stillMyTurn = when (phase) {
                    MultiplayerTurnPhase.Bidding -> state.room.state == RoomState.Bidding && state.isBidTurn
                    MultiplayerTurnPhase.Playing -> state.room.state == RoomState.Playing && state.isPlayTurn
                }
                if (!stillMyTurn) return@launch

                updateUiState { it.copy(turnSecondsRemaining = remaining) }
                if (remaining == 0) {
                    showFeedback("倒计时结束")
                }
            }
        }
    }

    private fun stopHumanTurnTimer(resetSeconds: Boolean = true) {
        turnTimerJob?.cancel()
        turnTimerJob = null
        turnTimerToken += 1
        if (resetSeconds) {
            updateUiState { it.copy(turnSecondsRemaining = TURN_TIMEOUT_SECONDS) }
        }
    }

    private fun clearHostGame() {
        aiActionJob?.cancel()
        aiActionJob = null
        gameFlow = null
        hostRoom = null
        aiManager.clear()
    }

    override fun onCleared() {
        super.onCleared()
        clearHostGame()
        viewModelScope.launch {
            mqttClient.disconnect()
        }
    }
}
