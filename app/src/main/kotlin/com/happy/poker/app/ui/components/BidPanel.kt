package com.happy.poker.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
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
        Text(
            text = if (currentBid > 0) "有人叫分，是否抢地主" else "轮到你叫地主",
            color = TextWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PokerImageButton(
                normalRes = R.drawable.btn_no_bid,
                onClick = onPassClick,
                enabled = enabled,
                modifier = Modifier
                    .width(92.dp)
                    .height(36.dp),
                contentDescription = "不叫"
            )
            
            if (currentBid < 1) {
                PokerImageButton(
                    text = "1分",
                    onClick = { onBidClick(1) },
                    normalRes = R.drawable.btn_green,
                    enabled = enabled,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .width(68.dp)
                        .height(34.dp)
                )
            }
            
            if (currentBid < 2) {
                PokerImageButton(
                    text = "2分",
                    onClick = { onBidClick(2) },
                    normalRes = R.drawable.btn_orange,
                    enabled = enabled,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .width(68.dp)
                        .height(34.dp)
                )
            }
            
            if (currentBid < 3) {
                PokerImageButton(
                    onClick = { onBidClick(3) },
                    normalRes = if (currentBid > 0) R.drawable.text_grab else R.drawable.btn_bid,
                    enabled = enabled,
                    modifier = Modifier
                        .width(92.dp)
                        .height(36.dp),
                    contentDescription = if (currentBid > 0) "抢地主" else "叫地主"
                )
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
