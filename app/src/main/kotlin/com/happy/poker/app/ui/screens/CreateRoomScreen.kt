package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    onBackClick: () -> Unit = {},
    onCreateClick: (String, Int) -> Unit = { _, _ -> }
) {
    var roomName by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableIntStateOf(3) }
    
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
                        text = "创建房间",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            // 表单内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // 房间名称输入框
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("房间名称") },
                    placeholder = { Text("请输入房间名称") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold500,
                        unfocusedBorderColor = TextGray,
                        focusedLabelColor = Gold500,
                        unfocusedLabelColor = TextGray
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 最大玩家数选择
                Text(
                    text = "最大玩家数",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 2人房间
                    FilterChip(
                        selected = maxPlayers == 2,
                        onClick = { maxPlayers = 2 },
                        label = { Text("2人") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = CardBlack
                        )
                    )
                    
                    // 3人房间
                    FilterChip(
                        selected = maxPlayers == 3,
                        onClick = { maxPlayers = 3 },
                        label = { Text("3人") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = CardBlack
                        )
                    )
                    
                    // 4人房间
                    FilterChip(
                        selected = maxPlayers == 4,
                        onClick = { maxPlayers = 4 },
                        label = { Text("4人") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = CardBlack
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // 创建按钮
                Button(
                    onClick = { 
                        if (roomName.isNotBlank()) {
                            onCreateClick(roomName, maxPlayers)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold500,
                        contentColor = CardBlack
                    ),
                    enabled = roomName.isNotBlank()
                ) {
                    Text(
                        text = "创建房间",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CreateRoomScreenPreview() {
    HappyPokerTheme {
        CreateRoomScreen()
    }
}
