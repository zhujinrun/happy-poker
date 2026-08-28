package com.happy.poker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.ui.theme.*
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit

@Composable
fun PlayingCard(
    card: GameCard,
    isSelected: Boolean = false,
    isFaceDown: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    Card(
        modifier = modifier
            .width(60.dp)
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFaceDown) Color(0xFF1565C0) else CardWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            if (isFaceDown) {
                drawCardBack()
            } else {
                drawCardContent(card, textMeasurer)
            }
        }
    }
}

private fun DrawScope.drawCardBack() {
    val width = size.width
    val height = size.height
    
    // 绘制背面图案
    drawRect(
        color = Color(0xFF0D47A1),
        topLeft = Offset(4f, 4f),
        size = Size(width - 8f, height - 8f),
        style = Stroke(width = 2f)
    )
    
    // 绘制装饰图案
    drawCircle(
        color = Color(0xFF1976D2),
        radius = 15f,
        center = Offset(width / 2, height / 2)
    )
}

private fun DrawScope.drawCardContent(
    card: GameCard,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val width = size.width
    val height = size.height
    
    // 确定花色颜色
    val suitColor = when (card.suit) {
        Suit.Hearts, Suit.Diamonds -> Color.Red
        Suit.Spades, Suit.Clubs -> Color.Black
        Suit.Joker -> if (card.rank == Rank.BigJoker) Color.Red else Color.Black
    }
    
    // 绘制点数（左上角）
    val rankText = card.rank.label
    val rankStyle = TextStyle(
        fontSize = 14.sp,
        color = suitColor
    )
    val rankLayoutResult = textMeasurer.measure(rankText, rankStyle)
    drawText(
        textLayoutResult = rankLayoutResult,
        topLeft = Offset(4f, 4f)
    )
    
    // 绘制花色符号（点数下方）
    val suitText = card.suit.symbol
    val suitStyle = TextStyle(
        fontSize = 10.sp,
        color = suitColor
    )
    val suitLayoutResult = textMeasurer.measure(suitText, suitStyle)
    drawText(
        textLayoutResult = suitLayoutResult,
        topLeft = Offset(4f, 20f)
    )
    
    // 绘制中心大花色
    val centerSuitStyle = TextStyle(
        fontSize = 24.sp,
        color = suitColor
    )
    val centerSuitLayoutResult = textMeasurer.measure(suitText, centerSuitStyle)
    drawText(
        textLayoutResult = centerSuitLayoutResult,
        topLeft = Offset(
            (width - centerSuitLayoutResult.size.width) / 2,
            (height - centerSuitLayoutResult.size.height) / 2
        )
    )
    
    // 绘制边框
    drawRect(
        color = Color.LightGray,
        topLeft = Offset(0f, 0f),
        size = Size(width, height),
        style = Stroke(width = 1f)
    )
}

@Composable
fun HandCards(
    cards: List<GameCard>,
    selectedCards: Set<String> = emptySet(),
    onCardClick: (GameCard) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-20).dp)
    ) {
        cards.forEach { card ->
            PlayingCard(
                card = card,
                isSelected = card.id in selectedCards,
                onClick = { onCardClick(card) }
            )
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
