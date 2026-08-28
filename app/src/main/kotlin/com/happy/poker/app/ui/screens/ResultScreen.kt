package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    players: List<PlayerResult> = emptyList(),
    multiplier: Int = 1,
    onBackToHomeClick: () -> Unit = {},
    onPlayAgainClick: () -> Unit = {}
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
            // 游戏结果标题
            Text(
                text = "游戏结束",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 胜利方
            Text(
                text = "${winner}胜利!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // 倍数信息
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PlayerAreaBackground
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "最终倍数",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${multiplier}x",
                        color = Gold500,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 玩家成绩列表
            if (players.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PlayerAreaBackground
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 表头
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "玩家",
                                color = TextGray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "角色",
                                color = TextGray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "得分",
                                color = TextGray,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 玩家数据
                        players.forEach { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = player.name,
                                    color = if (player.isWinner) Gold500 else TextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = if (player.isWinner) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = player.role,
                                    color = if (player.role == "地主") Gold500 else Green600,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (player.score > 0) "+${player.score}" else "${player.score}",
                                    color = if (player.score > 0) Green600 else ButtonDanger,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 返回首页按钮
                OutlinedButton(
                    onClick = onBackToHomeClick,
                    modifier = Modifier
                        .width(140.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite
                    )
                ) {
                    Text(
                        text = "返回首页",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // 再来一局按钮
                Button(
                    onClick = onPlayAgainClick,
                    modifier = Modifier
                        .width(140.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = CardBlack
                    )
                ) {
                    Text(
                        text = "再来一局",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
            players = samplePlayers,
            multiplier = 3
        )
    }
}
