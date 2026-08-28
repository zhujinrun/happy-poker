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
import com.happy.poker.app.ui.components.*
import com.happy.poker.app.ui.theme.*
import com.happy.poker.app.viewmodel.GameViewModel
import com.happy.poker.app.viewmodel.PlayerUiState
import com.happy.poker.app.effects.SpecialEffectOverlay
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.PlayerRole
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val specialEffectState by viewModel.specialEffectState.collectAsState()
    
    // 显示错误信息
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // 显示错误提示
            viewModel.clearError()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GameScreenContent(
            playerCards = uiState.playerCards,
            selectedCards = uiState.selectedCards,
            onCardClick = { viewModel.selectCard(it.id) },
            onPlayClick = { viewModel.playCards() },
            onPassClick = { viewModel.pass() },
            onHintClick = { /* TODO: 提示功能 */ },
            onBidClick = { viewModel.bid(it) },
            onBidPassClick = { viewModel.bidPass() },
            isPlayTurn = uiState.isPlayTurn,
            isBidTurn = uiState.isBidTurn,
            currentBid = uiState.currentBid,
            multiplier = uiState.multiplier,
            players = uiState.players,
            currentPlayerId = uiState.currentPlayerId,
            onBackClick = onBackClick
        )
        
        // 特效覆盖层
        SpecialEffectOverlay(
            effectState = specialEffectState,
            onEffectComplete = { viewModel.stopSpecialEffect() }
        )
    }
}

@Composable
fun GameScreenContent(
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
    players: List<PlayerUiState> = emptyList(),
    currentPlayerId: String? = null,
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
        // 牌桌
        GameTable {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部信息区
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    Button(
                        onClick = onBackClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = "返回",
                            color = TextWhite
                        )
                    }
                    
                    // 游戏信息
                    GameInfo(
                        multiplier = multiplier,
                        bottomCards = 3
                    )
                }
                
                // 左侧玩家
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧玩家信息（玩家1）
                    if (players.size > 1) {
                        val player1 = players[1]
                        PlayerInfo(
                            playerName = player1.name,
                            cardCount = player1.handSize,
                            role = when (player1.role) {
                                PlayerRole.Landlord -> "地主"
                                PlayerRole.Farmer -> "农民"
                                else -> ""
                            },
                            isOnline = player1.isOnline,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 中间区域（出牌区）
                    CenterArea {
                        // 这里可以显示其他玩家出的牌
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 右侧玩家信息（玩家2）
                    if (players.size > 2) {
                        val player2 = players[2]
                        PlayerInfo(
                            playerName = player2.name,
                            cardCount = player2.handSize,
                            role = when (player2.role) {
                                PlayerRole.Landlord -> "地主"
                                PlayerRole.Farmer -> "农民"
                                else -> ""
                            },
                            isOnline = player2.isOnline,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
                
                // 底部区域（自己的手牌和操作）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 叫地主面板（如果在叫地主阶段）
                    if (isBidTurn) {
                        BidPanel(
                            currentBid = currentBid,
                            onBidClick = onBidClick,
                            onPassClick = onBidPassClick
                        )
                    }
                    
                    // 操作面板（如果在出牌阶段）
                    if (isPlayTurn) {
                        PlayPanel(
                            onPlayClick = onPlayClick,
                            onPassClick = onPassClick,
                            onHintClick = onHintClick,
                            isPlayTurn = isPlayTurn,
                            canPass = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 自己的手牌
                    HandCards(
                        cards = playerCards,
                        selectedCards = selectedCards,
                        onCardClick = onCardClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    // 自己的玩家信息
                    val humanPlayer = players.find { it.id == "human_player" }
                    PlayerInfo(
                        playerName = humanPlayer?.name ?: "我",
                        cardCount = playerCards.size,
                        role = when (humanPlayer?.role) {
                            PlayerRole.Landlord -> "地主"
                            PlayerRole.Farmer -> "农民"
                            else -> ""
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreenPreview() {
    val sampleCards = listOf(
        GameCard(Rank.Three, Suit.Spades),
        GameCard(Rank.Four, Suit.Hearts),
        GameCard(Rank.Five, Suit.Diamonds),
        GameCard(Rank.Six, Suit.Clubs),
        GameCard(Rank.Seven, Suit.Spades),
        GameCard(Rank.Eight, Suit.Hearts),
        GameCard(Rank.Nine, Suit.Diamonds),
        GameCard(Rank.Ten, Suit.Clubs),
        GameCard(Rank.Jack, Suit.Spades),
        GameCard(Rank.Queen, Suit.Hearts),
        GameCard(Rank.King, Suit.Diamonds),
        GameCard(Rank.Ace, Suit.Clubs),
        GameCard(Rank.Two, Suit.Spades),
        GameCard(Rank.SmallJoker, Suit.Joker),
        GameCard(Rank.BigJoker, Suit.Joker)
    )
    
    val samplePlayers = listOf(
        PlayerUiState("human_player", "我", PlayerRole.Unknown, 15),
        PlayerUiState("ai_1", "电脑1", PlayerRole.Unknown, 17),
        PlayerUiState("ai_2", "电脑2", PlayerRole.Unknown, 17)
    )
    
    HappyPokerTheme {
        GameScreenContent(
            playerCards = sampleCards,
            selectedCards = setOf("4♥", "5♦"),
            isPlayTurn = true,
            multiplier = 2,
            players = samplePlayers
        )
    }
}
