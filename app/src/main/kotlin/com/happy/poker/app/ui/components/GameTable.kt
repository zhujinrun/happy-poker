package com.happy.poker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.happy.poker.app.ui.theme.*

@Composable
fun GameTable(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 牌桌背景
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawTableBackground()
        }
        
        // 内容
        content()
    }
}

private fun DrawScope.drawTableBackground() {
    val width = size.width
    val height = size.height
    
    // 绘制椭圆形牌桌背景
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2E7D32),  // 深绿
                Color(0xFF388E3C),  // 中绿
                Color(0xFF43A047)   // 浅绿
            )
        ),
        topLeft = Offset(width * 0.05f, height * 0.1f),
        size = Size(width * 0.9f, height * 0.8f)
    )
    
    // 绘制边框
    drawOval(
        color = Color(0xFF1B5E20),
        topLeft = Offset(width * 0.05f, height * 0.1f),
        size = Size(width * 0.9f, height * 0.8f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
    )
    
    // 绘制中心装饰
    drawCircle(
        color = Color(0x33FFFFFF),
        radius = 50f,
        center = Offset(width / 2, height / 2)
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
            .fillMaxWidth()
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
