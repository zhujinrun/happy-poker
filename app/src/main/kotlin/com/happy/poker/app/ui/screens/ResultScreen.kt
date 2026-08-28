package com.happy.poker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.happy.poker.app.R
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
    onBackToHomeClick: () -> Unit = {},
    onPlayAgainClick: () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 520.dp
        val landscape = maxWidth > maxHeight
        val horizontalPadding = if (landscape) 28.dp else 22.dp
        val topPadding = if (compactHeight) 6.dp else 26.dp
        val actionsReserve = if (compactHeight) 118.dp else 102.dp
        val actionsBottomPadding = if (compactHeight) 42.dp else 18.dp

        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.game_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        if (landscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = actionsReserve
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ResultHeroSection(
                    winner = winner,
                    isLandlordWin = isLandlordWin,
                    multiplier = multiplier,
                    compactHeight = compactHeight,
                    modifier = Modifier.weight(0.95f)
                )

                PlayerScoresCard(
                    players = players,
                    compactHeight = compactHeight,
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxWidth()
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = horizontalPadding,
                        top = topPadding,
                        end = horizontalPadding,
                        bottom = actionsReserve
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ResultHeroSection(
                    winner = winner,
                    isLandlordWin = isLandlordWin,
                    multiplier = multiplier,
                    compactHeight = compactHeight,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(if (compactHeight) 14.dp else 24.dp))

                PlayerScoresCard(
                    players = players,
                    compactHeight = compactHeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        ResultActions(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(40f)
                .navigationBarsPadding()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = actionsBottomPadding
                ),
            compactHeight = compactHeight,
            onBackToHomeClick = onBackToHomeClick,
            onPlayAgainClick = onPlayAgainClick
        )
    }
}

@Composable
private fun ResultHeroSection(
    winner: String,
    isLandlordWin: Boolean,
    multiplier: Int,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                id = if (isLandlordWin) R.drawable.result_win else R.drawable.result_lose
            ),
            contentDescription = if (isLandlordWin) "胜利" else "失败",
            modifier = Modifier.height(if (compactHeight) 58.dp else 112.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 5.dp else 14.dp))

        Text(
            text = "${winner}胜利!",
            fontSize = if (compactHeight) 21.sp else 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLandlordWin) Gold500 else Color(0xFFFF5252),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 6.dp else 18.dp))

        MultiplierCard(
            multiplier = multiplier,
            compactHeight = compactHeight,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
        )
    }
}

@Composable
private fun MultiplierCard(
    multiplier: Int,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PlayerAreaBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (compactHeight) 7.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "最终倍数",
                color = TextGray,
                fontSize = if (compactHeight) 11.sp else 14.sp
            )
            Text(
                text = "${multiplier}x",
                color = Gold500,
                fontSize = if (compactHeight) 22.sp else 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PlayerScoresCard(
    players: List<PlayerResult>,
    compactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 520.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PlayerAreaBackground)
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
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .widthIn(max = 360.dp)
            .height(if (compactHeight) 54.dp else 62.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.72f))
            .border(1.dp, Gold500.copy(alpha = 0.45f), shape)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallMenuButton(
            text = "返回主页",
            onClick = onBackToHomeClick,
            normalRes = R.drawable.btn_blue,
            width = if (compactHeight) 108 else 132,
            height = if (compactHeight) 36 else 45
        )

        Spacer(modifier = Modifier.width(if (compactHeight) 10.dp else 16.dp))

        SmallMenuButton(
            text = "再来一局",
            onClick = onPlayAgainClick,
            normalRes = R.drawable.btn_green,
            width = if (compactHeight) 108 else 132,
            height = if (compactHeight) 36 else 45
        )
    }
}

@Composable
private fun SmallMenuButton(
    text: String,
    onClick: () -> Unit,
    normalRes: Int = R.drawable.btn_blue,
    pressedRes: Int? = null,
    width: Int = 90,
    height: Int = 45
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
            .scale(if (isPressed) 0.96f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Image(
            painter = painterResource(
                id = pressedRes?.takeIf { isPressed } ?: normalRes
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isPressed && pressedRes == null) 0.84f else 1f
                },
            contentScale = ContentScale.FillBounds
        )

        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center)
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
            multiplier = 3
        )
    }
}
