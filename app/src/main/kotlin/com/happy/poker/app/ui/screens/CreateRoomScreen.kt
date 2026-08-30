package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.ui.components.PokerGlassPanel
import com.happy.poker.app.ui.components.PokerImageButton
import com.happy.poker.app.ui.components.PokerLobbyHeader
import com.happy.poker.app.ui.components.PokerScreenBackground
import com.happy.poker.app.ui.theme.CardBlack
import com.happy.poker.app.ui.theme.Gold500
import com.happy.poker.app.ui.theme.Green600
import com.happy.poker.app.ui.theme.HappyPokerTheme
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite
import com.happy.poker.core.model.Room

@Composable
fun CreateRoomScreen(
    onBackClick: () -> Unit = {},
    onCreateClick: (String, Int) -> Unit = { _, _ -> }
) {
    var roomName by remember { mutableStateOf("") }
    var maxPlayers by remember { mutableIntStateOf(3) }
    val canCreate = roomName.isNotBlank()

    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp

            Column(modifier = Modifier.fillMaxSize()) {
                PokerLobbyHeader(
                    title = "创建房间",
                    subtitle = "设置牌局人数，邀请好友入座",
                    onBackClick = onBackClick
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = if (compact) 4.dp else 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PokerGlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 540.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp)
                        ) {
                            Text(
                                text = "新牌局",
                                color = Gold500,
                                fontSize = if (compact) 20.sp else 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it.take(18) },
                                label = { Text("房间名称") },
                                placeholder = { Text("例如：欢乐房间") },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = Gold500,
                                    unfocusedBorderColor = TextWhite.copy(alpha = 0.26f),
                                    focusedLabelColor = Gold500,
                                    unfocusedLabelColor = TextGray,
                                    focusedPlaceholderColor = TextGray,
                                    unfocusedPlaceholderColor = TextGray,
                                    cursorColor = Gold500,
                                    focusedContainerColor = Color.Black.copy(alpha = 0.22f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.18f)
                                )
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "牌桌人数",
                                    color = TextWhite.copy(alpha = 0.82f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Room.SUPPORTED_PLAYER_COUNTS.forEach { size ->
                                        RoomSizeChip(
                                            text = "${size}人",
                                            selected = maxPlayers == size,
                                            onClick = { maxPlayers = size },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (maxPlayers == 3) "经典斗地主人数" else "双人联机模式",
                                color = TextWhite.copy(alpha = 0.68f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            PokerImageButton(
                                normalRes = R.drawable.btn_orange,
                                text = "创建房间",
                                enabled = canCreate,
                                onClick = { onCreateClick(roomName.trim(), maxPlayers) },
                                modifier = Modifier
                                    .width(if (compact) 150.dp else 176.dp)
                                    .height(if (compact) 42.dp else 48.dp),
                                textColor = CardBlack,
                                fontSize = if (compact) 15.sp else 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomSizeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) {
                        listOf(Gold500, Color(0xFFFFA726))
                    } else {
                        listOf(Color.Black.copy(alpha = 0.28f), Color.Black.copy(alpha = 0.38f))
                    }
                )
            )
            .border(
                width = 1.dp,
                color = if (selected) Gold500 else TextWhite.copy(alpha = 0.20f),
                shape = shape
            )
            .clickable(onClick = {
                GameAudio.buttonClick()
                onClick()
            }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) CardBlack else TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CreateRoomScreenPreview() {
    HappyPokerTheme {
        CreateRoomScreen()
    }
}
