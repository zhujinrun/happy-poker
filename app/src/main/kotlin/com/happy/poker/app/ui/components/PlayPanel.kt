package com.happy.poker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
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
        if (isPlayTurn) {
            Text(
                text = "轮到你出牌",
                color = Gold500,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokerImageButton(
                text = "提示",
                onClick = onHintClick,
                normalRes = R.drawable.btn_orange,
                enabled = isPlayTurn,
                fontSize = 14.sp,
                modifier = Modifier
                    .width(76.dp)
                    .height(36.dp)
            )
            
            PokerImageButton(
                onClick = onPassClick,
                normalRes = R.drawable.btn_pass,
                enabled = canPass && isPlayTurn,
                modifier = Modifier
                    .width(92.dp)
                    .height(36.dp),
                contentDescription = "不出"
            )
            
            PokerImageButton(
                onClick = onPlayClick,
                normalRes = R.drawable.discard,
                enabled = canPlay && isPlayTurn,
                modifier = Modifier
                    .width(92.dp)
                    .height(36.dp),
                contentDescription = "出牌"
            )
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
        CounterStatusPlate(
            label = "倍",
            valueText = multiplier.toString()
        )
        
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
