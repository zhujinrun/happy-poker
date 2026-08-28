package com.happy.poker.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.happy.poker.app.R
import com.happy.poker.app.ui.components.*
import com.happy.poker.app.ui.theme.*
import com.happy.poker.app.viewmodel.GameViewModel
import com.happy.poker.app.viewmodel.PlayerUiState
import com.happy.poker.app.effects.SpecialEffectOverlay
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.PlayerRole
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.RoomState
import com.happy.poker.core.model.Suit
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val specialEffectState by viewModel.specialEffectState.collectAsState()
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
    
    Box(modifier = Modifier.fillMaxSize()) {
        // 游戏背景图片
        Image(
            painter = painterResource(id = R.drawable.game_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        val gameResult = uiState.gameResult
        if (uiState.roomState == RoomState.Finished && gameResult != null) {
            val humanPlayer = uiState.players.find {
                it.id == "human_player" || it.id.startsWith("human_player_") || it.name == "我"
            }
            val didHumanSideWin = humanPlayer?.role == gameResult.winnerRole ||
                humanPlayer?.id == gameResult.winnerId

            ResultScreen(
                winner = when (gameResult.winnerRole) {
                    PlayerRole.Landlord -> "地主"
                    PlayerRole.Farmer -> "农民"
                    else -> "未知"
                },
                isLandlordWin = didHumanSideWin,
                players = uiState.players.map { player ->
                    PlayerResult(
                        name = player.name,
                        role = when (player.role) {
                            PlayerRole.Landlord -> "地主"
                            PlayerRole.Farmer -> "农民"
                            else -> "未知"
                        },
                        score = gameResult.scores[player.id] ?: 0,
                        isWinner = player.role == gameResult.winnerRole
                    )
                },
                multiplier = gameResult.multiplier,
                onBackToHomeClick = onBackClick,
                onPlayAgainClick = { viewModel.startGame() }
            )
        } else {
            GameScreenContent(
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
                players = uiState.players,
                currentPlayerId = uiState.currentPlayerId,
                lastPlayedCards = uiState.lastPlayedCards,
                lastPlayedBy = uiState.lastPlayedBy,
                onBackClick = onBackClick
            )
        }
        
        // 特效覆盖层
        SpecialEffectOverlay(
            effectState = specialEffectState,
            onEffectComplete = { viewModel.stopSpecialEffect() }
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
fun GameFeedbackToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    var lastNonBlankMessage by remember { mutableStateOf<String?>(null) }
    val currentMessage = message?.takeIf { it.isNotBlank() }
    val displayMessage = currentMessage ?: lastNonBlankMessage

    SideEffect {
        if (currentMessage != null) {
            lastNonBlankMessage = currentMessage
        }
    }

    AnimatedVisibility(
        visible = currentMessage != null,
        enter = fadeIn(animationSpec = tween(120)) + slideInVertically { it / 3 },
        exit = fadeOut(animationSpec = tween(160)) + slideOutVertically { it / 3 },
        modifier = modifier
    ) {
        val text = displayMessage.orEmpty()
        val isHint = text.startsWith("提示")
        val isPlayError = listOf("选择", "牌型", "压过", "不符合", "失败", "无效").any { text.contains(it) }
        val accentColor = if (isPlayError) ButtonDanger else Gold500
        val title = when {
            isHint -> "提示"
            isPlayError -> "出牌失败"
            else -> "提示"
        }
        val body = text.removePrefix("提示：")
        val shape = RoundedCornerShape(24.dp)

        Row(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 520.dp)
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.84f),
                            Color(0xFF24180A).copy(alpha = 0.86f)
                        )
                    )
                )
                .border(1.dp, accentColor.copy(alpha = 0.72f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlayError) "!" else "?",
                    color = CardBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = title,
                color = accentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color.White.copy(alpha = 0.18f))
            )

            Text(
                text = body,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
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
    turnSecondsRemaining: Int = 30,
    multiplier: Int = 1,
    bottomCards: List<GameCard> = emptyList(),
    players: List<PlayerUiState> = emptyList(),
    currentPlayerId: String? = null,
    lastPlayedCards: List<GameCard>? = null,
    lastPlayedBy: String? = null,
    onBackClick: () -> Unit = {}
) {
    val humanPlayer = players.find {
        it.id == "human_player" || it.id.startsWith("human_player_") || it.name == "我"
    } ?: PlayerUiState("human_player", "我", PlayerRole.Unknown, playerCards.size)
    val playedByName = players.find { it.id == lastPlayedBy }?.name.orEmpty()

    GameTable {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tableMaxWidth = maxWidth
            val compactHeight = maxHeight < 520.dp
            val landscape = maxWidth > maxHeight
            val edgePadding = if (compactHeight) 8.dp else 10.dp
            val centerWidthFraction = when {
                compactHeight && landscape -> 0.66f
                landscape -> 0.74f
                else -> 0.94f
            }
            val centerMaxWidth = if (compactHeight) 610.dp else 720.dp
            val opponentYOffset = if (compactHeight) (-30).dp else (-62).dp
            val actionBottom = if (compactHeight) 160.dp else 184.dp
            val centerYOffset = if (compactHeight) (-78).dp else (-44).dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edgePadding, vertical = 6.dp)
            ) {
                PokerTopHud(
                    multiplier = multiplier,
                    currentBid = currentBid,
                    bottomCards = bottomCards,
                    players = players,
                    onBackClick = onBackClick,
                    compact = compactHeight,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .offset(y = if (compactHeight) (-12).dp else (-8).dp)
                        .zIndex(4f)
                )

                OpponentStrip(
                    players = players,
                    currentPlayerId = currentPlayerId,
                    compact = compactHeight,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = opponentYOffset)
                        .fillMaxWidth()
                        .zIndex(2f)
                )

                CenterPlayedArea(
                    lastPlayedCards = lastPlayedCards,
                    playedByName = playedByName,
                    isBidTurn = isBidTurn,
                    isPlayTurn = isPlayTurn,
                    compact = compactHeight,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = centerYOffset)
                        .fillMaxWidth(centerWidthFraction)
                        .widthIn(max = centerMaxWidth)
                        .zIndex(3f)
                )

                BattleActionStrip(
                    isBidTurn = isBidTurn,
                    isPlayTurn = isPlayTurn,
                    currentBid = currentBid,
                    turnSecondsRemaining = turnSecondsRemaining,
                    onBidClick = onBidClick,
                    onBidPassClick = onBidPassClick,
                    onPlayClick = onPlayClick,
                    onPassClick = onPassClick,
                    onHintClick = onHintClick,
                    compact = compactHeight,
                    showLabel = tableMaxWidth >= 520.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = actionBottom)
                        .zIndex(6f)
                )

                LocalPlayerDock(
                    player = humanPlayer.copy(handSize = playerCards.size),
                    playerCards = playerCards,
                    selectedCards = selectedCards,
                    onCardClick = onCardClick,
                    compact = compactHeight,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .zIndex(5f)
                )
            }
        }
    }
}

@Composable
private fun PokerTopHud(
    multiplier: Int,
    currentBid: Int,
    bottomCards: List<GameCard>,
    players: List<PlayerUiState>,
    onBackClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val sideWidth = if (compact) 112.dp else 132.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 8.dp else 14.dp, vertical = if (compact) 0.dp else 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.width(sideWidth)) {
            PokerNavChip(
                text = "返回",
                onClick = onBackClick,
                modifier = Modifier.width(if (compact) 64.dp else 72.dp)
            )
        }

        BottomCardsTray(
            cards = bottomCards,
            isRevealed = players.any { it.role == PlayerRole.Landlord },
            compact = compact,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.width(sideWidth),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp, Alignment.End)
        ) {
            HudMetric(label = "倍数", value = "${multiplier}x", valueColor = Gold500, compact = compact)
            HudMetric(
                label = "叫分",
                value = currentBid.takeIf { it > 0 }?.toString() ?: "-",
                valueColor = TextWhite,
                compact = compact
            )
        }
    }
}

@Composable
private fun PokerNavChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(1.dp, Gold500.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HudMetric(
    label: String,
    value: String,
    valueColor: Color,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(if (compact) 52.dp else 62.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .padding(vertical = if (compact) 4.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = TextGray, fontSize = if (compact) 9.sp else 10.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun BottomCardsTray(
    cards: List<GameCard>,
    isRevealed: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "底牌",
            color = TextGray,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) (-6).dp else (-7).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRevealed && cards.isNotEmpty()) {
                cards.take(3).forEach { card ->
                    SmallPlayingCard(
                        card = card,
                        modifier = Modifier
                            .width(if (compact) 34.dp else 40.dp)
                            .height(if (compact) 48.dp else 56.dp)
                    )
                }
            } else {
                repeat(3) {
                    MiniCardBack(compact = compact)
                }
            }
        }
    }
}

@Composable
private fun MiniCardBack(compact: Boolean = false) {
    Card(
        modifier = Modifier
            .width(if (compact) 34.dp else 40.dp)
            .height(if (compact) 48.dp else 56.dp),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.card_back_new),
            contentDescription = "底牌牌背",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun OpponentStrip(
    players: List<PlayerUiState>,
    currentPlayerId: String?,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val leftPlayer = players.getOrNull(1)
    val rightPlayer = players.getOrNull(2)
    val badgeWidth = if (compact) 132.dp else 148.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        PlayerBadgeCard(
            player = leftPlayer,
            avatarRes = R.drawable.avatar_luoli,
            isCurrentPlayer = leftPlayer?.id == currentPlayerId,
            compact = compact,
            modifier = Modifier.width(badgeWidth)
        )

        PlayerBadgeCard(
            player = rightPlayer,
            avatarRes = R.drawable.avatar_daheng,
            isCurrentPlayer = rightPlayer?.id == currentPlayerId,
            compact = compact,
            modifier = Modifier.width(badgeWidth)
        )
    }
}

@Composable
private fun PlayerBadgeCard(
    player: PlayerUiState?,
    avatarRes: Int,
    isCurrentPlayer: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        Spacer(modifier = modifier)
        return
    }

    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (isCurrentPlayer) Gold500 else Color.White.copy(alpha = 0.16f)

    Box(
        modifier = modifier.height(if (compact) 78.dp else 112.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.Black.copy(alpha = if (isCurrentPlayer) 0.45f else 0.30f))
                .border(1.dp, borderColor, shape)
                .padding(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(
                avatarRes = avatarRes,
                role = player.role,
                compact = compact,
                modifier = Modifier.size(if (compact) 46.dp else 58.dp)
            )

            Spacer(modifier = Modifier.width(if (compact) 6.dp else 8.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = player.name,
                    color = TextWhite,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                OpponentMiniHand(cardCount = player.handSize, compact = compact)
                if (!player.isOnline) {
                    Text(
                        text = "离线",
                        color = ButtonDanger,
                        fontSize = if (compact) 9.sp else 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerAvatar(
    avatarRes: Int,
    role: PlayerRole,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringColor = when (role) {
        PlayerRole.Landlord -> Gold500
        PlayerRole.Farmer -> Green600
        else -> Color.White.copy(alpha = 0.48f)
    }
    val isMaleAvatar = avatarRes == R.drawable.avatar_daheng
    val avatarZoom = when {
        isMaleAvatar -> if (compact) 0.98f else 1.0f
        compact -> 1.22f
        else -> 1.18f
    }
    val avatarYOffset = if (isMaleAvatar) 0.dp else if (compact) 2.dp else 4.dp

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF1E3D2B))
                .border(if (compact) 2.dp else 3.dp, ringColor.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "玩家头像",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 122.dp else 154.dp)
                    .offset(y = avatarYOffset)
                    .graphicsLayer {
                        scaleX = avatarZoom
                        scaleY = avatarZoom
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth
            )
        }

        if (role == PlayerRole.Landlord) {
            Image(
                painter = painterResource(id = R.drawable.landlord_hat_icon),
                contentDescription = "地主",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(if (compact) 24.dp else 30.dp)
                    .offset(y = if (compact) (-10).dp else (-12).dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun OpponentMiniHand(cardCount: Int, compact: Boolean = false) {
    Row(
        modifier = Modifier
            .padding(top = if (compact) 4.dp else 6.dp)
            .height(if (compact) 20.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Image(
                painter = painterResource(id = R.drawable.opponent_card_back_new),
                contentDescription = "对手手牌",
                modifier = Modifier
                    .width(if (compact) 15.dp else 18.dp)
                    .height(if (compact) 20.dp else 24.dp)
                    .offset(x = (index * (if (compact) -4 else -5)).dp),
                contentScale = ContentScale.FillBounds
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        Box(
            modifier = Modifier
                .size(width = if (compact) 26.dp else 30.dp, height = if (compact) 18.dp else 22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.56f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cardCount.toString(),
                color = Gold500,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CenterPlayedArea(
    lastPlayedCards: List<GameCard>?,
    playedByName: String,
    isBidTurn: Boolean,
    isPlayTurn: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .height(if (compact) 122.dp else 180.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.10f),
                        Color(0x55103F2F),
                        Color.Black.copy(alpha = 0.18f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!lastPlayedCards.isNullOrEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${playedByName.ifEmpty { "上家" }} 出牌",
                    color = Gold500,
                    fontSize = if (compact) 13.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = if (compact) 6.dp else 10.dp)
                )
                PlayedCardsFan(
                    cards = lastPlayedCards,
                    compact = compact,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            TurnPrompt(
                text = when {
                    isBidTurn -> "叫地主阶段"
                    isPlayTurn -> "出牌区"
                    else -> "等待对手行动"
                },
                showTimer = false,
                compact = compact
            )
        }
    }
}

@Composable
private fun PlayedCardsFan(
    cards: List<GameCard>,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.height(if (compact) 84.dp else 118.dp),
        contentAlignment = Alignment.Center
    ) {
        val cardWidth = when {
            compact && cards.size > 12 -> 34.dp
            compact -> 46.dp
            cards.size > 12 -> 42.dp
            else -> 54.dp
        }
        val cardHeight = when {
            compact && cards.size > 12 -> 50.dp
            compact -> 68.dp
            cards.size > 12 -> 62.dp
            else -> 78.dp
        }
        val step = if (cards.size <= 1) {
            0.dp
        } else {
            val availableStep = ((maxWidth - cardWidth).value / (cards.size - 1)).dp
            availableStep.coerceIn(if (compact) 10.dp else 13.dp, if (compact) 32.dp else 38.dp)
        }
        val fanWidth = if (cards.isEmpty()) 0.dp else cardWidth + (step.value * (cards.size - 1)).dp

        Box(
            modifier = Modifier
                .width(fanWidth)
                .height(cardHeight + if (compact) 10.dp else 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            cards.forEachIndexed { index, card ->
                Card(
                    modifier = Modifier
                        .offset(
                            x = (step.value * index).dp,
                            y = if (index % 2 == 0) 0.dp else 4.dp
                        )
                        .width(cardWidth)
                        .height(cardHeight)
                        .zIndex(index.toFloat()),
                    shape = RoundedCornerShape(5.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                ) {
                    Image(
                        painter = painterResource(id = getCardDrawableId(card)),
                        contentDescription = card.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun BattleActionStrip(
    isBidTurn: Boolean,
    isPlayTurn: Boolean,
    currentBid: Int,
    turnSecondsRemaining: Int,
    onBidClick: (Int) -> Unit,
    onBidPassClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPassClick: () -> Unit,
    onHintClick: () -> Unit,
    compact: Boolean,
    showLabel: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isBidTurn && !isPlayTurn) return

    val shape = RoundedCornerShape(if (compact) 22.dp else 26.dp)
    val buttonHeight = if (compact) 34.dp else 36.dp
    val narrowButtonWidth = if (compact) 58.dp else 68.dp
    val wideButtonWidth = if (compact) 80.dp else 92.dp

    Row(
        modifier = modifier
            .widthIn(max = if (compact) 448.dp else 520.dp)
            .background(Color.Black.copy(alpha = 0.54f), shape)
            .border(1.dp, Gold500.copy(alpha = 0.34f), shape)
            .padding(horizontal = if (compact) 9.dp else 12.dp, vertical = if (compact) 7.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        ActionTimer(
            compact = compact,
            remainingSeconds = turnSecondsRemaining
        )

        if (showLabel) {
            Text(
                text = if (isBidTurn) {
                    if (currentBid > 0) "抢地主" else "叫地主"
                } else {
                    "轮到你出牌"
                },
                color = Gold500,
                fontSize = if (compact) 13.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp)
                    .width(if (compact) 70.dp else 94.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBidTurn) {
                PokerImageButton(
                    normalRes = R.drawable.btn_no_bid,
                    onClick = onBidPassClick,
                    modifier = Modifier
                        .width(wideButtonWidth)
                        .height(buttonHeight),
                    contentDescription = "不叫"
                )

                if (currentBid < 1) {
                    PokerImageButton(
                        text = "1分",
                        onClick = { onBidClick(1) },
                        normalRes = R.drawable.btn_green,
                        fontSize = if (compact) 13.sp else 14.sp,
                        modifier = Modifier
                            .width(narrowButtonWidth)
                            .height(buttonHeight)
                    )
                }

                if (currentBid < 2) {
                    PokerImageButton(
                        text = "2分",
                        onClick = { onBidClick(2) },
                        normalRes = R.drawable.btn_orange,
                        fontSize = if (compact) 13.sp else 14.sp,
                        modifier = Modifier
                            .width(narrowButtonWidth)
                            .height(buttonHeight)
                    )
                }

                if (currentBid < 3) {
                    PokerImageButton(
                        onClick = { onBidClick(3) },
                        normalRes = if (currentBid > 0) R.drawable.text_grab else R.drawable.btn_bid,
                        modifier = Modifier
                            .width(wideButtonWidth)
                            .height(buttonHeight),
                        contentDescription = if (currentBid > 0) "抢地主" else "叫地主"
                    )
                }
            } else {
                PokerImageButton(
                    text = "提示",
                    onClick = onHintClick,
                    normalRes = R.drawable.btn_orange,
                    fontSize = if (compact) 13.sp else 14.sp,
                    modifier = Modifier
                        .width(if (compact) 68.dp else 76.dp)
                        .height(buttonHeight)
                )

                PokerImageButton(
                    onClick = onPassClick,
                    normalRes = R.drawable.btn_pass,
                    modifier = Modifier
                        .width(wideButtonWidth)
                        .height(buttonHeight),
                    contentDescription = "不出"
                )

                PokerImageButton(
                    onClick = onPlayClick,
                    normalRes = R.drawable.discard,
                    modifier = Modifier
                        .width(wideButtonWidth)
                        .height(buttonHeight),
                    contentDescription = "出牌"
                )
            }
        }
    }
}

@Composable
private fun ActionTimer(
    compact: Boolean,
    remainingSeconds: Int
) {
    val safeRemaining = remainingSeconds.coerceIn(0, 99)
    val timerColor = if (safeRemaining <= 5) ButtonDanger else Color(0xFF6A2A00)

    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.timer_bg),
            contentDescription = "计时",
            modifier = Modifier.size(if (compact) 34.dp else 40.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = safeRemaining.toString(),
            color = timerColor,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LocalPlayerDock(
    player: PlayerUiState,
    playerCards: List<GameCard>,
    selectedCards: Set<String>,
    onCardClick: (GameCard) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

    Row(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.10f),
                        Color.Black.copy(alpha = 0.46f)
                    )
                ),
                shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .padding(
                start = if (compact) 6.dp else 8.dp,
                top = if (compact) 7.dp else 8.dp,
                end = if (compact) 6.dp else 8.dp,
                bottom = if (compact) 7.dp else 8.dp
            ),
        verticalAlignment = Alignment.Bottom
    ) {
        LocalPlayerBadge(
            player = player,
            compact = compact,
            modifier = Modifier.width(if (compact) 66.dp else 78.dp)
        )

        HandCards(
            cards = playerCards,
            selectedCards = selectedCards,
            onCardClick = onCardClick,
            cardWidth = if (compact) 48.dp else 60.dp,
            cardHeight = if (compact) 72.dp else 90.dp,
            containerHeight = if (compact) 94.dp else 118.dp,
            minStep = if (compact) 14.dp else 18.dp,
            maxStep = if (compact) 32.dp else 38.dp,
            selectedLift = if (compact) 12.dp else 20.dp,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (compact) 2.dp else 4.dp)
        )
    }
}

@Composable
private fun LocalPlayerBadge(
    player: PlayerUiState,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = if (compact) 2.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayerAvatar(
            avatarRes = R.drawable.avatar_yujie,
            role = player.role,
            compact = compact,
            modifier = Modifier.size(if (compact) 46.dp else 58.dp)
        )
        Text(
            text = player.name,
            color = TextWhite,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (compact) 3.dp else 4.dp)
        )
    }
}

@Composable
private fun TurnPrompt(
    text: String,
    showTimer: Boolean,
    compact: Boolean = false,
    remainingSeconds: Int = 30
) {
    val safeRemaining = remainingSeconds.coerceIn(0, 99)
    val timerColor = if (safeRemaining <= 5) ButtonDanger else Color(0xFF6A2A00)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showTimer) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.timer_bg),
                    contentDescription = "计时",
                    modifier = Modifier.size(if (compact) 40.dp else 48.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = safeRemaining.toString(),
                    color = timerColor,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
        }
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = if (compact) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
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
            bottomCards = sampleCards.takeLast(3),
            players = samplePlayers
        )
    }
}
