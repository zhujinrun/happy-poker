package com.happy.poker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happy.poker.core.model.*
import com.happy.poker.core.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val playerCards: List<Card> = emptyList(),
    val selectedCards: Set<String> = emptySet(),
    val bottomCards: List<Card> = emptyList(),
    val multiplier: Int = 1,
    val isPlayTurn: Boolean = false,
    val isBidTurn: Boolean = false,
    val currentBid: Int = 0,
    val lastPlayedCards: List<Card>? = null,
    val lastPlayedPattern: HandPattern? = null,
    val gameResult: GameResult? = null,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
    val isSearching: Boolean = false
)

class MultiplayerGameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MultiplayerGameUiState())
    val uiState: StateFlow<MultiplayerGameUiState> = _uiState.asStateFlow()

    private val mqttClient = GameMqttClient()
    private val humanPlayerId: String = "human_player_${System.currentTimeMillis()}"
    private var currentRoomId: String = ""

    init {
        connectToBroker()
    }

    private fun connectToBroker() {
        viewModelScope.launch {
            try {
                mqttClient.connect()
                updateUiState { it.copy(isConnected = true) }
                setupMessageHandlers()
            } catch (e: Exception) {
                updateUiState { it.copy(errorMessage = "连接服务器失败: ${e.message}") }
            }
        }
    }

    private fun setupMessageHandlers() {
        viewModelScope.launch {
            mqttClient.incomingMessages.collect { message ->
                when (message) {
                    is RoomCreatedMessage -> handleRoomCreated(message)
                    is RoomJoinedMessage -> handleRoomJoined(message)
                    is RoomStateMessage -> handleRoomState(message)
                    is GameStartMessage -> handleGameStarted(message)
                    is GameBidResultMessage -> handleBidResult(message)
                    is GamePlayResultMessage -> handlePlayResult(message)
                    is GameStateMessage -> handleGameStateUpdate(message)
                    is GameEndMessage -> handleGameEnded(message)
                    is ErrorMessage -> handleError(message)
                    else -> {}
                }
            }
        }
    }

    fun createRoom(roomName: String, maxPlayers: Int) {
        viewModelScope.launch {
            updateUiState { it.copy(isSearching = true) }
            val message = RoomCreateMessage(
                playerName = "我",
                playerId = humanPlayerId,
                maxPlayers = maxPlayers
            )
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_CREATE, message)
        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            updateUiState { it.copy(isSearching = true) }
            val message = RoomJoinMessage(
                roomId = roomId,
                playerName = "我",
                playerId = humanPlayerId
            )
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_JOIN, message)
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            val message = RoomLeaveMessage(
                roomId = currentRoomId,
                playerId = humanPlayerId
            )
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_LEAVE, message)
            currentRoomId = ""
            updateUiState { MultiplayerGameUiState() }
        }
    }

    fun setReady(isReady: Boolean) {
        viewModelScope.launch {
            val message = PlayerReadyMessage(
                roomId = currentRoomId,
                playerId = humanPlayerId,
                isReady = isReady
            )
            mqttClient.sendMessage(Protocol.TOPIC_PLAYER_READY, message)
        }
    }

    fun startGame() {
        viewModelScope.launch {
            val message = GameStartMessage(
                roomId = currentRoomId,
                players = _uiState.value.room.players.map { p ->
                    PlayerInfo(
                        id = p.id,
                        name = p.name,
                        isAI = false,
                        isOnline = true
                    )
                },
                bottomCards = emptyList()
            )
            mqttClient.sendMessage(Protocol.TOPIC_GAME_START, message)
        }
    }

    fun selectCard(cardId: String) {
        val currentSelected = _uiState.value.selectedCards.toMutableSet()
        if (cardId in currentSelected) {
            currentSelected.remove(cardId)
        } else {
            currentSelected.add(cardId)
        }
        updateUiState { it.copy(selectedCards = currentSelected) }
    }

    fun playCards() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        val selectedCards = state.playerCards.filter { it.id in state.selectedCards }
        if (selectedCards.isEmpty()) {
            updateUiState { it.copy(errorMessage = "请先选择要出的牌") }
            return
        }

        viewModelScope.launch {
            val message = GamePlayMessage(
                roomId = currentRoomId,
                playerId = humanPlayerId,
                cards = selectedCards.map { CardInfo.fromCard(it) }
            )
            mqttClient.sendMessage(Protocol.TOPIC_GAME_PLAY, message)
        }
    }

    fun pass() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        viewModelScope.launch {
            val message = GamePassMessage(
                roomId = currentRoomId,
                playerId = humanPlayerId
            )
            mqttClient.sendMessage(Protocol.TOPIC_GAME_PASS, message)
        }
    }

    fun bid(bid: Int) {
        val state = _uiState.value
        if (!state.isBidTurn) return

        viewModelScope.launch {
            val message = GameBidMessage(
                roomId = currentRoomId,
                playerId = humanPlayerId,
                bid = bid
            )
            mqttClient.sendMessage(Protocol.TOPIC_GAME_BID, message)
        }
    }

    fun bidPass() {
        bid(0)
    }

    fun refreshRoomList() {
        viewModelScope.launch {
            val message = RoomListMessage()
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_LIST, message)
        }
    }

    fun clearError() {
        updateUiState { it.copy(errorMessage = null) }
    }

    private fun updateUiState(update: (MultiplayerGameUiState) -> MultiplayerGameUiState) {
        _uiState.value = update(_uiState.value)
    }

    private fun handleRoomCreated(message: RoomCreatedMessage) {
        currentRoomId = message.roomId
        updateUiState {
            it.copy(
                room = RoomUiState(
                    roomId = message.roomId,
                    roomName = message.roomName,
                    players = listOf(
                        PlayerUiState(humanPlayerId, "我", PlayerRole.Unknown, 0, true)
                    ),
                    maxPlayers = 3,
                    isHost = true,
                    hostId = humanPlayerId
                ),
                isSearching = false
            )
        }
    }

    private fun handleRoomJoined(message: RoomJoinedMessage) {
        currentRoomId = message.roomId
        updateUiState {
            it.copy(
                room = it.room.copy(
                    roomId = message.roomId,
                    players = message.players.map { p ->
                        PlayerUiState(
                            id = p.id,
                            name = p.name,
                            role = PlayerRole.Unknown,
                            handSize = 0,
                            isOnline = p.isOnline
                        )
                    }
                ),
                isSearching = false
            )
        }
    }

    private fun handleRoomState(message: RoomStateMessage) {
        updateUiState {
            it.copy(
                room = RoomUiState(
                    roomId = message.roomId,
                    roomName = message.roomName,
                    players = message.players.map { p ->
                        PlayerUiState(
                            id = p.id,
                            name = p.name,
                            role = PlayerRole.Unknown,
                            handSize = 0,
                            isOnline = p.isOnline
                        )
                    },
                    state = message.state,
                    hostId = message.hostId
                )
            )
        }
    }

    private fun handleGameStarted(message: GameStartMessage) {
        updateUiState {
            it.copy(
                bottomCards = message.bottomCards.map { it.toCard() },
                room = it.room.copy(state = RoomState.Playing)
            )
        }
    }

    private fun handleBidResult(message: GameBidResultMessage) {
        updateUiState {
            it.copy(
                currentBid = message.currentBid,
                isBidTurn = false
            )
        }
    }

    private fun handlePlayResult(message: GamePlayResultMessage) {
        val cards = message.cards.map { it.toCard() }
        updateUiState {
            it.copy(
                lastPlayedCards = if (!message.isPass) cards else it.lastPlayedCards,
                lastPlayedPattern = if (!message.isPass) message.pattern.toHandPattern() else it.lastPlayedPattern,
                isPlayTurn = false,
                selectedCards = emptySet()
            )
        }
    }

    private fun handleGameStateUpdate(message: GameStateMessage) {
        val myState = message.players.find { it.id == humanPlayerId }
        updateUiState {
            it.copy(
                multiplier = message.multiplier,
                lastPlayedCards = message.lastPlayedCards?.map { it.toCard() },
                room = it.room.copy(
                    state = message.state,
                    players = message.players.map { ps ->
                        PlayerUiState(
                            id = ps.id,
                            name = ps.name,
                            role = ps.role,
                            handSize = ps.handSize,
                            isOnline = ps.isOnline
                        )
                    }
                )
            )
        }
    }

    private fun handleGameEnded(message: GameEndMessage) {
        updateUiState {
            it.copy(
                room = it.room.copy(state = RoomState.Finished),
                isPlayTurn = false,
                isBidTurn = false,
                gameResult = GameResult(
                    winnerId = message.winnerId,
                    winnerRole = message.winnerRole,
                    scores = message.scores,
                    multiplier = message.multiplier
                )
            )
        }
    }

    private fun handleError(message: ErrorMessage) {
        updateUiState { it.copy(errorMessage = message.message) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            mqttClient.disconnect()
        }
    }
}
