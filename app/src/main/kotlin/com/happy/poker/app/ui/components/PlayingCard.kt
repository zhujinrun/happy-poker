package com.happy.poker.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.happy.poker.app.R
import com.happy.poker.app.ui.theme.*
import com.happy.poker.core.model.Card as GameCard
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit

/**
 * 获取纸牌图片资源ID
 */
fun getCardDrawableId(card: GameCard): Int {
    // 王牌特殊处理
    if (card.rank == Rank.SmallJoker) return R.drawable.card_bj
    if (card.rank == Rank.BigJoker) return R.drawable.card_lj
    
    // 普通牌
    val rankChar = when (card.rank) {
        Rank.Two -> "2"
        Rank.Three -> "3"
        Rank.Four -> "4"
        Rank.Five -> "5"
        Rank.Six -> "6"
        Rank.Seven -> "7"
        Rank.Eight -> "8"
        Rank.Nine -> "9"
        Rank.Ten -> "x"
        Rank.Jack -> "j"
        Rank.Queen -> "q"
        Rank.King -> "k"
        Rank.Ace -> "a"
        else -> "2"
    }
    
    val suitChar = when (card.suit) {
        Suit.Clubs -> "c"
        Suit.Diamonds -> "d"
        Suit.Hearts -> "h"
        Suit.Spades -> "s"
        else -> "c"
    }
    
    val resourceName = "card_${rankChar}${suitChar}"
    val resourceId = when (resourceName) {
        "card_2c" -> R.drawable.card_2c
        "card_2d" -> R.drawable.card_2d
        "card_2h" -> R.drawable.card_2h
        "card_2s" -> R.drawable.card_2s
        "card_3c" -> R.drawable.card_3c
        "card_3d" -> R.drawable.card_3d
        "card_3h" -> R.drawable.card_3h
        "card_3s" -> R.drawable.card_3s
        "card_4c" -> R.drawable.card_4c
        "card_4d" -> R.drawable.card_4d
        "card_4h" -> R.drawable.card_4h
        "card_4s" -> R.drawable.card_4s
        "card_5c" -> R.drawable.card_5c
        "card_5d" -> R.drawable.card_5d
        "card_5h" -> R.drawable.card_5h
        "card_5s" -> R.drawable.card_5s
        "card_6c" -> R.drawable.card_6c
        "card_6d" -> R.drawable.card_6d
        "card_6h" -> R.drawable.card_6h
        "card_6s" -> R.drawable.card_6s
        "card_7c" -> R.drawable.card_7c
        "card_7d" -> R.drawable.card_7d
        "card_7h" -> R.drawable.card_7h
        "card_7s" -> R.drawable.card_7s
        "card_8c" -> R.drawable.card_8c
        "card_8d" -> R.drawable.card_8d
        "card_8h" -> R.drawable.card_8h
        "card_8s" -> R.drawable.card_8s
        "card_9c" -> R.drawable.card_9c
        "card_9d" -> R.drawable.card_9d
        "card_9h" -> R.drawable.card_9h
        "card_9s" -> R.drawable.card_9s
        "card_xc" -> R.drawable.card_xc
        "card_xd" -> R.drawable.card_xd
        "card_xh" -> R.drawable.card_xh
        "card_xs" -> R.drawable.card_xs
        "card_jc" -> R.drawable.card_jc
        "card_jd" -> R.drawable.card_jd
        "card_jh" -> R.drawable.card_jh
        "card_js" -> R.drawable.card_js
        "card_qc" -> R.drawable.card_qc
        "card_qd" -> R.drawable.card_qd
        "card_qh" -> R.drawable.card_qh
        "card_qs" -> R.drawable.card_qs
        "card_kc" -> R.drawable.card_kc
        "card_kd" -> R.drawable.card_kd
        "card_kh" -> R.drawable.card_kh
        "card_ks" -> R.drawable.card_ks
        "card_ac" -> R.drawable.card_ac
        "card_ad" -> R.drawable.card_ad
        "card_ah" -> R.drawable.card_ah
        "card_as" -> R.drawable.card_as
        else -> R.drawable.card_2c
    }
    
    return resourceId
}

@Composable
fun PlayingCard(
    card: GameCard,
    isSelected: Boolean = false,
    isFaceDown: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
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
    ) { if (it) -20f else 0f }
    
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
            .width(60.dp)
            .height(90.dp)
            .scale(scale)
            .offset(y = offsetY.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFaceDown) Color(0xFF1565C0) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        if (isFaceDown) {
            Image(
                painter = painterResource(id = R.drawable.card_back),
                contentDescription = "牌背",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = getCardDrawableId(card)),
                contentDescription = card.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
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
        horizontalArrangement = Arrangement.spacedBy((-40).dp)
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
