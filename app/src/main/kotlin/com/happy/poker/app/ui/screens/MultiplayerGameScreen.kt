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
import com.happy.poker.app.viewmodel.MultiplayerGameViewModel
import com.happy.poker.app.viewmodel.PlayerUiState
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.PlayerRole
import com.happy.poker.core.model.RoomState

@Composable
fun MultiplayerGameScreen(
    viewModel: MultiplayerGameViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 显示错误信息
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // 显示错误提示
            viewModel.clearError()
        }
    }
    
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
            onBidClick = { viewModel.bid(it) },
            onBidPassClick = { viewModel.bidPass() },
            isPlayTurn = uiState.isPlayTurn,
            isBidTurn = uiState.isBidTurn,
            currentBid = uiState.currentBid,
            multiplier = uiState.multiplier,
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
                    onPlayAgainClick = { /* TODO: 再来一局 */ }
                )
            }
        }
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
    onBidClick: (Int) -> Unit = {},
    onBidPassClick: () -> Unit = {},
    isPlayTurn: Boolean = false,
    isBidTurn: Boolean = false,
    currentBid: Int = 0,
    multiplier: Int = 1,
    players: List<PlayerUiState> = emptyList(),
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
                    
                    GameInfo(
                        multiplier = multiplier,
                        bottomCards = 3
                    )
                }
                
                // 玩家区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧玩家
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
                    
                    CenterArea {
                        // 出牌区
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 右侧玩家
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
                
                // 底部区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isBidTurn) {
                        BidPanel(
                            currentBid = currentBid,
                            onBidClick = onBidClick,
                            onPassClick = onBidPassClick
                        )
                    }
                    
                    if (isPlayTurn) {
                        PlayPanel(
                            onPlayClick = onPlayClick,
                            onPassClick = onPassClick,
                            onHintClick = { },
                            isPlayTurn = isPlayTurn,
                            canPass = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HandCards(
                        cards = playerCards,
                        selectedCards = selectedCards,
                        onCardClick = onCardClick,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    val humanPlayer = players.find { it.id.contains("human_player") }
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
