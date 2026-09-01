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
import com.happy.poker.app.progress.PlayerProgressManager
import com.happy.poker.app.ui.components.*
import com.happy.poker.app.settings.AppSettingsManager
import com.happy.poker.app.sound.GameAudio
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
import androidx.compose.ui.platform.LocalContext

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val specialEffectState by viewModel.specialEffectState.collectAsState()
    val context = LocalContext.current
    val appSettingsManager = remember { AppSettingsManager(context) }
    val progressManager = remember { PlayerProgressManager(context) }
    val beanBalance = progressManager.getBeanBalance()
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var visibleFeedbackId by remember { mutableStateOf(0) }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.cancelPendingGameEndReveal()
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.roomState != RoomState.Bidding && uiState.roomState != RoomState.Playing) {
            viewModel.startGame()
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
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                beanDelta = gameResult.beanDelta,
                beanBalance = gameResult.beanBalance,
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
                beanBalance = beanBalance,
                onBackClick = onBackClick,
                humanAvatarKey = appSettingsManager.getAvatarKey()
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
    beanBalance: Int = PlayerProgressManager.INITIAL_BEAN_BALANCE,
    humanAvatarKey: String = AppSettingsManager.DEFAULT_AVATAR,
    onBackClick: () -> Unit = {}
) {
    val isLocalHumanPlayer: (PlayerUiState) -> Boolean = {
        it.id == "human_player" || it.id.startsWith("human_player_") || it.name == "我"
    }
    val humanPlayer = players.find(isLocalHumanPlayer) ?: PlayerUiState(
        id = "human_player",
        name = "我",
        role = PlayerRole.Unknown,
        handSize = playerCards.size,
        beanBalance = beanBalance
    )
    val localBeanBalance = if (isLocalHumanPlayer(humanPlayer)) {
        beanBalance
    } else {
        humanPlayer.beanBalance
    }
    GameTable {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactHeight = maxHeight < 430.dp
            val landscape = maxWidth > maxHeight
            val tightLandscape = landscape && maxHeight < 430.dp
            val sideInset = when {
                tightLandscape -> 10.dp
                compactHeight -> 12.dp
                else -> 18.dp
            }
            val centerWidthFraction = when {
                tightLandscape -> 0.52f
                compactHeight && landscape -> 0.60f
                landscape -> 0.64f
                else -> 0.90f
            }
            val centerMaxWidth = when {
                tightLandscape -> 440.dp
                compactHeight -> 580.dp
                landscape -> 640.dp
                else -> 720.dp
            }
            val handCardWidth = when {
                tightLandscape -> 54.dp
                compactHeight -> 58.dp
                landscape -> 66.dp
                else -> 60.dp
            }
            val handCardHeight = when {
                tightLandscape -> 82.dp
                compactHeight -> 88.dp
                landscape -> 100.dp
                else -> 90.dp
            }
            val selectedLift = when {
                tightLandscape -> 20.dp
                compactHeight -> 22.dp
                else -> 24.dp
            }
            val handContainerHeight = handCardHeight + selectedLift + if (tightLandscape) 12.dp else 16.dp
            val actionBottom = handContainerHeight + when {
                tightLandscape -> 6.dp
                compactHeight -> 8.dp
                else -> 14.dp
            }
            val opponentOffsetY = when {
                tightLandscape -> (-42).dp
                compactHeight -> (-36).dp
                landscape -> (-30).dp
                else -> (-24).dp
            }

            PokerTopHud(
                currentBid = currentBid,
                bottomCards = bottomCards,
                players = players,
                onBackClick = onBackClick,
                landscape = landscape,
                compact = compactHeight,
                tightLandscape = tightLandscape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = sideInset, vertical = if (tightLandscape) 4.dp else 7.dp)
                    .zIndex(10f)
            )

            OpponentStrip(
                players = players,
                humanPlayerId = humanPlayer.id,
                currentPlayerId = currentPlayerId,
                compact = compactHeight,
                tightLandscape = tightLandscape,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = sideInset)
                    .offset(y = opponentOffsetY)
                    .zIndex(12f)
            )

            CenterPlayedArea(
                lastPlayedCards = lastPlayedCards,
                isBidTurn = isBidTurn,
                isPlayTurn = isPlayTurn,
                compact = compactHeight,
                landscape = landscape,
                tightLandscape = tightLandscape,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = if (isBidTurn || isPlayTurn) (-20).dp else (-8).dp)
                    .fillMaxWidth(centerWidthFraction)
                    .widthIn(max = centerMaxWidth)
                    .zIndex(4f)
            )

            if (isBidTurn || isPlayTurn) {
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
                    landscape = landscape,
                    tightLandscape = tightLandscape,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = actionBottom)
                        .zIndex(8f)
                )
            }

            LocalPlayerBadge(
                player = humanPlayer.copy(handSize = playerCards.size, beanBalance = localBeanBalance),
                avatarKey = humanAvatarKey,
                compact = compactHeight,
                tightLandscape = tightLandscape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = sideInset, bottom = if (compactHeight) 10.dp else 14.dp)
                    .zIndex(12f)
            )

            HandCards(
                cards = playerCards,
                selectedCards = selectedCards,
                onCardClick = onCardClick,
                cardWidth = handCardWidth,
                cardHeight = handCardHeight,
                containerHeight = handContainerHeight,
                minStep = if (tightLandscape) 14.dp else 16.dp,
                maxStep = if (tightLandscape) 34.dp else 40.dp,
                selectedLift = selectedLift,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (tightLandscape) 0.dp else 4.dp)
                    .fillMaxWidth(if (tightLandscape) 0.66f else 0.70f)
                    .widthIn(max = if (tightLandscape) 720.dp else 860.dp)
                    .zIndex(7f)
            )

            TableBottomStatus(
                multiplier = multiplier,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = sideInset, bottom = if (compactHeight) 12.dp else 18.dp)
                    .zIndex(9f)
            )
        }
    }
}

@Composable
private fun PokerTopHud(
    currentBid: Int,
    bottomCards: List<GameCard>,
    players: List<PlayerUiState>,
    onBackClick: () -> Unit,
    landscape: Boolean,
    compact: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val sideWidth = when {
        tightLandscape -> 106.dp
        compact -> 122.dp
        landscape -> 142.dp
        else -> 132.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.width(sideWidth),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(if (tightLandscape) 7.dp else 10.dp)
        ) {
            PokerNavChip(
                text = "返回",
                onClick = onBackClick
            )
        }

        BottomCardsTray(
            cards = bottomCards,
            currentBid = currentBid,
            isRevealed = players.any { it.role == PlayerRole.Landlord },
            landscape = landscape,
            compact = compact,
            tightLandscape = tightLandscape,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(sideWidth))
    }
}

@Composable
private fun PokerNavChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PokerBackButton(
        onClick = onClick,
        contentDescription = text,
        modifier = modifier
    )
}

@Composable
private fun BottomCardsTray(
    cards: List<GameCard>,
    currentBid: Int,
    isRevealed: Boolean,
    landscape: Boolean,
    tightLandscape: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val trayCardWidth = when {
        tightLandscape -> 48.dp
        compact -> 50.dp
        landscape -> 52.dp
        else -> 54.dp
    }
    val trayCardHeight = when {
        tightLandscape -> 70.dp
        compact -> 74.dp
        landscape -> 78.dp
        else -> 80.dp
    }

    Row(
        modifier = modifier
            .widthIn(max = if (tightLandscape) 390.dp else 520.dp)
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((-6).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRevealed && cards.isNotEmpty()) {
                cards.take(3).forEach { card ->
                    TopMiniPlayingCard(
                        card = card,
                        width = trayCardWidth,
                        height = trayCardHeight
                    )
                }
            } else {
                repeat(3) {
                    MiniCardBack(compact = compact, landscape = landscape, tightLandscape = tightLandscape)
                }
            }
        }

        TopBidInfo(
            currentBid = currentBid,
            landscape = landscape,
            compact = compact,
            tightLandscape = tightLandscape
        )
    }
}

@Composable
private fun TopBidInfo(
    currentBid: Int,
    landscape: Boolean,
    compact: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tightLandscape) 1.dp else 2.dp)
    ) {
        Text(
            text = "底分 20",
            color = TextWhite.copy(alpha = 0.88f),
            fontSize = when {
                tightLandscape -> 10.sp
                compact -> 11.sp
                landscape -> 12.sp
                else -> 12.sp
            },
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        TopBidStatus(
            currentBid = currentBid,
            compact = compact,
            tightLandscape = tightLandscape
        )
    }
}

@Composable
private fun TopBidStatus(
    currentBid: Int,
    compact: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    CounterStatusPlate(
        label = "叫",
        valueText = currentBid.takeIf { it > 0 }?.toString() ?: "-",
        modifier = modifier,
        compact = compact || tightLandscape
    )
}

@Composable
private fun TopMiniPlayingCard(
    card: GameCard,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    Card(
        modifier = Modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Image(
            painter = painterResource(id = getCardDrawableId(card)),
            contentDescription = card.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun MiniCardBack(
    compact: Boolean = false,
    landscape: Boolean = false,
    tightLandscape: Boolean = false
) {
    val cardWidth = when {
        tightLandscape -> 48.dp
        compact -> 50.dp
        landscape -> 52.dp
        else -> 54.dp
    }
    val cardHeight = when {
        tightLandscape -> 70.dp
        compact -> 74.dp
        landscape -> 78.dp
        else -> 80.dp
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.card_back_new),
            contentDescription = "底牌牌背",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun OpponentStrip(
    players: List<PlayerUiState>,
    humanPlayerId: String,
    currentPlayerId: String?,
    compact: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val humanIndex = players.indexOfFirst { it.id == humanPlayerId }
    val seatedPlayers = if (humanIndex >= 0) {
        players.drop(humanIndex) + players.take(humanIndex)
    } else {
        players
    }
    val leftPlayer = seatedPlayers.getOrNull(1)
    val rightPlayer = seatedPlayers.getOrNull(2)
    val badgeWidth = when {
        tightLandscape -> 126.dp
        compact -> 144.dp
        else -> 164.dp
    }

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
            tightLandscape = tightLandscape,
            countOnStart = false,
            modifier = Modifier.width(badgeWidth)
        )

        PlayerBadgeCard(
            player = rightPlayer,
            avatarRes = R.drawable.avatar_daheng,
            isCurrentPlayer = rightPlayer?.id == currentPlayerId,
            compact = compact,
            tightLandscape = tightLandscape,
            countOnStart = true,
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
    tightLandscape: Boolean,
    countOnStart: Boolean,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        Spacer(modifier = modifier)
        return
    }

    val borderColor = if (isCurrentPlayer) Gold500 else Color.White.copy(alpha = 0.24f)
    val cardHeight = when {
        tightLandscape -> 116.dp
        compact -> 126.dp
        else -> 142.dp
    }
    val avatarSize = when {
        tightLandscape -> 58.dp
        compact -> 66.dp
        else -> 74.dp
    }

    Box(
        modifier = modifier.height(cardHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isCurrentPlayer) 0.dp else 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (countOnStart) Arrangement.End else Arrangement.Start
        ) {
            if (countOnStart) {
                CardCountPlate(player.handSize, compact = compact, modifier = Modifier.padding(end = 6.dp))
            }

            Box(
                modifier = Modifier
                    .width(if (tightLandscape) 78.dp else 92.dp)
                    .zIndex(if (player.role == PlayerRole.Landlord) 20f else 0f),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF101A36).copy(alpha = if (isCurrentPlayer) 0.68f else 0.48f))
                        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                        .padding(horizontal = 5.dp, vertical = if (tightLandscape) 5.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlayerAvatar(
                        avatarRes = avatarRes,
                        role = player.role,
                        compact = compact,
                        tightLandscape = tightLandscape,
                        modifier = Modifier.size(avatarSize)
                    )
                    Text(
                        text = player.name,
                        color = TextWhite,
                        fontSize = when {
                            tightLandscape -> 10.sp
                            compact -> 11.sp
                            else -> 12.sp
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    BeanAmountText(
                        beanBalance = player.beanBalance,
                        modifier = Modifier
                            .padding(top = 0.dp)
                            .offset(y = (-2).dp),
                        compact = true
                    )
                    if (!player.isOnline) {
                        Text(
                            text = "离线",
                            color = ButtonDanger,
                            fontSize = when {
                                tightLandscape -> 8.sp
                                compact -> 9.sp
                                else -> 10.sp
                            }
                        )
                    }
                }

                if (player.role == PlayerRole.Landlord) {
                    LandlordHatOverlay(
                        compact = compact,
                        tightLandscape = tightLandscape,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .zIndex(30f)
                    )
                }
            }

            if (!countOnStart) {
                CardCountPlate(player.handSize, compact = compact, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun CardCountPlate(
    count: Int,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(if (compact) 28.dp else 32.dp)
            .height(if (compact) 40.dp else 48.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF5BA5E5),
                        Color(0xFF234A93)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.62f), RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = TextWhite,
            fontSize = if (compact) 19.sp else 22.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun LandlordHatOverlay(
    compact: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val hatSize = when {
        tightLandscape -> 26.dp
        compact -> 28.dp
        else -> 32.dp
    }
    val hatOffsetY = when {
        tightLandscape -> (-12).dp
        compact -> (-14).dp
        else -> (-17).dp
    }

    Image(
        painter = painterResource(id = R.drawable.landlord_hat_icon),
        contentDescription = "地主",
        modifier = modifier
            .size(hatSize)
            .offset(y = hatOffsetY),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun PlayerAvatar(
    avatarRes: Int,
    role: PlayerRole,
    compact: Boolean = false,
    tightLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringColor = when (role) {
        PlayerRole.Landlord -> Gold500
        PlayerRole.Farmer -> Green600
        else -> Color.White.copy(alpha = 0.48f)
    }
    val isMaleAvatar = avatarRes == R.drawable.avatar_daheng
    val avatarZoom = when {
        isMaleAvatar -> when {
            tightLandscape -> 0.92f
            compact -> 0.98f
            else -> 1.0f
        }
        compact -> 1.22f
        else -> 1.18f
    }
    val avatarYOffset = when {
        isMaleAvatar -> if (tightLandscape) (-2).dp else 0.dp
        compact -> 2.dp
        else -> 4.dp
    }

    Box(
        modifier = modifier
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
                .height(
                    when {
                        tightLandscape -> 104.dp
                        compact -> 122.dp
                        else -> 154.dp
                    }
                )
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
    isBidTurn: Boolean,
    isPlayTurn: Boolean,
    compact: Boolean,
    landscape: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val panelHeight = when {
        tightLandscape -> 126.dp
        compact -> 138.dp
        landscape -> 150.dp
        else -> 168.dp
    }
    Box(
        modifier = modifier
            .height(panelHeight)
            .padding(horizontal = 12.dp, vertical = if (landscape) 8.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!lastPlayedCards.isNullOrEmpty()) {
            PlayedCardsFan(
                cards = lastPlayedCards,
                landscape = landscape,
                compact = compact,
                tightLandscape = tightLandscape,
                modifier = Modifier.fillMaxWidth()
            )
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
    landscape: Boolean,
    compact: Boolean = false,
    tightLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fanHeight = when {
        tightLandscape -> 112.dp
        compact -> 120.dp
        landscape -> 130.dp
        else -> 128.dp
    }
    BoxWithConstraints(
        modifier = modifier.height(fanHeight),
        contentAlignment = Alignment.Center
    ) {
        val cardWidth = when {
            tightLandscape && cards.size > 12 -> 38.dp
            tightLandscape -> 50.dp
            compact && cards.size > 12 -> 40.dp
            landscape && cards.size > 12 -> 44.dp
            compact -> 54.dp
            landscape -> 58.dp
            cards.size > 12 -> 42.dp
            else -> 54.dp
        }
        val cardHeight = when {
            tightLandscape && cards.size > 12 -> 58.dp
            tightLandscape -> 78.dp
            compact && cards.size > 12 -> 62.dp
            landscape && cards.size > 12 -> 68.dp
            compact -> 84.dp
            landscape -> 92.dp
            cards.size > 12 -> 66.dp
            else -> 84.dp
        }
        val step = if (cards.size <= 1) {
            0.dp
        } else {
            val availableStep = ((maxWidth - cardWidth).value / (cards.size - 1)).dp
            val minStep = when {
                tightLandscape -> 6.dp
                compact -> 10.dp
                landscape -> 11.dp
                else -> 13.dp
            }
            val maxStep = when {
                tightLandscape -> 18.dp
                compact -> 32.dp
                landscape -> 30.dp
                else -> 38.dp
            }
            availableStep.coerceIn(minStep, maxStep)
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
                        contentScale = ContentScale.Fit
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
    landscape: Boolean,
    tightLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isBidTurn && !isPlayTurn) return

    val buttonHeight = when {
        tightLandscape -> 42.dp
        compact -> 46.dp
        landscape -> 48.dp
        else -> 46.dp
    }
    val narrowButtonWidth = when {
        tightLandscape -> 62.dp
        compact -> 70.dp
        landscape -> 76.dp
        else -> 70.dp
    }
    val wideButtonWidth = when {
        tightLandscape -> 92.dp
        compact -> 104.dp
        landscape -> 116.dp
        else -> 104.dp
    }
    val labelFontSize = when {
        tightLandscape -> 20.sp
        compact -> 22.sp
        else -> 24.sp
    }

    Row(
        modifier = modifier
            .widthIn(max = when {
                tightLandscape -> 520.dp
                compact -> 620.dp
                else -> 700.dp
            }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            when {
                tightLandscape -> 8.dp
                compact -> 10.dp
                else -> 12.dp
            },
            Alignment.CenterHorizontally
        )
    ) {
        if (isBidTurn) {
            PokerImageButton(
                normalRes = R.drawable.btn_no_bid,
                onClick = onBidPassClick,
                soundType = null,
                modifier = Modifier
                    .width(wideButtonWidth)
                    .height(buttonHeight),
                contentDescription = "不叫"
            )

            ActionTimer(
                compact = compact,
                remainingSeconds = turnSecondsRemaining
            )

            if (currentBid < 1) {
                PokerImageButton(
                    text = "1分",
                    onClick = { onBidClick(1) },
                    normalRes = R.drawable.btn_blue,
                    soundType = null,
                    fontSize = labelFontSize,
                    modifier = Modifier
                        .width(narrowButtonWidth)
                        .height(buttonHeight)
                )
            }

            if (currentBid < 2) {
                PokerImageButton(
                    text = "2分",
                    onClick = { onBidClick(2) },
                    normalRes = R.drawable.btn_blue,
                    soundType = null,
                    fontSize = labelFontSize,
                    modifier = Modifier
                        .width(narrowButtonWidth)
                        .height(buttonHeight)
                )
            }

            if (currentBid < 3) {
                PokerImageButton(
                    onClick = { onBidClick(3) },
                    normalRes = if (currentBid > 0) R.drawable.text_grab else R.drawable.btn_bid,
                    soundType = null,
                    modifier = Modifier
                        .width(wideButtonWidth)
                        .height(buttonHeight),
                    contentDescription = if (currentBid > 0) "抢地主" else "叫地主"
                )
            }
        } else {
            PokerImageButton(
                onClick = onPassClick,
                normalRes = R.drawable.btn_pass,
                soundType = null,
                modifier = Modifier
                    .width(wideButtonWidth)
                    .height(buttonHeight),
                contentDescription = "不出"
            )

            ActionTimer(
                compact = compact,
                remainingSeconds = turnSecondsRemaining
            )

            PokerImageButton(
                text = "提示",
                onClick = onHintClick,
                normalRes = R.drawable.btn_blue,
                soundType = null,
                fontSize = labelFontSize,
                modifier = Modifier
                    .width(wideButtonWidth)
                    .height(buttonHeight)
            )

            PokerImageButton(
                onClick = onPlayClick,
                normalRes = R.drawable.discard,
                soundType = null,
                modifier = Modifier
                    .width(wideButtonWidth)
                    .height(buttonHeight),
                contentDescription = "出牌"
            )
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
            modifier = Modifier.size(if (compact) 48.dp else 54.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = safeRemaining.toString(),
            color = timerColor,
            fontSize = if (compact) 18.sp else 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LocalPlayerBadge(
    player: PlayerUiState,
    avatarKey: String,
    compact: Boolean = false,
    tightLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val avatarSize = when {
        tightLandscape -> 58.dp
        compact -> 64.dp
        else -> 72.dp
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(if (tightLandscape) 7.dp else 9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .zIndex(if (player.role == PlayerRole.Landlord) 20f else 0f),
            contentAlignment = Alignment.TopCenter
        ) {
            PlayerAvatar(
                avatarRes = avatarResourceForKey(avatarKey),
                role = player.role,
                compact = compact,
                tightLandscape = tightLandscape,
                modifier = Modifier.matchParentSize()
            )

            if (player.role == PlayerRole.Landlord) {
                LandlordHatOverlay(
                    compact = compact,
                    tightLandscape = tightLandscape,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .zIndex(30f)
                )
            }
        }
        Column(
            modifier = Modifier.padding(bottom = if (compact) 2.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
        ) {
            Text(
                text = player.name,
                color = TextWhite,
                fontSize = when {
                    tightLandscape -> 15.sp
                    compact -> 17.sp
                    else -> 19.sp
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = if (tightLandscape) 112.dp else 150.dp)
            )
            BeanStatusPill(beanBalance = player.beanBalance)
        }
    }
}

@Composable
private fun TableBottomStatus(
    multiplier: Int,
    modifier: Modifier = Modifier
) {
    CounterStatusPlate(
        label = "倍",
        valueText = multiplier.toString(),
        modifier = modifier
    )
}

private fun avatarResourceForKey(avatarKey: String): Int =
    when (avatarKey) {
        "daheng" -> R.drawable.avatar_daheng
        "luoli" -> R.drawable.avatar_luoli
        else -> R.drawable.avatar_yujie
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
