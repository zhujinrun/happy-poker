package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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

data class RoomInfo(
    val id: String,
    val name: String,
    val playerCount: Int,
    val maxPlayers: Int = 3,
    val state: String = "等待中"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    rooms: List<RoomInfo> = emptyList(),
    onBackClick: () -> Unit = {},
    onRoomClick: (RoomInfo) -> Unit = {},
    onCreateRoomClick: () -> Unit = {}
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
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部导航栏
            TopAppBar(
                title = {
                    Text(
                        text = "房间列表",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCreateRoomClick) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "创建房间",
                            tint = Gold500
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            // 房间列表
            if (rooms.isEmpty()) {
                // 空列表提示
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "暂无房间",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右上角创建新房间",
                            color = TextGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(rooms) { room ->
                        RoomCard(
                            room = room,
                            onClick = { onRoomClick(room) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomCard(
    room: RoomInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = PlayerAreaBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 房间信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = room.name,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "状态: ${room.state}",
                    color = TextGray,
                    fontSize = 14.sp
                )
            }
            
            // 玩家数量
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${room.playerCount}/${room.maxPlayers}",
                    color = Gold500,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "玩家",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
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
