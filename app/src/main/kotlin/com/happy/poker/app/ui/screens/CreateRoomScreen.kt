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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.happy.poker.app.ui.theme.HappyPokerTheme
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite
import com.happy.poker.core.model.Room

private const val DEFAULT_ROOM_NAME = "欢乐房间"

@Composable
fun CreateRoomScreen(
    onBackClick: () -> Unit = {},
    feedbackMessage: String? = null,
    feedbackId: Int = 0,
    onFeedbackDismiss: () -> Unit = {},
    onCreateClick: (String, Int) -> Unit = { _, _ -> }
) {
    var roomName by remember { mutableStateOf(DEFAULT_ROOM_NAME) }
    var maxPlayers by remember { mutableIntStateOf(2) }
    var localFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var visibleFeedbackId by remember { mutableIntStateOf(0) }
    val canCreate = roomName.isNotBlank()

    LaunchedEffect(feedbackId, feedbackMessage) {
        if (feedbackMessage.isNullOrBlank()) {
            localFeedbackMessage = null
            return@LaunchedEffect
        }

        localFeedbackMessage = feedbackMessage
        visibleFeedbackId = feedbackId
        kotlinx.coroutines.delay(1800)
        if (visibleFeedbackId == feedbackId) {
            localFeedbackMessage = null
            onFeedbackDismiss()
        }
    }

    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp
            val contentLift = if (compact) 18.dp else 30.dp

            Column(modifier = Modifier.fillMaxSize()) {
                PokerLobbyHeader(
                    title = "创建房间",
                    onBackClick = onBackClick,
                    compact = compact
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(
                            start = if (compact) 22.dp else 30.dp,
                            end = if (compact) 22.dp else 30.dp,
                            top = if (compact) 0.dp else 4.dp,
                            bottom = if (compact) 12.dp else 18.dp
                        )
                ) {
                    PokerGlassPanel(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = -contentLift)
                            .widthIn(max = 500.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp)
                        ) {
                            Text(
                                text = "新牌局",
                                color = Gold500,
                                fontSize = if (compact) 18.sp else 22.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = roomName,
                                onValueChange = { roomName = it.take(18) },
                                label = { Text("房间名称") },
                                placeholder = { Text(DEFAULT_ROOM_NAME) },
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

                        }
                    }

                    CreateRoomBottomActions(
                        compact = compact,
                        canCreate = canCreate,
                        onBackClick = onBackClick,
                        onCreateClick = { onCreateClick(roomName.trim(), maxPlayers) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = contentLift)
                    )
                }
            }

            GameFeedbackToast(
                message = localFeedbackMessage,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = if (compact) 78.dp else 96.dp)
            )
        }
    }
}

@Composable
private fun CreateRoomBottomActions(
    compact: Boolean,
    canCreate: Boolean,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(if (compact) 272.dp else 308.dp)
            .height(if (compact) 48.dp else 55.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PokerImageButton(
            normalRes = R.drawable.btn_green,
            text = "返回大厅",
            onClick = onBackClick,
            modifier = Modifier
                .width(if (compact) 128.dp else 146.dp)
                .height(if (compact) 48.dp else 55.dp),
            fontSize = if (compact) 14.sp else 16.sp
        )

        Spacer(modifier = Modifier.width(16.dp))

        PokerImageButton(
            normalRes = R.drawable.btn_orange,
            text = "创建房间",
            enabled = canCreate,
            onClick = onCreateClick,
            modifier = Modifier
                .width(if (compact) 128.dp else 146.dp)
                .height(if (compact) 48.dp else 55.dp),
            textColor = CardBlack,
            fontSize = if (compact) 14.sp else 16.sp
        )
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
