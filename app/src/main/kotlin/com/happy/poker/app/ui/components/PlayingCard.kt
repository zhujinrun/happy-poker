package com.happy.poker.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.happy.poker.app.R
import com.happy.poker.app.ui.theme.*
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit
import kotlin.math.roundToInt

@Composable
fun PlayingCard(
    card: GameCard,
    isSelected: Boolean = false,
    isFaceDown: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    cardWidth: Dp = 60.dp,
    cardHeight: Dp = 90.dp,
    selectedLift: Dp = 20.dp,
    clickEnabled: Boolean = true
) {
    // 选中动画
    val transition = updateTransition(targetState = isSelected, label = "cardSelect")
    val offsetY by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "offsetY"
    ) { if (it) -selectedLift.value else 0f }
    
    val scale by transition.animateFloat(
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "scale"
    ) { if (it) 1.1f else 1f }
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .scale(scale)
            .offset(y = offsetY.dp)
            .then(if (clickEnabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFaceDown) Color(0xFF1565C0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        border = if (isSelected) BorderStroke(2.dp, Gold500) else null
    ) {
        if (isFaceDown) {
            Image(
                painter = painterResource(id = R.drawable.card_back),
                contentDescription = "牌背",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            PokerCardFace(card = card, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun PokerCardFace(
    card: GameCard,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.poker_card_frame),
            contentDescription = card.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        if (card.rank == Rank.SmallJoker || card.rank == Rank.BigJoker) {
            Image(
                painter = painterResource(
                    id = if (card.rank == Rank.SmallJoker) {
                        R.drawable.poker_small_king
                    } else {
                        R.drawable.poker_big_king
                    }
                ),
                contentDescription = card.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = maxWidth * 0.06f,
                        vertical = maxHeight * 0.05f
                ),
                contentScale = ContentScale.Fit
            )
        } else {
            val rankIndex = pokerRankSpriteIndex(card.rank, isRedSuit(card.suit))
            val suitIndex = pokerSuitSpriteIndex(card.suit)
            val rankWidth = maxWidth * 0.238f
            val rankHeight = maxHeight * 0.255f
            val suitWidth = maxWidth * 0.238f
            val suitHeight = maxHeight * 0.191f
            val leftRankX = maxWidth * 0.086f
            val leftRankY = maxHeight * 0.051f
            val leftSuitX = maxWidth * 0.095f
            val leftSuitY = leftRankY + rankHeight
            val rightX = maxWidth * 0.690f
            val rightSuitY = maxHeight * 0.492f
            val rightRankY = rightSuitY + suitHeight

            PokerSpriteImage(
                spriteSheetRes = R.drawable.poker_num_2,
                columns = 26,
                index = rankIndex,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = leftRankX, y = leftRankY)
                    .width(rankWidth)
                    .height(rankHeight)
            )
            PokerSpriteImage(
                spriteSheetRes = R.drawable.poker_type,
                columns = 4,
                index = suitIndex,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = leftSuitX, y = leftSuitY)
                    .width(suitWidth)
                    .height(suitHeight)
            )
            PokerSpriteImage(
                spriteSheetRes = R.drawable.poker_type,
                columns = 4,
                index = suitIndex,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = rightX, y = rightSuitY)
                    .width(suitWidth)
                    .height(suitHeight)
                    .rotate(180f)
            )
            PokerSpriteImage(
                spriteSheetRes = R.drawable.poker_num_2,
                columns = 26,
                index = rankIndex,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = rightX, y = rightRankY)
                    .width(rankWidth)
                    .height(rankHeight)
                    .rotate(180f)
            )
        }
    }
}

@Composable
private fun PokerSpriteImage(
    spriteSheetRes: Int,
    columns: Int,
    index: Int,
    modifier: Modifier = Modifier
) {
    val spriteSheet = ImageBitmap.imageResource(id = spriteSheetRes)
    Canvas(modifier = modifier) {
        val srcLeft = (spriteSheet.width * index.toFloat() / columns)
            .roundToInt()
            .coerceIn(0, spriteSheet.width - 1)
        val srcRight = (spriteSheet.width * (index + 1).toFloat() / columns)
            .roundToInt()
            .coerceIn(srcLeft + 1, spriteSheet.width)
        val spriteWidth = srcRight - srcLeft
        val spriteHeight = spriteSheet.height
        drawImage(
            image = spriteSheet,
            srcOffset = IntOffset(srcLeft, 0),
            srcSize = IntSize(spriteWidth, spriteHeight),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(
                width = size.width.roundToInt().coerceAtLeast(1),
                height = size.height.roundToInt().coerceAtLeast(1)
            ),
            filterQuality = FilterQuality.None
        )
    }
}

private fun isRedSuit(suit: Suit): Boolean {
    return suit == Suit.Hearts || suit == Suit.Diamonds
}

private fun pokerSuitSpriteIndex(suit: Suit): Int {
    return when (suit) {
        Suit.Diamonds -> 0
        Suit.Clubs -> 1
        Suit.Hearts -> 2
        Suit.Spades -> 3
        Suit.Joker -> 0
    }
}

private fun pokerRankSpriteIndex(rank: Rank, isRed: Boolean): Int {
    val rankOffset = when (rank) {
        Rank.Three -> 0
        Rank.Four -> 1
        Rank.Five -> 2
        Rank.Six -> 3
        Rank.Seven -> 4
        Rank.Eight -> 5
        Rank.Nine -> 6
        Rank.Ten -> 7
        Rank.Jack -> 8
        Rank.Queen -> 9
        Rank.King -> 10
        Rank.Ace -> 11
        Rank.Two -> 12
        Rank.SmallJoker,
        Rank.BigJoker -> 0
    }
    return rankOffset + if (isRed) 0 else 13
}

@Composable
fun HandCards(
    cards: List<GameCard>,
    selectedCards: Set<String> = emptySet(),
    onCardClick: (GameCard) -> Unit = {},
    modifier: Modifier = Modifier,
    cardWidth: Dp = 60.dp,
    cardHeight: Dp = 90.dp,
    containerHeight: Dp = 118.dp,
    minStep: Dp = 18.dp,
    maxStep: Dp = 38.dp,
    selectedLift: Dp = 20.dp
) {
    val density = LocalDensity.current
    val currentSelectedCards by rememberUpdatedState(selectedCards)
    val currentOnCardClick by rememberUpdatedState(onCardClick)
    val displayCards = cards.sortedWith(
        compareByDescending<GameCard> { it.rank.value }
            .thenBy { it.suit.ordinal }
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight),
        contentAlignment = androidx.compose.ui.Alignment.BottomCenter
    ) {
        val step = if (displayCards.size <= 1) {
            0.dp
        } else {
            val availableStep = ((maxWidth - cardWidth).value / (displayCards.size - 1)).dp
            availableStep.coerceIn(minStep, maxStep)
        }
        val handWidth = if (displayCards.isEmpty()) 0.dp else cardWidth + (step.value * (displayCards.size - 1)).dp
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val stepPx = with(density) { step.toPx() }
        val handWidthPx = with(density) { handWidth.toPx() }

        fun cardAt(position: Offset): GameCard? {
            if (displayCards.isEmpty() || position.x < 0f || position.x > handWidthPx) return null

            val hitIndex = displayCards.indices.reversed().firstOrNull { index ->
                val left = stepPx * index
                position.x >= left && position.x <= left + cardWidthPx
            }
            return hitIndex?.let(displayCards::get)
        }

        Box(
            modifier = Modifier
                .width(handWidth)
                .height(containerHeight)
                .pointerInput(displayCards, cardWidthPx, stepPx, handWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchedCardIds = mutableSetOf<String>()
                        val selectedAtGestureStart = currentSelectedCards
                        var shouldSelect: Boolean? = null

                        fun applyCardAt(position: Offset) {
                            val card = cardAt(position) ?: return
                            if (!touchedCardIds.add(card.id)) return

                            val targetSelected = shouldSelect
                                ?: (card.id !in selectedAtGestureStart).also { shouldSelect = it }
                            val isSelectedAtStart = card.id in selectedAtGestureStart
                            if (targetSelected != isSelectedAtStart) {
                                currentOnCardClick(card)
                            }
                        }

                        applyCardAt(down.position)
                        down.consume()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) break
                            if (change.positionChanged()) {
                                applyCardAt(change.position)
                                change.consume()
                            }
                            if (!change.pressed) break
                        }
                    }
                }
        ) {
            displayCards.forEachIndexed { index, card ->
                PlayingCard(
                    card = card,
                    isSelected = card.id in selectedCards,
                    onClick = { currentOnCardClick(card) },
                    modifier = Modifier
                        .offset(x = (step.value * index).dp)
                        .width(cardWidth)
                        .height(cardHeight)
                        .zIndex(index.toFloat()),
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    selectedLift = selectedLift,
                    clickEnabled = false
                )
            }
        }
    }
}

@Composable
fun CardBack(
    modifier: Modifier = Modifier
) {
    PlayingCard(
        card = GameCard(Rank.Three, Suit.Spades),
        isFaceDown = true,
        modifier = modifier
    )
}

@Composable
fun SmallPlayingCard(
    card: GameCard,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(40.dp)
            .height(56.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        PokerCardFace(card = card, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun PlayingCardPreview() {
    HappyPokerTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayingCard(
                card = GameCard(Rank.Ace, Suit.Spades)
            )
            PlayingCard(
                card = GameCard(Rank.King, Suit.Hearts),
                isSelected = true
            )
            PlayingCard(
                card = GameCard(Rank.Three, Suit.Clubs)
            )
            CardBack()
        }
    }
}
