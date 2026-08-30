package com.happy.poker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happy.poker.core.ai.AiEvaluator
import com.happy.poker.core.ai.AiManager
import com.happy.poker.core.flow.GameCallback
import com.happy.poker.core.flow.GameFlow
import com.happy.poker.core.flow.GameState
import com.happy.poker.core.model.*
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.core.rules.Validator
import com.happy.poker.app.effects.SpecialEffectsManager
import com.happy.poker.app.effects.SpecialEffectState
import com.happy.poker.app.effects.SpecialEffectType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TURN_TIMEOUT_SECONDS = 30
private const val AI_PLAY_REVEAL_DELAY_MS = 1600L

private enum class HumanTurnPhase {
    Bidding,
    Playing
}

data class PlayerUiState(
    val id: String,
    val name: String,
    val role: PlayerRole = PlayerRole.Unknown,
    val handSize: Int = 0,
    val isOnline: Boolean = true
)

data class GameUiState(
    val players: List<PlayerUiState> = emptyList(),
    val currentPlayerId: String? = null,
    val playerCards: List<Card> = emptyList(),
    val selectedCards: Set<String> = emptySet(),
    val bottomCards: List<Card> = emptyList(),
    val multiplier: Int = 1,
    val roomState: RoomState = RoomState.Waiting,
    val isPlayTurn: Boolean = false,
    val isBidTurn: Boolean = false,
    val currentBid: Int = 0,
    val lastPlayedCards: List<Card>? = null,
    val lastPlayedBy: String? = null,
    val lastPlayedPattern: HandPattern? = null,
    val gameResult: GameResult? = null,
    val errorMessage: String? = null,
    val feedbackMessage: String? = null,
    val feedbackId: Int = 0,
    val turnSecondsRemaining: Int = TURN_TIMEOUT_SECONDS
)

data class GameResult(
    val winnerId: String,
    val winnerRole: PlayerRole,
    val scores: Map<String, Int>,
    val multiplier: Int
)

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var room: Room? = null
    private var gameFlow: GameFlow? = null
    private val humanPlayerId: String = "human_player"
    private var gameSessionId: Int = 0
    private var turnTimerJob: Job? = null
    private var turnTimerToken: Int = 0
    private var aiActionJob: Job? = null
    private var suppressFlowError: Boolean = false
    private val aiManager = AiManager()
    private val specialEffectsManager = SpecialEffectsManager()
    private val _specialEffectState = MutableStateFlow(SpecialEffectState(SpecialEffectType.Bomb))
    val specialEffectState: StateFlow<SpecialEffectState> = _specialEffectState.asStateFlow()

    init {
        initializeGame()
    }

    private fun updateSpecialEffectState() {
        val currentState = specialEffectsManager.effectState.value
        _specialEffectState.value = currentState
    }

    private fun initializeGame() {
        val newRoom = Room(
            id = "room_1",
            name = "单机房间",
            hostId = humanPlayerId,
            maxPlayers = 3
        )

        val humanPlayer = Player(humanPlayerId, "我", isAI = false)
        val aiPlayer1 = Player("ai_1", "电脑1", isAI = true)
        val aiPlayer2 = Player("ai_2", "电脑2", isAI = true)

        newRoom.addPlayer(humanPlayer)
        newRoom.addPlayer(aiPlayer1)
        newRoom.addPlayer(aiPlayer2)

        room = newRoom
        gameFlow = GameFlow(newRoom, createGameCallback())

        // 创建AI玩家
        aiManager.createAiPlayer(aiPlayer1)
        aiManager.createAiPlayer(aiPlayer2)

        updateUiState {
            it.copy(
                players = newRoom.players.map { p ->
                    PlayerUiState(
                        id = p.id,
                        name = p.name,
                        role = p.role,
                        handSize = p.handSize,
                        isOnline = p.isOnline
                    )
                },
                roomState = newRoom.state
            )
        }
    }

    fun startGame() {
        gameSessionId += 1
        val sessionId = gameSessionId
        stopHumanTurnTimer(resetSeconds = false)
        aiActionJob?.cancel()
        aiActionJob = null

        updateUiState {
            it.copy(
                selectedCards = emptySet(),
                currentPlayerId = null,
                playerCards = emptyList(),
                bottomCards = emptyList(),
                multiplier = 1,
                roomState = RoomState.Waiting,
                isPlayTurn = false,
                isBidTurn = false,
                currentBid = 0,
                lastPlayedCards = null,
                lastPlayedBy = null,
                lastPlayedPattern = null,
                gameResult = null,
                errorMessage = null,
                feedbackMessage = null,
                feedbackId = it.feedbackId + 1,
                turnSecondsRemaining = TURN_TIMEOUT_SECONDS
            )
        }

        viewModelScope.launch {
            if (sessionId == gameSessionId) {
                gameFlow?.startGame()
            }
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
        GameAudio.cardSelect()
    }

    fun playCards() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        val latestGameState = gameFlow?.getState()
        if (
            latestGameState != null &&
            (latestGameState.state != RoomState.Playing || latestGameState.currentPlayerId != humanPlayerId)
        ) {
            updateUiState { it.copy(isPlayTurn = false) }
            showFeedback("还没轮到你出牌")
            return
        }

        val selectedCards = state.playerCards.filter { it.id in state.selectedCards }
        if (selectedCards.isEmpty()) {
            showFeedback("请先选择要出的牌")
            return
        }

        val previousPattern = latestGameState?.activePreviousPatternFor(humanPlayerId)
            ?: if (latestGameState == null) state.lastPlayedPattern else null
        val validation = Validator.validatePlay(selectedCards, previousPattern)
        if (!validation.isValid) {
            showFeedback(validation.reason.ifBlank { "不符合出牌规则" })
            return
        }

        viewModelScope.launch {
            val success = gameFlow?.playerPlay(humanPlayerId, selectedCards) ?: false
            if (!success && _uiState.value.feedbackMessage == null) {
                showFeedback("无效的出牌")
            }
        }
    }

    fun pass() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        val gameState = gameFlow?.getState()
        if (gameState == null || gameState.state != RoomState.Playing || gameState.currentPlayerId != humanPlayerId) {
            updateUiState { it.copy(isPlayTurn = false) }
            showFeedback("还没轮到你出牌")
            return
        }

        val previousPattern = gameState.activePreviousPatternFor(humanPlayerId)
        if (previousPattern == null) {
            showFeedback("本轮需要先出牌，不能不出")
            return
        }

        viewModelScope.launch {
            val success = gameFlow?.playerPass(humanPlayerId) ?: false
            if (!success) {
                showFeedback("不能跳过")
            }
        }
    }

    fun bid(bid: Int) {
        val state = _uiState.value
        if (!state.isBidTurn) return

        viewModelScope.launch {
            val success = gameFlow?.playerBid(humanPlayerId, bid) ?: false
            if (!success) {
                showFeedback("叫分失败")
            }
        }
    }

    fun hintPlay() {
        val state = _uiState.value
        if (!state.isPlayTurn) {
            showFeedback("还没轮到你出牌")
            return
        }

        val flow = gameFlow ?: run {
            showFeedback("牌局尚未准备好")
            return
        }
        val gameState = flow.getState()
        if (gameState.state != RoomState.Playing || gameState.currentPlayerId != humanPlayerId) {
            showFeedback("还没轮到你出牌")
            return
        }

        val currentRoom = room ?: run {
            showFeedback("牌局尚未准备好")
            return
        }
        val hand = currentRoom.findPlayer(humanPlayerId)?.hand?.toList() ?: state.playerCards
        val isLandlord = gameState.landlordId == humanPlayerId
        val landlordHandSize = if (isLandlord) {
            hand.size
        } else {
            currentRoom.landlord?.handSize ?: 0
        }
        val previousPattern = gameState.activePreviousPatternFor(humanPlayerId)
        val suggestion = AiEvaluator.suggestBestPlay(
            hand = hand,
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

    fun bidPass() {
        bid(0)
    }

    fun clearError() {
        updateUiState { it.copy(errorMessage = null, feedbackMessage = null) }
    }

    fun stopSpecialEffect() {
        specialEffectsManager.stopEffect()
        updateSpecialEffectState()
    }

    private fun updateUiState(update: (GameUiState) -> GameUiState) {
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

    private fun startHumanTurnTimer(phase: HumanTurnPhase) {
        turnTimerJob?.cancel()
        val sessionId = gameSessionId
        val timerToken = ++turnTimerToken

        updateUiState { it.copy(turnSecondsRemaining = TURN_TIMEOUT_SECONDS) }

        turnTimerJob = viewModelScope.launch {
            for (remaining in (TURN_TIMEOUT_SECONDS - 1) downTo 0) {
                delay(1000)
                if (sessionId != gameSessionId || timerToken != turnTimerToken) return@launch

                val state = _uiState.value
                val stillMyTurn = when (phase) {
                    HumanTurnPhase.Bidding -> state.roomState == RoomState.Bidding && state.isBidTurn
                    HumanTurnPhase.Playing -> state.roomState == RoomState.Playing && state.isPlayTurn
                }
                if (!stillMyTurn) return@launch

                updateUiState { it.copy(turnSecondsRemaining = remaining) }
                if (remaining == 0) {
                    handleHumanTurnTimeout(phase, sessionId, timerToken)
                    return@launch
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

    private fun handleHumanTurnTimeout(
        phase: HumanTurnPhase,
        sessionId: Int,
        timerToken: Int
    ) {
        if (sessionId != gameSessionId || timerToken != turnTimerToken) return

        when (phase) {
            HumanTurnPhase.Bidding -> {
                showFeedback("倒计时结束，自动不叫")
                gameFlow?.playerBid(humanPlayerId, 0)
            }
            HumanTurnPhase.Playing -> handlePlayTimeout()
        }
    }

    private fun handlePlayTimeout() {
        val flow = gameFlow ?: return
        val gameState = flow.getState()
        if (gameState.state != RoomState.Playing || gameState.currentPlayerId != humanPlayerId) return

        val state = _uiState.value
        val previousPattern = gameState.activePreviousPatternFor(humanPlayerId)
        val selectedCards = state.playerCards.filter { it.id in state.selectedCards }
        val selectedPlay = selectedCards.takeIf {
            it.isNotEmpty() && Validator.validatePlay(it, previousPattern).isValid
        }

        when {
            selectedPlay != null -> {
                showFeedback("倒计时结束，自动出牌")
                flow.playerPlay(humanPlayerId, selectedPlay)
            }
            previousPattern != null && gameState.lastPlayedPlayerId != humanPlayerId -> {
                showFeedback("倒计时结束，自动不出")
                flow.playerPass(humanPlayerId)
            }
            else -> {
                val autoCards = chooseAutoPlayCards(gameState)
                if (autoCards.isEmpty()) {
                    showFeedback("倒计时结束，暂无可出牌")
                    return
                }

                showFeedback("倒计时结束，自动出 ${autoCards.toCardText()}")
                flow.playerPlay(humanPlayerId, autoCards)
            }
        }
    }

    private fun chooseAutoPlayCards(gameState: GameState): List<Card> {
        val currentRoom = room ?: return emptyList()
        val hand = currentRoom.findPlayer(humanPlayerId)?.hand?.toList().orEmpty()
        if (hand.isEmpty()) return emptyList()

        val previousPattern = gameState.activePreviousPatternFor(humanPlayerId)
        val isLandlord = gameState.landlordId == humanPlayerId
        val landlordHandSize = if (isLandlord) {
            hand.size
        } else {
            currentRoom.landlord?.handSize ?: 0
        }
        val suggestion = AiEvaluator.suggestBestPlay(
            hand = hand,
            lastPattern = previousPattern,
            isLandlord = isLandlord,
            landlordHandSize = landlordHandSize
        )

        if (!suggestion.isNullOrEmpty() && Validator.validatePlay(suggestion, previousPattern).isValid) {
            return suggestion
        }

        return if (previousPattern == null) {
            listOf(hand.sortedByGameOrder().first())
        } else {
            emptyList()
        }
    }

    private fun scheduleAiBidTurn(playerId: String) {
        val sessionId = gameSessionId
        aiActionJob?.cancel()
        aiActionJob = viewModelScope.launch {
            delay(1000)
            handleAiBidTurn(playerId, sessionId)
        }
    }

    private fun scheduleAiPlayTurn(playerId: String) {
        val sessionId = gameSessionId
        aiActionJob?.cancel()
        aiActionJob = viewModelScope.launch {
            delay(AI_PLAY_REVEAL_DELAY_MS)
            handleAiPlayTurn(playerId, sessionId)
        }
    }

    private fun handleAiBidTurn(playerId: String, sessionId: Int) {
        if (sessionId != gameSessionId) return

        val aiPlayer = aiManager.getAiPlayer(playerId) ?: return
        val currentBid = _uiState.value.currentBid
        val flow = gameFlow ?: return
        val gameState = flow.getState()
        if (gameState.state != RoomState.Bidding || gameState.currentPlayerId != playerId) return

        val suggestedBid = runCatching { aiPlayer.decideBid(flow, currentBid) }
            .getOrDefault(0)
            .coerceIn(0, 3)
        val bid = if (suggestedBid > currentBid) suggestedBid else 0
        val latestState = flow.getState()
        if (
            sessionId == gameSessionId &&
            latestState.state == RoomState.Bidding &&
            latestState.currentPlayerId == playerId
        ) {
            val success = runWithSuppressedFlowError { flow.playerBid(playerId, bid) }
            if (!success && flow.getState().currentPlayerId == playerId) {
                runWithSuppressedFlowError { flow.playerBid(playerId, 0) }
            }
        }
    }

    private fun handleAiPlayTurn(playerId: String, sessionId: Int) {
        if (sessionId != gameSessionId) return

        val aiPlayer = aiManager.getAiPlayer(playerId) ?: return
        val flow = gameFlow ?: return
        val gameState = flow.getState()
        if (gameState.state != RoomState.Playing || gameState.currentPlayerId != playerId) return

        val room = room ?: return

        val ai = room.findPlayer(playerId) ?: return
        val latestState = flow.getState()
        if (latestState.state != RoomState.Playing || latestState.currentPlayerId != playerId) return

        val isLandlord = latestState.landlordId == playerId
        val landlordHandSize = if (isLandlord) {
            ai.handSize
        } else {
            room.landlord?.handSize ?: 0
        }

        val previousPattern = latestState.activePreviousPatternFor(playerId)
        val suggestedCards = runCatching {
            aiPlayer.decidePlay(flow, previousPattern, isLandlord, landlordHandSize)
        }.getOrNull()
        val legalSuggestedCards = suggestedCards
            ?.takeIf { it.isNotEmpty() }
            ?.takeIf { ai.hasCards(it) }
            ?.takeIf { Validator.validatePlay(it, previousPattern).isValid }
        val fallbackCards = if (previousPattern == null) {
            ai.hand.sortedByGameOrder().firstOrNull()?.let { listOf(it) }
        } else {
            null
        }

        val success = when {
            legalSuggestedCards != null -> {
                runWithSuppressedFlowError { flow.playerPlay(playerId, legalSuggestedCards) }
            }
            previousPattern != null -> {
                runWithSuppressedFlowError { flow.playerPass(playerId) }
            }
            fallbackCards != null -> {
                runWithSuppressedFlowError { flow.playerPlay(playerId, fallbackCards) }
            }
            else -> false
        }

        if (!success) {
            syncGameState(flow.getState())
        }
    }

    private fun GameState.activePreviousPatternFor(playerId: String): HandPattern? {
        if (lastPlayedCards.isNullOrEmpty()) return null
        if (lastPlayedPlayerId == playerId) return null
        return lastPlayedPattern
    }

    private inline fun runWithSuppressedFlowError(action: () -> Boolean): Boolean {
        suppressFlowError = true
        return try {
            action()
        } finally {
            suppressFlowError = false
        }
    }

    private fun syncGameState(gameState: GameState) {
        updateUiState { state ->
            state.copy(
                players = gameState.players.map { ps ->
                    PlayerUiState(
                        id = ps.id,
                        name = ps.name,
                        role = ps.role,
                        handSize = ps.handSize,
                        isOnline = ps.isOnline
                    )
                },
                currentPlayerId = gameState.currentPlayerId,
                bottomCards = gameState.bottomCards,
                multiplier = gameState.multiplier,
                lastPlayedBy = if (gameState.lastPlayedCards.isNullOrEmpty()) null else gameState.lastPlayedPlayerId,
                lastPlayedPattern = gameState.lastPlayedPattern,
                lastPlayedCards = gameState.lastPlayedCards,
                roomState = gameState.state
            )
        }
    }

    private fun createGameCallback(): GameCallback {
        return object : GameCallback {
            override fun onGameStart(players: List<Player>, bottomCards: List<Card>) {
                val gameState = gameFlow?.getState()
                if (gameState != null) {
                    syncGameState(gameState)
                }
                updateUiState {
                    it.copy(
                        roomState = RoomState.Bidding,
                        bottomCards = bottomCards
                    )
                }
            }

            override fun onDealCards(playerId: String, cards: List<Card>) {
                if (playerId == humanPlayerId) {
                    updateUiState { it.copy(playerCards = cards) }
                }
            }

            override fun onBidStart(firstBidderId: String) {
                val isMyTurn = firstBidderId == humanPlayerId
                updateUiState {
                    it.copy(
                        isBidTurn = isMyTurn,
                        currentPlayerId = firstBidderId
                    )
                }

                if (isMyTurn) {
                    aiActionJob?.cancel()
                    aiActionJob = null
                    startHumanTurnTimer(HumanTurnPhase.Bidding)
                } else {
                    stopHumanTurnTimer()
                }

                // 如果第一个叫地主的是AI玩家，延迟后自动叫地主
                if (!isMyTurn) {
                    scheduleAiBidTurn(firstBidderId)
                }
            }

            override fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean) {
                val state = _uiState.value
                val newBid = if (bid > 0 && bid > state.currentBid) bid else state.currentBid
                updateUiState { it.copy(currentBid = newBid, isBidTurn = false) }
                GameAudio.playBid(bid, isPass)
                stopHumanTurnTimer()
            }

            override fun onLandlordDecided(landlordId: String, bottomCards: List<Card>, multiplier: Int) {
                val gameState = gameFlow?.getState()
                if (gameState != null) {
                    syncGameState(gameState)
                }
                updateUiState {
                    it.copy(
                        bottomCards = bottomCards,
                        multiplier = multiplier,
                        isBidTurn = false
                    )
                }
                stopHumanTurnTimer()
            }

            override fun onPlayStart(landlordId: String, firstPlayerId: String) {
                val isMyTurn = firstPlayerId == humanPlayerId
                val humanPlayer = room?.findPlayer(humanPlayerId)
                if (isMyTurn) {
                    aiActionJob?.cancel()
                    aiActionJob = null
                }
                updateUiState {
                    it.copy(
                        roomState = RoomState.Playing,
                        isPlayTurn = isMyTurn,
                        currentPlayerId = firstPlayerId,
                        playerCards = humanPlayer?.hand?.toList() ?: it.playerCards
                    )
                }

                if (isMyTurn) {
                    startHumanTurnTimer(HumanTurnPhase.Playing)
                } else {
                    stopHumanTurnTimer()
                }

                // 如果第一个出牌的是AI玩家，延迟后自动出牌
                if (!isMyTurn) {
                    scheduleAiPlayTurn(firstPlayerId)
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
                val isWinningPlay = !isPass && (
                    gameState
                        ?.players
                        ?.firstOrNull { it.id == playerId }
                        ?.handSize == 0
                )

                if (gameState != null) {
                    syncGameState(gameState)
                }

                updateUiState {
                    it.copy(
                        lastPlayedCards = if (!isPass) cards else it.lastPlayedCards,
                        lastPlayedBy = if (!isPass) playerId else it.lastPlayedBy,
                        lastPlayedPattern = if (!isPass) pattern else it.lastPlayedPattern,
                        isPlayTurn = false
                    )
                }

                if (isPass) {
                    GameAudio.playPass()
                } else if (!pattern.isBombOrRocket) {
                    GameAudio.playPattern(pattern, _uiState.value.multiplier)
                }

                // 如果是人类玩家出的牌，更新手牌并清除选中状态
                if (playerId == humanPlayerId) {
                    val humanPlayer = room?.findPlayer(humanPlayerId)
                    updateUiState {
                        it.copy(
                            playerCards = humanPlayer?.hand?.toList() ?: it.playerCards,
                            selectedCards = emptySet()
                        )
                    }
                }

                // 检查下一个玩家
                val nextGameState = gameFlow?.getState()
                if (!isWinningPlay && nextGameState != null && nextGameState.state == RoomState.Playing) {
                    val nextPlayerId = nextGameState.currentPlayerId
                    if (nextPlayerId != null && nextPlayerId != humanPlayerId) {
                        // 下一个是AI玩家，延迟后自动出牌
                        stopHumanTurnTimer()
                        scheduleAiPlayTurn(nextPlayerId)
                    } else if (nextPlayerId == humanPlayerId) {
                        aiActionJob?.cancel()
                        aiActionJob = null
                        updateUiState { it.copy(isPlayTurn = true) }
                        startHumanTurnTimer(HumanTurnPhase.Playing)
                    }
                } else {
                    stopHumanTurnTimer()
                }
            }

            override fun onMultiplierChanged(multiplier: Int, bombCount: Int) {
                updateUiState { it.copy(multiplier = multiplier) }
                if (bombCount > 0) {
                    _uiState.value.lastPlayedPattern?.let { GameAudio.playPattern(it, multiplier) }
                    when (_uiState.value.lastPlayedPattern?.type) {
                        PatternType.Rocket -> specialEffectsManager.triggerRocketEffect(multiplier)
                        else -> when {
                        bombCount == 1 -> specialEffectsManager.triggerBombEffect(multiplier, bombCount)
                        bombCount == 2 -> specialEffectsManager.triggerDoubleBombEffect(multiplier)
                        else -> specialEffectsManager.triggerMultiBombEffect(multiplier, bombCount)
                        }
                    }
                    updateSpecialEffectState()
                }
            }

            override fun onSpring(landlordId: String, isLandlordWin: Boolean) {
                // 春天事件 - 触发春天特效
                specialEffectsManager.triggerSpringEffect(_uiState.value.multiplier)
                updateSpecialEffectState()
                GameAudio.playSpring()
            }

            override fun onGameEnd(
                winnerId: String,
                winnerRole: PlayerRole,
                scores: Map<String, Int>,
                multiplier: Int
            ) {
                val humanSideWon = _uiState.value.players.find { it.id == humanPlayerId }?.role == winnerRole
                if (humanSideWon) {
                    GameAudio.playWin()
                } else {
                    GameAudio.playLose()
                }
                updateUiState {
                    it.copy(
                        roomState = RoomState.Finished,
                        isPlayTurn = false,
                        isBidTurn = false,
                        gameResult = GameResult(
                            winnerId = winnerId,
                            winnerRole = winnerRole,
                            scores = scores,
                            multiplier = multiplier
                        )
                    )
                }
                stopHumanTurnTimer()
            }

            override fun onError(message: String) {
                if (!suppressFlowError) {
                    showFeedback(message)
                }
            }

            override fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean) {
                // 暂不处理
            }
        }
    }
}
