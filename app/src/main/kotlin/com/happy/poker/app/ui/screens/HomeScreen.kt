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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.happy.poker.app.settings.AppSettingsManager
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.ui.components.PokerBlueTableBackground
import com.happy.poker.app.ui.components.PokerIconButton
import com.happy.poker.app.ui.theme.Gold500
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
    val nickname = settingsManager.getNickname()
    val avatarRes = homeAvatarResourceForKey(settingsManager.getAvatarKey())

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 430.dp
        val horizontalInset = if (compactHeight) 16.dp else 24.dp

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
                .padding(horizontal = horizontalInset, vertical = if (compactHeight) 4.dp else 8.dp)
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
            text = "v1.0.0",
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
                PokerIconButton(
                    iconRes = R.drawable.poker_back_arrow,
                    onClick = onBackClick,
                    modifier = Modifier
                        .width(34.dp)
                        .height(48.dp),
                    iconSize = 34.dp,
                    contentDescription = "退出"
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.poker_back_arrow),
                    contentDescription = "返回占位",
                    modifier = Modifier
                        .width(34.dp)
                        .height(48.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            LobbyFeatureButton(label = "设置", tone = TextWhite, onClick = onSettingsClick)
            LobbyFeatureButton(label = "更多", tone = TextWhite, onClick = onSettingsClick)
        }
    }
}

@Composable
private fun LobbyFeatureButton(
    label: String,
    tone: Color,
    modifier: Modifier = Modifier,
    badge: String? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                GameAudio.buttonClick()
                onClick()
            }
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickModifier)
            .graphicsLayer {
                val pressedScale = if (isPressed) 0.95f else 1f
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .width(54.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.take(1),
                    color = tone,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                text = label,
                color = tone,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-5).dp)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4E38)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    color = TextWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "知府 I",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF2D8B74).copy(alpha = 0.84f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 13.dp, vertical = 3.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = nickname,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp)
                )
            }
            Text(
                text = "豆  1.144万",
                color = Gold500,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.26f))
                    .padding(horizontal = 18.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun LobbyBottomStatus(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(start = 8.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFA629)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "倍",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "  0",
            color = Gold500,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }
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
