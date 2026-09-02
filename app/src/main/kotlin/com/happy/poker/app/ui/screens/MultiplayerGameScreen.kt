package com.happy.poker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.happy.poker.app.progress.PlayerProgressManager
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
    val progressManager = remember { PlayerProgressManager(context) }
    val specialEffectsManager = remember { SpecialEffectsManager() }
    val specialEffectState by specialEffectsManager.effectState.collectAsState()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var visibleFeedbackId by remember { mutableStateOf(0) }
    var previousSpecialEffectMultiplier by remember { mutableIntStateOf(uiState.multiplier) }
    val humanBeanBalance = progressManager.getBeanBalance()

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.cancelPendingGameEndReveal()
        }
    }

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
        val patternType = uiState.lastPlayedPattern?.type
        val multiplierIncreased = uiState.multiplier > previousSpecialEffectMultiplier
        if (multiplierIncreased) {
            when (patternType) {
                PatternType.Rocket -> {
                    specialEffectsManager.triggerRocketEffect(uiState.multiplier)
                }
                PatternType.Bomb -> {
                    specialEffectsManager.triggerBombEffect(uiState.multiplier, 1)
                }
                else -> Unit
            }
        }
        previousSpecialEffectMultiplier = uiState.multiplier
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
                turnSecondsRemaining = uiState.turnSecondsRemaining,
                multiplier = uiState.multiplier,
                bottomCards = uiState.bottomCards,
                lastPlayedCards = uiState.lastPlayedCards,
                currentPlayerId = uiState.currentPlayerId,
                lastPlayedBy = uiState.lastPlayedBy,
                players = uiState.room.players,
                beanBalance = humanBeanBalance,
                humanAvatarKey = appSettingsManager.getAvatarKey(),
                localPlayerId = viewModel.localPlayerId,
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
                            it.id == viewModel.localPlayerId
                        }?.let { player ->
                            player.role == result.winnerRole || player.id == result.winnerId
                        } ?: true,
                        players = uiState.room.players.map { player ->
                            com.happy.poker.app.ui.screens.PlayerResult(
                                name = waitingSeatDisplayName(player, uiState.room.players),
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
                        beanDelta = result.beanDelta,
                        beanBalance = result.beanBalance,
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
    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp
            val seatCount = room.maxPlayers.coerceAtLeast(2)
            val horizontalInset = if (compact) 18.dp else 24.dp

            PokerBackButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = horizontalInset, top = if (compact) 2.dp else 8.dp),
                contentDescription = "返回房间列表"
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (compact) 8.dp else 12.dp)
                    .widthIn(max = 430.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = room.roomName.ifBlank { "欢乐房间" },
                    color = Gold500,
                    fontSize = if (compact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isHost) "房主等待开局" else "等待房主开局",
                    color = TextWhite.copy(alpha = 0.72f),
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            PokerStatusPill(
                text = room.state.displayName,
                color = if (room.state == RoomState.Waiting) Gold500 else Green600,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = horizontalInset, top = if (compact) 13.dp else 18.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = if (compact) (-4).dp else (-10).dp)
                    .widthIn(max = if (compact) 600.dp else 720.dp)
                    .fillMaxWidth()
                    .height(if (compact) 150.dp else 178.dp)
                    .padding(horizontal = if (compact) 34.dp else 46.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 88.dp else 104.dp)
                        .clip(RoundedCornerShape(42.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF163B76).copy(alpha = 0.22f),
                                    Color(0xFF07132A).copy(alpha = 0.18f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(42.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    room.players.take(seatCount).forEach { player ->
                        WaitingSeat(
                            player = player,
                            displayName = waitingSeatDisplayName(player, room.players),
                            isHostSeat = player.id == room.hostId,
                            compact = compact
                        )
                    }
                    repeat((seatCount - room.players.size).coerceAtLeast(0)) {
                        EmptySeat(compact = compact)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (compact) 18.dp else 28.dp)
                    .widthIn(max = 388.dp)
                    .fillMaxWidth(0.52f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PokerImageButton(
                    normalRes = R.drawable.btn_green,
                    text = "退出",
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 42.dp else 48.dp),
                    fontSize = if (compact) 15.sp else 16.sp,
                    contentDescription = "返回房间列表"
                )

                if (isHost) {
                    PokerImageButton(
                        normalRes = R.drawable.btn_orange,
                        text = "开始游戏",
                        onClick = onStartClick,
                        enabled = room.players.size >= 2,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 42.dp else 48.dp),
                        textColor = CardBlack,
                        fontSize = if (compact) 15.sp else 16.sp
                    )
                } else {
                    PokerImageButton(
                        normalRes = R.drawable.btn_orange,
                        text = "准备",
                        onClick = onReadyClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 42.dp else 48.dp),
                        textColor = CardBlack,
                        fontSize = if (compact) 15.sp else 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingSeat(
    player: PlayerUiState,
    displayName: String,
    isHostSeat: Boolean,
    compact: Boolean
) {
    val avatarRes = waitingAvatarResource(player)
    val avatarSize = if (compact) 58.dp else 70.dp
    val seatWidth = if (compact) 96.dp else 116.dp

    Column(
        modifier = Modifier.width(seatWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(Color(0xFFEAF6FF).copy(alpha = 0.94f))
                .border(
                    width = if (isHostSeat) 3.dp else 2.dp,
                    color = if (isHostSeat) Gold500 else Color.White.copy(alpha = 0.86f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 106.dp else 126.dp)
                    .offset(y = if (avatarRes == R.drawable.avatar_daheng) (-4).dp else 1.dp),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth
            )
        }

        Text(
            text = displayName,
            color = TextWhite,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        BeanAmountText(beanBalance = player.beanBalance, compact = true)
    }
}

private fun waitingSeatDisplayName(player: PlayerUiState, players: List<PlayerUiState>): String {
    val name = player.name.ifBlank { "牌友" }
    val sameNamePlayers = players.filter { it.name.ifBlank { "牌友" } == name }
    if (sameNamePlayers.size <= 1) return name

    val seatIndex = sameNamePlayers.indexOfFirst { it.id == player.id }.takeIf { it >= 0 } ?: 0
    return "$name${seatIndex + 1}"
}

private fun waitingAvatarResource(player: PlayerUiState): Int {
    return when (player.avatarKey) {
        "daheng" -> R.drawable.avatar_daheng
        "luoli" -> R.drawable.avatar_luoli
        "yujie" -> R.drawable.avatar_yujie
        else -> if ((player.id.hashCode() and 1) == 0) {
            R.drawable.avatar_luoli
        } else {
            R.drawable.avatar_yujie
        }
    }
}

@Composable
private fun EmptySeat(compact: Boolean) {
    Column(
        modifier = Modifier.width(if (compact) 96.dp else 116.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 58.dp else 70.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.20f))
                .border(2.dp, TextWhite.copy(alpha = 0.24f), CircleShape),
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
    beanBalance: Int = PlayerProgressManager.INITIAL_BEAN_BALANCE,
    humanAvatarKey: String = AppSettingsManager.DEFAULT_AVATAR,
    localPlayerId: String? = null,
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
        beanBalance = beanBalance,
        humanAvatarKey = humanAvatarKey,
        localPlayerId = localPlayerId,
        onBackClick = onBackClick
    )
}
