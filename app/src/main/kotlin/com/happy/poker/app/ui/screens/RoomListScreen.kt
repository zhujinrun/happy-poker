package com.happy.poker.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import com.happy.poker.app.ui.components.PokerGlassPanel
import com.happy.poker.app.ui.components.PokerImageButton
import com.happy.poker.app.ui.components.PokerLobbyHeader
import com.happy.poker.app.ui.components.PokerScreenBackground
import com.happy.poker.app.ui.components.PokerStatusPill
import com.happy.poker.app.ui.theme.ButtonDanger
import com.happy.poker.app.ui.theme.CardBlack
import com.happy.poker.app.ui.theme.Gold500
import com.happy.poker.app.ui.theme.Green600
import com.happy.poker.app.ui.theme.HappyPokerTheme
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite

data class RoomInfo(
    val id: String,
    val name: String,
    val playerCount: Int,
    val maxPlayers: Int = 3,
    val state: String = "等待中"
)

@Composable
fun RoomListScreen(
    rooms: List<RoomInfo> = emptyList(),
    onBackClick: () -> Unit = {},
    onRoomClick: (RoomInfo) -> Unit = {},
    onCreateRoomClick: () -> Unit = {}
) {
    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp

            Column(modifier = Modifier.fillMaxSize()) {
                PokerLobbyHeader(
                    title = "联机大厅",
                    subtitle = "选择牌局入座，或创建新房间",
                    onBackClick = onBackClick,
                    trailing = {
                        PokerImageButton(
                            normalRes = R.drawable.btn_orange,
                            text = "创建",
                            onClick = onCreateRoomClick,
                            modifier = Modifier
                                .width(76.dp)
                                .height(36.dp),
                            fontSize = 14.sp
                        )
                    }
                )

                if (rooms.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 20.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyRoomState(
                            compact = compact,
                            onCreateRoomClick = onCreateRoomClick
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .navigationBarsPadding()
                            .padding(horizontal = if (compact) 14.dp else 20.dp),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
                        contentPadding = PaddingValues(
                            top = if (compact) 8.dp else 14.dp,
                            bottom = 22.dp
                        )
                    ) {
                        items(rooms) { room ->
                            RoomCard(
                                room = room,
                                compact = compact,
                                onClick = { onRoomClick(room) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRoomState(
    compact: Boolean,
    onCreateRoomClick: () -> Unit
) {
    PokerGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.landlord_hat_icon),
                contentDescription = null,
                modifier = Modifier.size(if (compact) 52.dp else 70.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "暂无可加入房间",
                color = Gold500,
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "开一个新牌局，等好友入座后就能开始",
                color = TextWhite.copy(alpha = 0.76f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            PokerImageButton(
                normalRes = R.drawable.btn_orange,
                text = "创建房间",
                onClick = onCreateRoomClick,
                modifier = Modifier
                    .width(132.dp)
                    .height(42.dp),
                textColor = CardBlack,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun RoomCard(
    room: RoomInfo,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val isWaiting = room.state.contains("等待")
    val isFull = room.playerCount >= room.maxPlayers
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.64f),
                        Color(0xFF123923).copy(alpha = 0.76f)
                    )
                )
            )
            .border(1.dp, Gold500.copy(alpha = if (isWaiting) 0.46f else 0.22f), shape)
            .clickable(enabled = isWaiting && !isFull, onClick = onClick)
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 10.dp else 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 44.dp else 52.dp)
                    .clip(CircleShape)
                    .background(Gold500.copy(alpha = 0.18f))
                    .border(1.dp, Gold500.copy(alpha = 0.60f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.card_back_new),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 30.dp else 34.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = room.name,
                        color = TextWhite,
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    PokerStatusPill(
                        text = when {
                            isFull -> "满员"
                            isWaiting -> "等待"
                            else -> "对局中"
                        },
                        color = when {
                            isFull -> ButtonDanger
                            isWaiting -> Gold500
                            else -> Green600
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoomSeatDots(
                        playerCount = room.playerCount,
                        maxPlayers = room.maxPlayers
                    )
                    Text(
                        text = "${room.playerCount}/${room.maxPlayers} 玩家",
                        color = TextWhite.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomSeatDots(
    playerCount: Int,
    maxPlayers: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(maxPlayers.coerceAtLeast(1)) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < playerCount) Gold500 else TextGray.copy(alpha = 0.42f)
                    )
            )
        }
    }
}

@Composable
fun RoomListScreenPreview() {
    HappyPokerTheme {
        RoomListScreen(
            rooms = listOf(
                RoomInfo("1", "欢乐房间", 2, 3, "等待中"),
                RoomInfo("2", "竞技房间", 3, 3, "游戏中"),
                RoomInfo("3", "新手房间", 1, 3, "等待中")
            )
        )
    }
}
