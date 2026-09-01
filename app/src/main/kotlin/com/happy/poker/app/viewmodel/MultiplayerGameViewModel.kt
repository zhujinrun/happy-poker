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
import com.happy.poker.core.ai.AiEvaluator
import com.happy.poker.core.model.*
import com.happy.poker.core.network.*
import com.happy.poker.core.rules.Validator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TURN_TIMEOUT_SECONDS = 30
private const val GAME_END_REVEAL_DELAY_MS = 1200L

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
    private var currentRoomId: String = ""
    private var currentRoomMaxPlayers: Int = 3
    private var currentGameSettlementKey: String = ""
    private var settledGameKey: String? = null
    private val reconnectManager = ReconnectManager()
    private val mqttConfigManager = MqttConfigManager(application)
    private val appSettingsManager = AppSettingsManager(application)
    private val playerProgressManager = PlayerProgressManager(application)
    private var turnTimerJob: Job? = null
    private var turnTimerToken: Int = 0
    private var gameEndRevealJob: Job? = null

    init {
        reconnectManager.init(viewModelScope) { reconnect() }
        connectToBroker()
    }

    private fun beanBalanceForPlayer(playerId: String, existing: PlayerUiState? = null): Int {
        return if (playerId == humanPlayerId) {
            playerProgressManager.getBeanBalance()
        } else {
            existing?.beanBalance ?: PlayerProgressManager.INITIAL_BEAN_BALANCE
        }
    }

    private fun connectToBroker() {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(connectionState = ConnectionState.CONNECTING) }
                val config = mqttConfigManager.getMqttConnectionConfig()
                mqttClient.connect(config)
                reconnectManager.onConnected()
                updateUiState { it.copy(isConnected = true, connectionState = ConnectionState.CONNECTED) }
                setupMessageHandlers()
            } catch (e: Exception) {
                reconnectManager.onError(e)
                updateUiState { it.copy(connectionState = ConnectionState.ERROR) }
                showFeedback("连接服务器失败: ${e.message}")
            }
        }
    }

    private fun reconnect() {
        viewModelScope.launch {
            try {
                updateUiState { it.copy(connectionState = ConnectionState.RECONNECTING) }
                val config = mqttConfigManager.getMqttConnectionConfig()
                mqttClient.connect(config)
                reconnectManager.onConnected()
                updateUiState { it.copy(isConnected = true, connectionState = ConnectionState.CONNECTED) }
                setupMessageHandlers()
            } catch (e: Exception) {
                reconnectManager.onError(e)
                updateUiState { 
                    it.copy(
                        connectionState = ConnectionState.ERROR
                    ) 
                }
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
        if (maxPlayers !in Room.SUPPORTED_PLAYER_COUNTS) {
            showFeedback("当前只支持2人或3人房间")
            return
        }

        viewModelScope.launch {
            currentRoomMaxPlayers = maxPlayers
            updateUiState { it.copy(isSearching = true) }
            val message = RoomCreateMessage(
                playerName = appSettingsManager.getNickname(),
                playerId = humanPlayerId,
                maxPlayers = maxPlayers
            )
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_CREATE, message)
        }
    }

    fun joinRoom(roomId: String, maxPlayers: Int = 3) {
        if (maxPlayers !in Room.SUPPORTED_PLAYER_COUNTS) {
            showFeedback("当前只支持2人或3人房间")
            return
        }

        viewModelScope.launch {
            currentRoomMaxPlayers = maxPlayers
            updateUiState { it.copy(isSearching = true) }
            val message = RoomJoinMessage(
                roomId = roomId,
                playerName = appSettingsManager.getNickname(),
                playerId = humanPlayerId
            )
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_JOIN, message)
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            stopHumanTurnTimer()
            cancelPendingGameEndReveal()
            val message = RoomLeaveMessage(
                roomId = currentRoomId,
                playerId = humanPlayerId
            )
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_LEAVE, message)
            currentRoomId = ""
            currentRoomMaxPlayers = 3
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
            cancelPendingGameEndReveal()
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

        if (state.lastPlayedCards.isNullOrEmpty()) {
            showFeedback("本轮需要先出牌，不能不出")
            return
        }

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

    fun refreshRoomList() {
        viewModelScope.launch {
            val message = RoomListMessage()
            mqttClient.sendMessage(Protocol.TOPIC_ROOM_LIST, message)
        }
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
                            beanBalanceForPlayer(humanPlayerId)
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
    }

    private fun handleRoomJoined(message: RoomJoinedMessage) {
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
                            beanBalance = beanBalanceForPlayer(p.id, existing)
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
    }

    private fun handleRoomState(message: RoomStateMessage) {
        currentRoomId = message.roomId
        updateUiState {
            val existingPlayersById = it.room.players.associateBy { player -> player.id }
            val isHost = message.hostId == humanPlayerId
            it.copy(
                room = RoomUiState(
                    roomId = message.roomId,
                    roomName = message.roomName,
                    maxPlayers = currentRoomMaxPlayers,
                    players = message.players.map { p ->
                        val existing = existingPlayersById[p.id]
                        PlayerUiState(
                            id = p.id,
                            name = p.name,
                            role = existing?.role ?: PlayerRole.Unknown,
                            handSize = existing?.handSize ?: 0,
                            isOnline = p.isOnline,
                            beanBalance = beanBalanceForPlayer(p.id, existing)
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
        currentRoomId = message.roomId
        currentGameSettlementKey = "multi-${message.roomId}-${message.timestamp}"
        settledGameKey = null
        cancelPendingGameEndReveal()
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
                    beanBalance = beanBalanceForPlayer(playerInfo.id, existing)
                )
            }
            it.copy(
                bottomCards = message.bottomCards.map { it.toCard() },
                room = it.room.copy(
                    state = RoomState.Bidding,
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
        currentRoomId = message.roomId
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
        currentRoomId = message.roomId
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
        currentRoomId = message.roomId
        val lastPlayedCards = message.lastPlayedCards?.map { it.toCard() }
        val previousMultiplier = _uiState.value.multiplier
        val existingPlayersById = _uiState.value.room.players.associateBy { player -> player.id }
        updateUiState {
            val nextCurrentPlayerId = message.currentPlayerId
            val isBidTurn = message.state == RoomState.Bidding && nextCurrentPlayerId == humanPlayerId
            val isPlayTurn = message.state == RoomState.Playing && nextCurrentPlayerId == humanPlayerId
            it.copy(
                multiplier = message.multiplier,
                currentPlayerId = nextCurrentPlayerId,
                lastPlayedCards = lastPlayedCards,
                lastPlayedPattern = if (lastPlayedCards.isNullOrEmpty()) null else Validator.identify(lastPlayedCards).pattern,
                lastPlayedBy = message.lastPlayedPlayerId,
                isBidTurn = isBidTurn,
                isPlayTurn = isPlayTurn,
                room = it.room.copy(
                    state = message.state,
                    isHost = it.room.hostId == humanPlayerId,
                    players = message.players.map { ps ->
                        val existing = existingPlayersById[ps.id]
                        PlayerUiState(
                            id = ps.id,
                            name = ps.name,
                            role = ps.role,
                            handSize = ps.handSize,
                            isOnline = ps.isOnline,
                            beanBalance = beanBalanceForPlayer(ps.id, existing)
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
        currentRoomId = message.roomId
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            mqttClient.disconnect()
        }
    }
}
