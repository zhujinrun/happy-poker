package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.ui.theme.*

@Composable
fun HomeScreen(
    onSinglePlayerClick: () -> Unit = {},
    onMultiplayerClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TableGradientStart,
                        TableGradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 游戏标题
            Text(
                text = "欢乐斗地主",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Happy Poker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = TextWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 64.dp)
            )
            
            // 单机模式按钮
            Button(
                onClick = onSinglePlayerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = CardBlack
                )
            ) {
                Text(
                    text = "单机对战",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 多人模式按钮
            Button(
                onClick = onMultiplayerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green600,
                    contentColor = TextWhite
                )
            ) {
                Text(
                    text = "多人对战",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 设置按钮
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextWhite
                )
            ) {
                Text(
                    text = "设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
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
fun HomeScreenPreview() {
    HappyPokerTheme {
        HomeScreen()
    }
}
