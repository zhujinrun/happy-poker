package com.happy.poker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import com.happy.poker.app.ui.theme.CardBlack
import com.happy.poker.app.ui.theme.Gold500
import com.happy.poker.app.ui.theme.Green600
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite

@Composable
fun PokerScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        PokerBlueTableBackground()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color(0xFF101D35).copy(alpha = 0.44f)
                        )
                    )
                )
        )

        content()
    }
}

@Composable
fun PokerLobbyHeader(
    title: String,
    subtitle: String? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val headerHeight = if (subtitle.isNullOrBlank()) 48.dp else 56.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .padding(horizontal = 22.dp)
    ) {
        PokerIconButton(
            iconRes = R.drawable.poker_back_arrow,
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(34.dp)
                .height(48.dp),
            iconSize = 34.dp,
            contentDescription = "返回"
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 480.dp)
                .padding(top = if (subtitle.isNullOrBlank()) 7.dp else 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Gold500,
                fontSize = if (subtitle.isNullOrBlank()) 22.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextWhite.copy(alpha = 0.78f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(48.dp)
                .widthIn(min = 56.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing?.invoke()
        }
    }
}

@Composable
fun PokerGlassPanel(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.42f),
                        Color(0xFF143A7E).copy(alpha = 0.52f)
                    )
                )
            )
            .border(1.dp, TextWhite.copy(alpha = 0.16f), shape)
            .padding(12.dp),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@Composable
fun PokerStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Gold500
) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = text,
        color = if (color == Gold500) CardBlack else TextWhite,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun PokerOnlineDot(
    online: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (online) Green600 else TextGray
    Spacer(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color)
    )
}
