package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.happy.poker.app.ui.components.*
import com.happy.poker.app.ui.theme.*
import com.happy.poker.app.viewmodel.MultiplayerGameViewModel
import com.happy.poker.app.viewmodel.PlayerUiState
import com.happy.poker.app.effects.SpecialEffectOverlay
import com.happy.poker.app.effects.SpecialEffectsManager
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.PatternType
import com.happy.poker.core.model.PlayerRole
import com.happy.poker.core.model.RoomState
import kotlinx.coroutines.delay

@Composable
fun MultiplayerGameScreen(
    viewModel: MultiplayerGameViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val specialEffectsManager = remember { SpecialEffectsManager() }
    val specialEffectState by specialEffectsManager.effectState.collectAsState()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var visibleFeedbackId by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.feedbackId, uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        val feedbackId = uiState.feedbackId
        if (message.isNullOrBlank()) {
            feedbackMessage = null
            return@LaunchedEffect
        }

        feedbackMessage = message
        visibleFeedbackId = feedbackId
        delay(1800)
        if (visibleFeedbackId == feedbackId) {
            feedbackMessage = null
            viewModel.clearError()
        }
    }
    
    // 监听倍数变化以触发特效
    LaunchedEffect(uiState.multiplier, uiState.lastPlayedPattern?.type) {
        if (uiState.multiplier > 1) {
            if (uiState.lastPlayedPattern?.type == PatternType.Rocket) {
                specialEffectsManager.triggerRocketEffect(uiState.multiplier)
            } else {
                specialEffectsManager.triggerBombEffect(uiState.multiplier, 1)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.room.state) {
            RoomState.Waiting -> WaitingRoomContent(
                room = uiState.room,
                isHost = uiState.room.isHost,
                onReadyClick = { viewModel.setReady(true) },
                onStartClick = { viewModel.startGame() },
                onBackClick = onBackClick
            )
            RoomState.Bidding, RoomState.Playing -> GameContent(
                playerCards = uiState.playerCards,
                selectedCards = uiState.selectedCards,
                onCardClick = { viewModel.selectCard(it.id) },
                onPlayClick = { viewModel.playCards() },
                onPassClick = { viewModel.pass() },
                onHintClick = { viewModel.hintPlay() },
                onBidClick = { viewModel.bid(it) },
                onBidPassClick = { viewModel.bidPass() },
                isPlayTurn = uiState.isPlayTurn,
                isBidTurn = uiState.isBidTurn,
                currentBid = uiState.currentBid,
                multiplier = uiState.multiplier,
                bottomCards = uiState.bottomCards,
                lastPlayedCards = uiState.lastPlayedCards,
                players = uiState.room.players,
                onBackClick = onBackClick
            )
            RoomState.Finished -> {
                uiState.gameResult?.let { result ->
                    ResultScreen(
                        winner = when (result.winnerRole) {
                            PlayerRole.Landlord -> "地主"
                            PlayerRole.Farmer -> "农民"
                            else -> "未知"
                        },
                        isLandlordWin = uiState.room.players.find { it.name == "我" }?.let { player ->
                            player.role == result.winnerRole || player.id == result.winnerId
                        } ?: true,
                        players = uiState.room.players.map { player ->
                            com.happy.poker.app.ui.screens.PlayerResult(
                                name = player.name,
                                role = when (player.role) {
                                    PlayerRole.Landlord -> "地主"
                                    PlayerRole.Farmer -> "农民"
                                    else -> "未知"
                                },
                                score = result.scores[player.id] ?: 0,
                                isWinner = player.id == result.winnerId
                            )
                        },
                        multiplier = result.multiplier,
                        onBackToHomeClick = onBackClick,
                        onPlayAgainClick = { viewModel.startGame() }
                    )
                }
            }
        }
        
        // 特效覆盖层
        SpecialEffectOverlay(
            effectState = specialEffectState,
            onEffectComplete = { specialEffectsManager.stopEffect() }
        )

        GameFeedbackToast(
            message = feedbackMessage,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 118.dp)
                .zIndex(20f)
        )
    }
}

@Composable
fun WaitingRoomContent(
    room: com.happy.poker.app.viewmodel.RoomUiState,
    isHost: Boolean,
    onReadyClick: () -> Unit = {},
    onStartClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TableGradientStart,
                        TableGradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 房间信息
            Text(
                text = room.roomName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "等待玩家加入...",
                fontSize = 18.sp,
                color = TextWhite,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // 玩家列表
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PlayerAreaBackground
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    room.players.forEach { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = player.name,
                                color = TextWhite,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (player.id == room.players.firstOrNull()?.id) "房主" else "玩家",
                                color = TextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    // 显示空位
                    repeat(room.maxPlayers - room.players.size) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "等待加入...",
                                color = TextGray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "空位",
                                color = TextGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 返回按钮
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .width(120.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite
                    )
                ) {
                    Text(
                        text = "返回",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (isHost) {
                    // 开始游戏按钮（房主）
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier
                            .width(120.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold500,
                            contentColor = CardBlack
                        ),
                        enabled = room.players.size >= 2
                    ) {
                        Text(
                            text = "开始游戏",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // 准备按钮（普通玩家）
                    Button(
                        onClick = onReadyClick,
                        modifier = Modifier
                            .width(120.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green600,
                            contentColor = TextWhite
                        )
                    ) {
                        Text(
                            text = "准备",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameContent(
    playerCards: List<GameCard> = emptyList(),
    selectedCards: Set<String> = emptySet(),
    onCardClick: (GameCard) -> Unit = {},
    onPlayClick: () -> Unit = {},
    onPassClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    onBidClick: (Int) -> Unit = {},
    onBidPassClick: () -> Unit = {},
    isPlayTurn: Boolean = false,
    isBidTurn: Boolean = false,
    currentBid: Int = 0,
    multiplier: Int = 1,
    bottomCards: List<GameCard> = emptyList(),
    lastPlayedCards: List<GameCard>? = null,
    players: List<PlayerUiState> = emptyList(),
    onBackClick: () -> Unit = {}
) {
    GameScreenContent(
        playerCards = playerCards,
        selectedCards = selectedCards,
        onCardClick = onCardClick,
        onPlayClick = onPlayClick,
        onPassClick = onPassClick,
        onHintClick = onHintClick,
        onBidClick = onBidClick,
        onBidPassClick = onBidPassClick,
        isPlayTurn = isPlayTurn,
        isBidTurn = isBidTurn,
        currentBid = currentBid,
        multiplier = multiplier,
        bottomCards = bottomCards,
        players = players,
        lastPlayedCards = lastPlayedCards,
        onBackClick = onBackClick
    )
}
