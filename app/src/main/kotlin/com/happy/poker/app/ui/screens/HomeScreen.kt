package com.happy.poker.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.ui.theme.*

@Composable
fun HomeScreen(
    onSinglePlayerClick: () -> Unit = {},
    onMultiplayerClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    // 标题动画
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.home_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 半透明遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.landlord_hat_icon),
                contentDescription = "Logo",
                modifier = Modifier
                    .height(100.dp)
                    .padding(bottom = 8.dp)
                    .scale(titleScale),
                contentScale = ContentScale.Fit
            )
            
            // 游戏标题（带动画）
            Text(
                text = "欢乐斗地主",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .scale(titleScale)
            )
            
            // 按钮一排展示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                SmallMenuButton(
                    text = "单机",
                    onClick = onSinglePlayerClick,
                    normalRes = R.drawable.btn_orange
                )
                
                SmallMenuButton(
                    text = "联机",
                    onClick = onMultiplayerClick,
                    normalRes = R.drawable.btn_orange
                )
                
                SmallMenuButton(
                    text = "设置",
                    onClick = onSettingsClick,
                    normalRes = R.drawable.btn_green
                )
            }
        }
        
        // 底部版本信息
        Text(
            text = "v1.0.0",
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun SmallMenuButton(
    text: String,
    onClick: () -> Unit,
    normalRes: Int = R.drawable.btn_blue,
    pressedRes: Int? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(40.dp)
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun HomeScreenPreview() {
    HappyPokerTheme {
        HomeScreen()
    }
}
