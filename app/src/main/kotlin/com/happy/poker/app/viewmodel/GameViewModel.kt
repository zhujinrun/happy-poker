package com.happy.poker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.happy.poker.core.ai.AiManager
import com.happy.poker.core.ai.AiPlayer
import com.happy.poker.core.flow.GameCallback
import com.happy.poker.core.flow.GameFlow
import com.happy.poker.core.flow.GameState
import com.happy.poker.core.model.*
import com.happy.poker.core.rules.Validator
import com.happy.poker.app.effects.SpecialEffectsManager
import com.happy.poker.app.effects.SpecialEffectState
import com.happy.poker.app.effects.SpecialEffectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val lastPlayedPattern: HandPattern? = null,
    val gameResult: GameResult? = null,
    val errorMessage: String? = null
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

        startGame()
    }

    fun startGame() {
        viewModelScope.launch {
            gameFlow?.startGame()
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
            val success = gameFlow?.playerPlay(humanPlayerId, selectedCards) ?: false
            if (!success) {
                updateUiState { it.copy(errorMessage = "无效的出牌") }
            }
        }
    }

    fun pass() {
        val state = _uiState.value
        if (!state.isPlayTurn) return

        viewModelScope.launch {
            val success = gameFlow?.playerPass(humanPlayerId) ?: false
            if (!success) {
                updateUiState { it.copy(errorMessage = "不能跳过") }
            }
        }
    }

    fun bid(bid: Int) {
        val state = _uiState.value
        if (!state.isBidTurn) return

        viewModelScope.launch {
            val success = gameFlow?.playerBid(humanPlayerId, bid) ?: false
            if (!success) {
                updateUiState { it.copy(errorMessage = "叫分失败") }
            }
        }
    }

    fun bidPass() {
        bid(0)
    }

    fun clearError() {
        updateUiState { it.copy(errorMessage = null) }
    }

    fun stopSpecialEffect() {
        specialEffectsManager.stopEffect()
    }

    private fun updateUiState(update: (GameUiState) -> GameUiState) {
        _uiState.value = update(_uiState.value)
    }

    private fun handleAiBidTurn(playerId: String) {
        val aiPlayer = aiManager.getAiPlayer(playerId) ?: return
        val currentBid = _uiState.value.currentBid
        val gameState = gameFlow?.getState() ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val bid = aiPlayer.decideBid(gameFlow!!, currentBid)
            gameFlow?.playerBid(playerId, bid)
        }
    }

    private fun handleAiPlayTurn(playerId: String) {
        val aiPlayer = aiManager.getAiPlayer(playerId) ?: return
        val gameState = gameFlow?.getState() ?: return
        val room = room ?: return

        val isLandlord = gameState.landlordId == playerId
        val landlordHandSize = if (isLandlord) {
            room.findPlayer(playerId)?.handSize ?: 0
        } else {
            room.landlord?.handSize ?: 0
        }

        viewModelScope.launch(Dispatchers.Default) {
            aiPlayer.autoPlay(gameFlow!!, gameState.lastPlayedCards?.let {
                val result = Validator.identify(it)
                if (result.isValid) result.pattern else null
            }, isLandlord, landlordHandSize)
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
            }

            override fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean) {
                val state = _uiState.value
                val newBid = if (bid > 0 && bid > state.currentBid) bid else state.currentBid
                updateUiState { it.copy(currentBid = newBid, isBidTurn = false) }

                // 检查下一个是否是AI玩家
                val gameState = gameFlow?.getState()
                if (gameState != null) {
                    val nextPlayerId = gameState.currentPlayerId
                    if (nextPlayerId != null && nextPlayerId != humanPlayerId) {
                        // 下一个是AI玩家，延迟后自动叫地主
                        viewModelScope.launch {
                            delay(1000) // 延迟1秒
                            handleAiBidTurn(nextPlayerId)
                        }
                    } else if (nextPlayerId == humanPlayerId) {
                        updateUiState { it.copy(isBidTurn = true) }
                    }
                }
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
            }

            override fun onPlayStart(landlordId: String, firstPlayerId: String) {
                val isMyTurn = firstPlayerId == humanPlayerId
                val humanPlayer = room?.findPlayer(humanPlayerId)
                updateUiState {
                    it.copy(
                        roomState = RoomState.Playing,
                        isPlayTurn = isMyTurn,
                        currentPlayerId = firstPlayerId,
                        playerCards = humanPlayer?.hand?.toList() ?: it.playerCards
                    )
                }

                // 如果第一个出牌的是AI玩家，延迟后自动出牌
                if (!isMyTurn) {
                    viewModelScope.launch {
                        delay(1000) // 延迟1秒
                        handleAiPlayTurn(firstPlayerId)
                    }
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
                    syncGameState(gameState)
                }

                updateUiState {
                    it.copy(
                        lastPlayedCards = if (!isPass) cards else it.lastPlayedCards,
                        lastPlayedPattern = if (!isPass) pattern else it.lastPlayedPattern,
                        isPlayTurn = false
                    )
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
                if (nextGameState != null && nextGameState.state == RoomState.Playing) {
                    val nextPlayerId = nextGameState.currentPlayerId
                    if (nextPlayerId != null && nextPlayerId != humanPlayerId) {
                        // 下一个是AI玩家，延迟后自动出牌
                        viewModelScope.launch {
                            delay(1000) // 延迟1秒
                            handleAiPlayTurn(nextPlayerId)
                        }
                    } else if (nextPlayerId == humanPlayerId) {
                        updateUiState { it.copy(isPlayTurn = true) }
                    }
                }
            }

            override fun onMultiplierChanged(multiplier: Int, bombCount: Int) {
                updateUiState { it.copy(multiplier = multiplier) }
                // 触发炸弹特效
                if (bombCount > 0) {
                    when {
                        bombCount == 1 -> specialEffectsManager.triggerBombEffect(multiplier, bombCount)
                        bombCount == 2 -> specialEffectsManager.triggerDoubleBombEffect(multiplier)
                        else -> specialEffectsManager.triggerMultiBombEffect(multiplier, bombCount)
                    }
                    updateSpecialEffectState()
                }
            }

            override fun onSpring(landlordId: String, isLandlordWin: Boolean) {
                // 春天事件 - 触发春天特效
                specialEffectsManager.triggerSpringEffect(_uiState.value.multiplier)
                updateSpecialEffectState()
            }

            override fun onGameEnd(
                winnerId: String,
                winnerRole: PlayerRole,
                scores: Map<String, Int>,
                multiplier: Int
            ) {
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
            }

            override fun onError(message: String) {
                updateUiState { it.copy(errorMessage = message) }
            }

            override fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean) {
                // 暂不处理
            }
        }
    }
}
