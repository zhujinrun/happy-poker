package com.happy.poker.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.happy.poker.app.R
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.sound.SoundType
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite

private val PokerTopIconButtonSize = 48.dp
private val PokerTopIconSize = 36.dp

@Composable
fun PokerImageButton(
    normalRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pressedRes: Int? = null,
    text: String? = null,
    enabled: Boolean = true,
    textColor: Color = TextWhite,
    disabledTextColor: Color = TextGray,
    fontSize: TextUnit = 16.sp,
    soundType: SoundType? = null,
    contentDescription: String? = text
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hasPressedImage = pressedRes != null

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = if (enabled) 1f else 0.45f
                val pressedScale = if (isPressed && enabled) 0.96f else 1f
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    GameAudio.buttonClick()
                    soundType?.let(GameAudio::play)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = pressedRes?.takeIf { isPressed && enabled } ?: normalRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isPressed && enabled && !hasPressedImage) 0.84f else 1f
                },
            contentScale = ContentScale.FillBounds
        )

        if (text != null) {
            Text(
                text = text,
                color = if (enabled) textColor else disabledTextColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PokerIconButton(
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    iconWidth: Dp = iconSize,
    iconHeight: Dp = iconSize,
    contentDescription: String = "返回"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .graphicsLayer {
                val pressedScale = if (isPressed) 0.92f else 1f
                scaleX = pressedScale
                scaleY = pressedScale
                alpha = if (isPressed) 0.78f else 1f
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    GameAudio.buttonClick()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .width(iconWidth)
                .height(iconHeight),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun PokerBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "返回"
) {
    PokerIconButton(
        iconRes = R.drawable.poker_back_arrow,
        onClick = onClick,
        modifier = modifier
            .width(PokerTopIconButtonSize)
            .height(PokerTopIconButtonSize),
        iconWidth = PokerTopIconSize,
        iconHeight = PokerTopIconSize,
        contentDescription = contentDescription
    )
}

@Composable
fun PokerSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "设置"
) {
    PokerIconButton(
        iconRes = R.drawable.poker_setting,
        onClick = onClick,
        modifier = modifier
            .width(PokerTopIconButtonSize)
            .height(PokerTopIconButtonSize),
        iconWidth = PokerTopIconSize,
        iconHeight = PokerTopIconSize,
        contentDescription = contentDescription
    )
}

@Composable
fun PokerBackPlaceholder(
    modifier: Modifier = Modifier,
    contentDescription: String = "返回占位"
) {
    Box(
        modifier = modifier
            .width(PokerTopIconButtonSize)
            .height(PokerTopIconButtonSize),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.poker_back_arrow),
            contentDescription = contentDescription,
            modifier = Modifier
                .width(PokerTopIconSize)
                .height(PokerTopIconSize),
            contentScale = ContentScale.Fit
        )
    }
}
