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
fun BidPanel(
    currentBid: Int = 0,
    onBidClick: (Int) -> Unit = {},
    onPassClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "叫地主",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // 叫分按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 不叫按钮
            Button(
                onClick = onPassClick,
                modifier = Modifier
                    .width(70.dp)
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonSecondary,
                    contentColor = TextWhite
                ),
                enabled = enabled
            ) {
                Text(
                    text = "不叫",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // 1分按钮
            if (currentBid < 1) {
                Button(
                    onClick = { onBidClick(1) },
                    modifier = Modifier
                        .width(70.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = CardBlack
                    ),
                    enabled = enabled
                ) {
                    Text(
                        text = "1分",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 2分按钮
            if (currentBid < 2) {
                Button(
                    onClick = { onBidClick(2) },
                    modifier = Modifier
                        .width(70.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = CardBlack
                    ),
                    enabled = enabled
                ) {
                    Text(
                        text = "2分",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 3分按钮
            if (currentBid < 3) {
                Button(
                    onClick = { onBidClick(3) },
                    modifier = Modifier
                        .width(70.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonDanger,
                        contentColor = TextWhite
                    ),
                    enabled = enabled
                ) {
                    Text(
                        text = "3分",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BidStatus(
    playerName: String,
    bid: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = PlayerAreaBackground
        )
    ) {
        Text(
            text = if (bid > 0) "$playerName: ${bid}分" else "$playerName: 不叫",
            color = if (bid > 0) Gold500 else TextGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun BidPanelPreview() {
    HappyPokerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BidPanel(currentBid = 1)
            Spacer(modifier = Modifier.height(16.dp))
            BidStatus(playerName = "玩家1", bid = 2)
            Spacer(modifier = Modifier.height(8.dp))
            BidStatus(playerName = "玩家2", bid = 0)
        }
    }
}
