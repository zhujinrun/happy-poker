package com.happy.poker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import com.happy.poker.app.ui.components.PokerGlassPanel
import com.happy.poker.app.ui.components.PokerImageButton
import com.happy.poker.app.ui.components.PokerLobbyHeader
import com.happy.poker.app.ui.components.BeanStatusPill
import com.happy.poker.app.ui.components.PokerScreenBackground
import com.happy.poker.app.ui.theme.*

data class PlayerResult(
    val name: String,
    val role: String,
    val score: Int,
    val isWinner: Boolean
)

@Composable
fun ResultScreen(
    winner: String = "地主",
    isLandlordWin: Boolean = true,
    players: List<PlayerResult> = emptyList(),
    multiplier: Int = 1,
    beanDelta: Int = 0,
    beanBalance: Int = 0,
    onBackToHomeClick: () -> Unit = {},
    onPlayAgainClick: () -> Unit = {}
) {
    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactHeight = maxHeight < 430.dp
            val horizontalPadding = if (compactHeight) 22.dp else 30.dp
            val heroWidth = if (compactHeight) 300.dp else 330.dp
            val scoresWidth = if (compactHeight) 360.dp else 400.dp

            Column(modifier = Modifier.fillMaxSize()) {
                PokerLobbyHeader(
                    title = "牌局结算",
                    onBackClick = onBackToHomeClick,
                    trailing = {
                        BeanStatusPill(beanBalance = beanBalance, compact = true)
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = if (compactHeight) 0.dp else 4.dp,
                            bottom = if (compactHeight) 8.dp else 12.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ResultHeroSection(
                            winner = winner,
                            isLandlordWin = isLandlordWin,
                            multiplier = multiplier,
                            beanDelta = beanDelta,
                            compactHeight = compactHeight,
                            modifier = Modifier
                                .width(heroWidth)
                        )

                        Spacer(modifier = Modifier.width(if (compactHeight) 12.dp else 18.dp))

                        PlayerScoresCard(
                            players = players,
                            compactHeight = compactHeight,
                            modifier = Modifier.width(scoresWidth)
                        )
                    }
                }

                ResultActions(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = if (compactHeight) 22.dp else 30.dp
                        ),
                    compactHeight = compactHeight,
                    onBackToHomeClick = onBackToHomeClick,
                    onPlayAgainClick = onPlayAgainClick
                )
            }
        }
    }
}

@Composable
private fun ResultHeroSection(
    winner: String,
    isLandlordWin: Boolean,
    multiplier: Int,
    beanDelta: Int,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    PokerGlassPanel(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(
                    id = if (isLandlordWin) R.drawable.result_win else R.drawable.result_lose
                ),
                contentDescription = if (isLandlordWin) "胜利" else "失败",
                modifier = Modifier.height(if (compactHeight) 50.dp else 84.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(if (compactHeight) 4.dp else 10.dp))

            Text(
                text = "${winner}胜利",
                fontSize = if (compactHeight) 20.sp else 28.sp,
                fontWeight = FontWeight.Black,
                color = if (isLandlordWin) Gold500 else Color(0xFFFF6B5E),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.height(if (compactHeight) 5.dp else 10.dp))

            MultiplierCard(
                multiplier = multiplier,
                compactHeight = compactHeight
            )

            if (beanDelta != 0) {
                Spacer(modifier = Modifier.height(if (compactHeight) 5.dp else 8.dp))
                Text(
                    text = if (beanDelta > 0) "本局豆子 +${beanDelta}" else "本局豆子 ${beanDelta}",
                    color = if (beanDelta > 0) Green600 else ButtonDanger,
                    fontSize = if (compactHeight) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MultiplierCard(
    multiplier: Int,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(max = 300.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Gold500.copy(alpha = 0.32f), RoundedCornerShape(999.dp))
            .padding(horizontal = if (compactHeight) 16.dp else 24.dp, vertical = if (compactHeight) 5.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "最终倍数  ${multiplier}倍",
            color = Gold500,
            fontSize = if (compactHeight) 13.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PlayerScoresCard(
    players: List<PlayerResult>,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    PokerGlassPanel(
        modifier = modifier.widthIn(max = 520.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compactHeight) 9.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "玩家",
                    color = TextGray,
                    fontSize = if (compactHeight) 11.sp else 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "角色",
                    color = TextGray,
                    fontSize = if (compactHeight) 11.sp else 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "得分",
                    color = TextGray,
                    fontSize = if (compactHeight) 11.sp else 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 3.dp else 8.dp))

            if (players.isEmpty()) {
                Text(
                    text = "暂无结算数据",
                    color = TextWhite,
                    fontSize = if (compactHeight) 14.sp else 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                players.forEach { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (compactHeight) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = player.name,
                            color = if (player.isWinner) Gold500 else TextWhite,
                            fontSize = if (compactHeight) 12.sp else 16.sp,
                            fontWeight = if (player.isWinner) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = player.role,
                            color = if (player.role == "地主") Gold500 else Green600,
                            fontSize = if (compactHeight) 11.sp else 14.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (player.score > 0) "+${player.score}" else "${player.score}",
                            color = if (player.score > 0) Green600 else ButtonDanger,
                            fontSize = if (compactHeight) 12.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultActions(
    compactHeight: Boolean,
    onBackToHomeClick: () -> Unit,
    onPlayAgainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(if (compactHeight) 272.dp else 308.dp)
            .height(if (compactHeight) 48.dp else 55.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PokerImageButton(
            normalRes = R.drawable.btn_green,
            text = "返回主页",
            onClick = onBackToHomeClick,
            modifier = Modifier
                .width(if (compactHeight) 128.dp else 146.dp)
                .height(if (compactHeight) 48.dp else 55.dp),
            fontSize = if (compactHeight) 14.sp else 16.sp
        )

        Spacer(modifier = Modifier.width(if (compactHeight) 16.dp else 16.dp))

        PokerImageButton(
            normalRes = R.drawable.btn_orange,
            text = "再来一局",
            onClick = onPlayAgainClick,
            modifier = Modifier
                .width(if (compactHeight) 128.dp else 146.dp)
                .height(if (compactHeight) 48.dp else 55.dp),
            fontSize = if (compactHeight) 14.sp else 16.sp
        )
    }
}

@Composable
fun ResultScreenPreview() {
    val samplePlayers = listOf(
        PlayerResult("我", "地主", 300, true),
        PlayerResult("电脑1", "农民", -100, false),
        PlayerResult("电脑2", "农民", -200, false)
    )

    HappyPokerTheme {
        ResultScreen(
            winner = "地主",
            isLandlordWin = true,
            players = samplePlayers,
            multiplier = 3,
            beanDelta = 18,
            beanBalance = 1000
        )
    }
}
