package com.happy.poker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.ui.theme.*

@Composable
fun PlayPanel(
    onPlayClick: () -> Unit = {},
    onPassClick: () -> Unit = {},
    onHintClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    canPlay: Boolean = true,
    canPass: Boolean = true,
    isPlayTurn: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 提示文字
        if (isPlayTurn) {
            Text(
                text = "轮到你出牌",
                color = Gold500,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 提示按钮
            OutlinedButton(
                onClick = onHintClick,
                modifier = Modifier
                    .width(80.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextWhite
                ),
                enabled = isPlayTurn
            ) {
                Text(
                    text = "提示",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 不出按钮
            Button(
                onClick = onPassClick,
                modifier = Modifier
                    .width(80.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonSecondary,
                    contentColor = TextWhite
                ),
                enabled = canPass && isPlayTurn
            ) {
                Text(
                    text = "不出",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 出牌按钮
            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .width(80.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = CardBlack
                ),
                enabled = canPlay && isPlayTurn
            ) {
                Text(
                    text = "出牌",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PlayerInfo(
    playerName: String,
    cardCount: Int,
    role: String = "",
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 玩家名称
        Text(
            text = playerName,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        
        // 角色标签
        if (role.isNotEmpty()) {
            Card(
                modifier = Modifier.padding(top = 4.dp),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (role == "地主") Gold500 else Green600
                )
            ) {
                Text(
                    text = role,
                    color = if (role == "地主") CardBlack else TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        // 剩余牌数
        Text(
            text = "剩余: $cardCount",
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // 在线状态
        if (!isOnline) {
            Text(
                text = "离线",
                color = ButtonDanger,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun GameInfo(
    multiplier: Int = 1,
    bottomCards: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 倍数
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "倍数",
                color = TextGray,
                fontSize = 12.sp
            )
            Text(
                text = "${multiplier}x",
                color = Gold500,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 底牌数量
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "底牌",
                color = TextGray,
                fontSize = 12.sp
            )
            Text(
                text = "${bottomCards}张",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayPanelPreview() {
    HappyPokerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayPanel(isPlayTurn = true)
            Spacer(modifier = Modifier.height(16.dp))
            PlayerInfo(
                playerName = "玩家1",
                cardCount = 10,
                role = "地主"
            )
            Spacer(modifier = Modifier.height(16.dp))
            GameInfo(multiplier = 2)
        }
    }
}
