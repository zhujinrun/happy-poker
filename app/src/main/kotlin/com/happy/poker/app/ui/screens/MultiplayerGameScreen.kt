package com.happy.poker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.happy.poker.app.R
import com.happy.poker.app.ui.components.*
import com.happy.poker.app.settings.AppSettingsManager
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
import androidx.compose.ui.platform.LocalContext

@Composable
fun MultiplayerGameScreen(
    viewModel: MultiplayerGameViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val appSettingsManager = remember { AppSettingsManager(context) }
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
                onBackClick = onBackClick,
                humanAvatarKey = appSettingsManager.getAvatarKey()
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
                turnSecondsRemaining = uiState.turnSecondsRemaining,
                multiplier = uiState.multiplier,
                bottomCards = uiState.bottomCards,
                lastPlayedCards = uiState.lastPlayedCards,
                currentPlayerId = uiState.currentPlayerId,
                lastPlayedBy = uiState.lastPlayedBy,
                players = uiState.room.players,
                humanAvatarKey = appSettingsManager.getAvatarKey(),
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
                        isLandlordWin = uiState.room.players.find {
                            it.id.startsWith("human_player_")
                        }?.let { player ->
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
    onBackClick: () -> Unit = {},
    humanAvatarKey: String = AppSettingsManager.DEFAULT_AVATAR
) {
    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp
            val seatCount = room.maxPlayers.coerceAtLeast(2)

            Column(modifier = Modifier.fillMaxSize()) {
                PokerLobbyHeader(
                    title = room.roomName,
                    subtitle = if (isHost) "你是房主，准备好后可以开局" else "等待房主开始游戏",
                    onBackClick = onBackClick,
                    trailing = {
                        PokerStatusPill(
                            text = room.state.displayName,
                            color = if (room.state == RoomState.Waiting) Gold500 else Green600
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = if (compact) 4.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
                ) {
                    PokerGlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "入座情况",
                                color = Gold500,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                room.players.take(seatCount).forEach { player ->
                                    WaitingSeat(
                                        player = player,
                                        isHostSeat = player.id == room.hostId,
                                        compact = compact,
                                        humanAvatarKey = humanAvatarKey
                                    )
                                }
                                repeat((seatCount - room.players.size).coerceAtLeast(0)) {
                                    EmptySeat(compact = compact)
                                }
                            }
                        }
                    }

                    PokerGlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "房间信息",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            RoomMetaRow(label = "房间", value = room.roomName)
                            RoomMetaRow(label = "人数", value = "${room.players.size}/${room.maxPlayers}")
                            RoomMetaRow(label = "身份", value = if (isHost) "房主" else "玩家")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PokerImageButton(
                            normalRes = R.drawable.btn_blue,
                            text = "返回",
                            onClick = onBackClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            fontSize = 15.sp
                        )

                        if (isHost) {
                            PokerImageButton(
                                normalRes = R.drawable.btn_orange,
                                text = "开始",
                                onClick = onStartClick,
                                enabled = room.players.size >= 2,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                textColor = CardBlack,
                                fontSize = 15.sp
                            )
                        } else {
                            PokerImageButton(
                                normalRes = R.drawable.btn_green,
                                text = "准备",
                                onClick = onReadyClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaitingSeat(
    player: PlayerUiState,
    isHostSeat: Boolean,
    compact: Boolean,
    humanAvatarKey: String
) {
    val avatarRes = if (player.id.startsWith("human_player")) {
        when (humanAvatarKey) {
            "daheng" -> R.drawable.avatar_daheng
            "luoli" -> R.drawable.avatar_luoli
            else -> R.drawable.avatar_yujie
        }
    } else if (player.id.hashCode() % 2 == 0) {
        R.drawable.avatar_daheng
    } else {
        R.drawable.avatar_luoli
    }
    val avatarSize = if (compact) 50.dp else 62.dp
    val seatWidth = if (compact) 84.dp else 96.dp

    Column(
        modifier = Modifier.width(seatWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f))
                .border(
                    width = if (isHostSeat) 2.dp else 1.dp,
                    color = if (isHostSeat) Gold500 else TextWhite.copy(alpha = 0.20f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = player.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 90.dp else 110.dp)
                    .offset(y = 2.dp),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth
            )
        }

        Text(
            text = player.name,
            color = TextWhite,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        PokerStatusPill(
            text = if (isHostSeat) "房主" else if (player.isOnline) "在线" else "离线",
            color = if (isHostSeat) Gold500 else if (player.isOnline) Green600 else ButtonDanger
        )
    }
}

@Composable
private fun EmptySeat(compact: Boolean) {
    Column(
        modifier = Modifier.width(if (compact) 84.dp else 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 50.dp else 62.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.20f))
                .border(1.dp, TextWhite.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "?",
                color = TextWhite.copy(alpha = 0.42f),
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "等待入座",
            color = TextGray,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Medium
        )
        PokerStatusPill(text = "空位", color = TextGray)
    }
}

@Composable
private fun RoomMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextWhite.copy(alpha = 0.66f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
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
    turnSecondsRemaining: Int = 30,
    multiplier: Int = 1,
    bottomCards: List<GameCard> = emptyList(),
    lastPlayedCards: List<GameCard>? = null,
    currentPlayerId: String? = null,
    lastPlayedBy: String? = null,
    players: List<PlayerUiState> = emptyList(),
    humanAvatarKey: String = AppSettingsManager.DEFAULT_AVATAR,
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
        turnSecondsRemaining = turnSecondsRemaining,
        multiplier = multiplier,
        bottomCards = bottomCards,
        players = players,
        currentPlayerId = currentPlayerId,
        lastPlayedCards = lastPlayedCards,
        lastPlayedBy = lastPlayedBy,
        humanAvatarKey = humanAvatarKey,
        onBackClick = onBackClick
    )
}
