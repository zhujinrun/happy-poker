package com.happy.poker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import com.happy.poker.app.progress.PlayerProgressManager
import com.happy.poker.app.settings.AppSettingsManager
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.ui.components.PokerBlueTableBackground
import com.happy.poker.app.ui.components.BeanStatusPill
import com.happy.poker.app.ui.components.CounterStatusPlate
import com.happy.poker.app.ui.components.PokerBackButton
import com.happy.poker.app.ui.components.PokerBackPlaceholder
import com.happy.poker.app.ui.components.PokerSettingsButton
import com.happy.poker.app.ui.components.pokerTopHorizontalInset
import com.happy.poker.app.ui.components.pokerTopVerticalInset
import com.happy.poker.app.ui.theme.HappyPokerTheme
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite

@Composable
fun HomeScreen(
    onSinglePlayerClick: () -> Unit = {},
    onMultiplayerClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val settingsManager = remember { AppSettingsManager(context) }
    val progressManager = remember { PlayerProgressManager(context) }
    val nickname = settingsManager.getNickname()
    val avatarRes = homeAvatarResourceForKey(settingsManager.getAvatarKey())
    val beanBalance = progressManager.getBeanBalance()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 430.dp
        val horizontalInset = pokerTopHorizontalInset(compactHeight)
        val topInset = pokerTopVerticalInset(compactHeight)

        PokerBlueTableBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color(0xFF101D35).copy(alpha = 0.42f)
                        )
                    )
                )
        )

        LobbyTopChrome(
            onSettingsClick = onSettingsClick,
            onBackClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = horizontalInset, vertical = topInset)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (compactHeight) (-4).dp else 4.dp)
                .widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "欢乐斗地主",
                color = Color(0xFF233F6E).copy(alpha = 0.54f),
                fontSize = if (compactHeight) 36.sp else 46.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "经典新手场  底分20",
                color = Color(0xFF1A3157).copy(alpha = 0.62f),
                fontSize = if (compactHeight) 14.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = if (compactHeight) 38.dp else 48.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(if (compactHeight) 18.dp else 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LobbyMenuButton(
                    text = "联机模式",
                    onClick = onMultiplayerClick,
                    normalRes = R.drawable.btn_green,
                    compact = compactHeight
                )
                LobbyMenuButton(
                    text = "开始游戏",
                    onClick = onSinglePlayerClick,
                    normalRes = R.drawable.btn_orange,
                    compact = compactHeight
                )
            }
        }

        LobbyUserDock(
            nickname = nickname,
            avatarRes = avatarRes,
            beanBalance = beanBalance,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = horizontalInset, bottom = if (compactHeight) 10.dp else 14.dp)
        )

        LobbyBottomStatus(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = horizontalInset, bottom = if (compactHeight) 12.dp else 18.dp)
        )

        Text(
            text = "v1.0.2",
            color = TextGray.copy(alpha = 0.55f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (compactHeight) 8.dp else 10.dp)
        )
    }
}

@Composable
private fun LobbyTopChrome(
    onSettingsClick: () -> Unit,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onBackClick != null) {
                PokerBackButton(
                    onClick = onBackClick,
                    contentDescription = "退出"
                )
            } else {
                PokerBackPlaceholder()
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            PokerSettingsButton(
                onClick = onSettingsClick,
                contentDescription = "设置"
            )
        }
    }
}

@Composable
private fun LobbyMenuButton(
    text: String,
    onClick: () -> Unit,
    normalRes: Int,
    compact: Boolean,
    pressedRes: Int? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .width(if (compact) 150.dp else 176.dp)
            .height(if (compact) 48.dp else 58.dp)
            .scale(if (isPressed) 0.96f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    GameAudio.buttonClick()
                    onClick()
                }
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
            fontSize = if (compact) 23.sp else 28.sp,
            fontWeight = FontWeight.Black,
            color = TextWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .offset(y = if (normalRes == R.drawable.btn_orange) (-2).dp else (-1).dp)
        )
    }
}

@Composable
private fun LobbyUserDock(
    nickname: String,
    avatarRes: Int,
    beanBalance: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFEAF6FF))
                .border(3.dp, Color.White.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = "玩家头像",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .offset(y = if (avatarRes == R.drawable.avatar_daheng) (-5).dp else 1.dp),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth
            )
        }

        Column(
            modifier = Modifier.padding(bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = nickname,
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
            BeanStatusPill(beanBalance = beanBalance)
        }
    }
}

@Composable
private fun LobbyBottomStatus(modifier: Modifier = Modifier) {
    CounterStatusPlate(
        label = "倍",
        valueText = "0",
        modifier = modifier
    )
}

private fun homeAvatarResourceForKey(avatarKey: String): Int =
    when (avatarKey) {
        "daheng" -> R.drawable.avatar_daheng
        "luoli" -> R.drawable.avatar_luoli
        else -> R.drawable.avatar_yujie
    }

@Composable
fun HomeScreenPreview() {
    HappyPokerTheme {
        HomeScreen()
    }
}
