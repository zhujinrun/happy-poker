package com.happy.poker.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.happy.poker.app.R
import com.happy.poker.app.ui.theme.*

@Composable
fun GameTable(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // 牌桌背景图片
        Image(
            painter = painterResource(id = R.drawable.table_bg),
            contentDescription = "Table",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 内容
        content()
    }
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
