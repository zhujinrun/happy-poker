package com.happy.poker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.happy.poker.app.ui.theme.*

@Composable
fun GameTable(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        PokerBlueTableBackground()
        content()
    }
}

@Composable
fun PokerBlueTableBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF142A56),
                        Color(0xFF5EA2F0),
                        Color(0xFF244E91)
                    )
                )
            )
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0D1B3D).copy(alpha = 0.72f),
                        Color.Transparent,
                        Color(0xFF1C3670).copy(alpha = 0.42f),
                        Color(0xFF0B1430).copy(alpha = 0.62f)
                    )
                )
            )
            .drawBehind {
                val step = 14.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.045f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
                drawOval(
                    color = Color(0xFF0A1736).copy(alpha = 0.26f),
                    topLeft = Offset(-size.width * 0.10f, size.height * 0.78f),
                    size = androidx.compose.ui.geometry.Size(size.width * 1.20f, size.height * 0.36f)
                )
            }
    )
}

@Composable
fun PlayerArea(
    modifier: Modifier = Modifier,
    isCurrentPlayer: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isCurrentPlayer) {
                        listOf(
                            Color(0x99FFD54F),  // 金色高亮
                            Color(0x66FFD54F)
                        )
                    } else {
                        listOf(
                            PlayerAreaBackground,
                            Color(0x40000000)
                        )
                    }
                )
            )
            .padding(8.dp)
    ) {
        content()
    }
}

@Composable
fun CenterArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .padding(horizontal = 32.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}

@Composable
fun BottomArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GameTablePreview() {
    HappyPokerTheme {
        GameTable()
    }
}
