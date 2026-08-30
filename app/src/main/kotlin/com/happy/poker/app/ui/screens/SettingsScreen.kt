package com.happy.poker.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.happy.poker.app.R
import com.happy.poker.app.network.MqttConfigManager
import com.happy.poker.app.settings.AppSettingsManager
import com.happy.poker.app.settings.AppHaptics
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.sound.SoundType
import com.happy.poker.app.ui.components.PokerGlassPanel
import com.happy.poker.app.ui.components.PokerLobbyHeader
import com.happy.poker.app.ui.components.PokerScreenBackground
import com.happy.poker.app.ui.components.PokerStatusPill
import com.happy.poker.app.ui.theme.CardBlack
import com.happy.poker.app.ui.theme.Gold500
import com.happy.poker.app.ui.theme.Green600
import com.happy.poker.app.ui.theme.TextGray
import com.happy.poker.app.ui.theme.TextWhite

private enum class SettingsSection(
    val title: String,
    val icon: ImageVector
) {
    Profile("我的资料", Icons.Outlined.Person),
    Experience("游戏体验", Icons.Outlined.Settings),
    Network("网络配置", Icons.Outlined.Wifi)
}

@Composable
fun SettingsScreen(
    appSettingsManager: AppSettingsManager,
    mqttConfigManager: MqttConfigManager,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    var selectedSection by remember { mutableStateOf(SettingsSection.Profile) }
    var showSaved by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(showSaved) {
        if (showSaved) {
            kotlinx.coroutines.delay(1800)
            showSaved = false
        }
    }

    PokerScreenBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp
            Column(modifier = Modifier.fillMaxSize()) {
                PokerLobbyHeader(
                    title = "设置中心",
                    onBackClick = onBackClick,
                    trailing = {
                        if (showSaved) {
                            PokerStatusPill(text = "已保存", color = Green600)
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = if (compact) 12.dp else 18.dp,
                            vertical = if (compact) 8.dp else 12.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
                ) {
                    SettingsRail(
                        selectedSection = selectedSection,
                        compact = compact,
                        onSectionSelected = {
                            AppHaptics.tap(context)
                            selectedSection = it
                        },
                        modifier = Modifier
                            .width(if (compact) 156.dp else 196.dp)
                            .fillMaxHeight()
                    )

                    PokerGlassPanel(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        when (selectedSection) {
                            SettingsSection.Profile -> ProfileSettingsPage(
                                appSettingsManager = appSettingsManager,
                                compact = compact,
                                onSaved = { showSaved = true }
                            )

                            SettingsSection.Experience -> ExperienceSettingsPage(
                                appSettingsManager = appSettingsManager,
                                compact = compact,
                                onSaved = { showSaved = true }
                            )

                            SettingsSection.Network -> NetworkSettingsPage(
                                mqttConfigManager = mqttConfigManager,
                                compact = compact,
                                onSaved = {
                                    showSaved = true
                                    onSaveClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRail(
    selectedSection: SettingsSection,
    compact: Boolean,
    onSectionSelected: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    PokerGlassPanel(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
        ) {
            Text(
                text = "偏好设置",
                color = Gold500,
                fontSize = if (compact) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
            ) {
                SettingsSection.entries.forEach { section ->
                    SettingsRailItem(
                        section = section,
                        selected = section == selectedSection,
                        compact = compact,
                        onClick = { onSectionSelected(section) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRailItem(
    section: SettingsSection,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) Gold500.copy(alpha = 0.94f)
                else Color.Black.copy(alpha = 0.20f)
            )
            .border(
                width = 1.dp,
                color = if (selected) Gold500 else TextWhite.copy(alpha = 0.10f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .height(if (compact) 44.dp else 50.dp)
            .padding(
                horizontal = if (compact) 9.dp else 11.dp,
                vertical = if (compact) 7.dp else 9.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = section.title,
            tint = if (selected) CardBlack else Gold500,
            modifier = Modifier.size(if (compact) 20.dp else 22.dp)
        )
        Text(
            text = section.title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (compact) 9.dp else 11.dp),
            color = if (selected) CardBlack else TextWhite,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsPageHeader(
    eyebrow: String,
    title: String,
    description: String,
    compact: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(bottom = if (compact) 12.dp else 16.dp)
    ) {
        Text(
            text = eyebrow.uppercase(),
            color = Gold500.copy(alpha = 0.82f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = TextWhite,
            fontSize = if (compact) 21.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            color = TextWhite.copy(alpha = 0.62f),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun ProfileSettingsPage(
    appSettingsManager: AppSettingsManager,
    compact: Boolean,
    onSaved: () -> Unit
) {
    var nickname by remember { mutableStateOf(appSettingsManager.getNickname()) }
    var avatarKey by remember { mutableStateOf(appSettingsManager.getAvatarKey()) }
    val canSave = nickname.trim().isNotEmpty()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsPageHeader(
            eyebrow = "PROFILE",
            title = "我的资料",
            description = "选择一个头像，留下你在牌桌上的名字。",
            compact = compact
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 24.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileAvatar(
                    avatarKey = avatarKey,
                    compact = compact,
                    modifier = Modifier.size(if (compact) 112.dp else 140.dp)
                )
                PokerStatusPill(text = "牌桌形象", color = Gold500)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
            ) {
                SettingsTextField(
                    value = nickname,
                    onValueChange = { nickname = it.take(12) },
                    label = "昵称",
                    placeholder = "输入你的牌桌昵称",
                    singleLine = true
                )

                Text(
                    text = "选择头像",
                    color = TextWhite.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
                ) {
                    AppSettingsManager.AVATAR_OPTIONS.forEach { option ->
                        AvatarOption(
                            avatarKey = option,
                            selected = avatarKey == option,
                            compact = compact,
                            onClick = {
                                AppHaptics.tap(context)
                                avatarKey = option
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "头像和昵称保存在本机，只会用于你的牌桌展示。",
                    color = TextWhite.copy(alpha = 0.52f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            appSettingsManager.setNickname(nickname)
                            appSettingsManager.setAvatarKey(avatarKey)
                            onSaved()
                        },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold500,
                            contentColor = CardBlack,
                            disabledContainerColor = Gold500.copy(alpha = 0.30f),
                            disabledContentColor = CardBlack.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 42.dp else 46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "保存资料",
                            fontSize = if (compact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            nickname = AppSettingsManager.DEFAULT_NICKNAME
                            avatarKey = AppSettingsManager.DEFAULT_AVATAR
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            TextWhite.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 42.dp else 46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "恢复默认",
                            fontSize = if (compact) 13.sp else 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperienceSettingsPage(
    appSettingsManager: AppSettingsManager,
    compact: Boolean,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var soundEnabled by remember { mutableStateOf(appSettingsManager.isSoundEnabled()) }
    var vibrationEnabled by remember { mutableStateOf(appSettingsManager.isVibrationEnabled()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsPageHeader(
            eyebrow = "EXPERIENCE",
            title = "游戏体验",
            description = "让每次出牌都有恰到好处的反馈。",
            compact = compact
        )

        SettingsToggleRow(
            icon = Icons.Outlined.VolumeUp,
            title = "音效",
            description = "播放出牌、叫分和结果音效",
            checked = soundEnabled,
            onCheckedChange = {
                soundEnabled = it
                appSettingsManager.setSoundEnabled(it)
                GameAudio.setEnabled(it)
                onSaved()
            }
        )
        HorizontalDivider(color = TextWhite.copy(alpha = 0.10f))
        SettingsToggleRow(
            icon = Icons.Outlined.Vibration,
            title = "震动",
            description = "操作时提供轻微触感反馈",
            checked = vibrationEnabled,
            onCheckedChange = {
                vibrationEnabled = it
                appSettingsManager.setVibrationEnabled(it)
                if (it) vibrate(context)
                onSaved()
            }
        )

        Spacer(modifier = Modifier.height(if (compact) 14.dp else 20.dp))
        Text(
            text = "快速试听",
            color = TextWhite.copy(alpha = 0.82f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.20f))
                .border(1.dp, TextWhite.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.VolumeUp,
                contentDescription = null,
                tint = Gold500,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = "牌面语音",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (soundEnabled) "试听一段单牌播报" else "音效已关闭",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = {
                    if (soundEnabled) {
                        GameAudio.play(SoundType.CARD_SINGLE_3)
                    }
                },
                enabled = soundEnabled,
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (soundEnabled) Gold500 else TextGray.copy(alpha = 0.24f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = "试听牌面语音",
                    tint = if (soundEnabled) CardBlack else TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(if (compact) 16.dp else 24.dp))
        OutlinedButton(
            onClick = {
                appSettingsManager.resetExperienceDefaults()
                soundEnabled = AppSettingsManager.DEFAULT_SOUND_ENABLED
                vibrationEnabled = AppSettingsManager.DEFAULT_VIBRATION_ENABLED
                GameAudio.setEnabled(soundEnabled)
                onSaved()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, TextWhite.copy(alpha = 0.22f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 42.dp else 46.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(text = "恢复体验默认值", fontSize = if (compact) 13.sp else 14.sp)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) Gold500 else TextGray,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = TextGray,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CardBlack,
                checkedTrackColor = Gold500,
                uncheckedThumbColor = TextWhite,
                uncheckedTrackColor = TextWhite.copy(alpha = 0.18f),
                uncheckedBorderColor = TextWhite.copy(alpha = 0.28f)
            )
        )
    }
}

@Composable
private fun NetworkSettingsPage(
    mqttConfigManager: MqttConfigManager,
    compact: Boolean,
    onSaved: () -> Unit
) {
    var brokerUrl by remember { mutableStateOf(mqttConfigManager.getBrokerUrl()) }
    var username by remember { mutableStateOf(mqttConfigManager.getUsername()) }
    var password by remember { mutableStateOf(mqttConfigManager.getPassword()) }
    var clientIdPrefix by remember { mutableStateOf(mqttConfigManager.getClientIdPrefix()) }
    var qos by remember { mutableIntStateOf(mqttConfigManager.getQos()) }
    var connectionTimeout by remember {
        mutableStateOf(mqttConfigManager.getConnectionTimeout().toString())
    }
    var keepAliveInterval by remember {
        mutableStateOf(mqttConfigManager.getKeepAliveInterval().toString())
    }
    var autoReconnect by remember { mutableStateOf(mqttConfigManager.getAutoReconnect()) }

    fun resetNetwork() {
        mqttConfigManager.resetToDefaults()
        brokerUrl = mqttConfigManager.getBrokerUrl()
        username = mqttConfigManager.getUsername()
        password = mqttConfigManager.getPassword()
        clientIdPrefix = mqttConfigManager.getClientIdPrefix()
        qos = mqttConfigManager.getQos()
        connectionTimeout = mqttConfigManager.getConnectionTimeout().toString()
        keepAliveInterval = mqttConfigManager.getKeepAliveInterval().toString()
        autoReconnect = mqttConfigManager.getAutoReconnect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsPageHeader(
            eyebrow = "NETWORK",
            title = "网络配置",
            description = "配置联机大厅使用的 MQTT 服务器。",
            compact = compact
        )

        SettingsTextField(
            value = brokerUrl,
            onValueChange = { brokerUrl = it },
            label = "Broker 地址",
            placeholder = MqttConfigManager.DEFAULT_BROKER_URL,
            singleLine = true
        )
        SettingsTextField(
            value = username,
            onValueChange = { username = it },
            label = "用户名",
            singleLine = true
        )
        SettingsTextField(
            value = password,
            onValueChange = { password = it },
            label = "密码",
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        SettingsTextField(
            value = clientIdPrefix,
            onValueChange = { clientIdPrefix = it },
            label = "客户端 ID 前缀",
            singleLine = true
        )

        Text(
            text = "服务质量",
            color = TextWhite.copy(alpha = 0.82f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 1, 2).forEach { qosValue ->
                QosOption(
                    value = qosValue,
                    selected = qos == qosValue,
                    onClick = { qos = qosValue },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingsTextField(
                value = connectionTimeout,
                onValueChange = { value ->
                    if (value.all(Char::isDigit)) connectionTimeout = value
                },
                label = "连接超时（秒）",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            SettingsTextField(
                value = keepAliveInterval,
                onValueChange = { value ->
                    if (value.all(Char::isDigit)) keepAliveInterval = value
                },
                label = "心跳间隔（秒）",
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        SettingsToggleRow(
            icon = Icons.Outlined.Wifi,
            title = "自动重连",
            description = "网络波动后自动尝试恢复连接",
            checked = autoReconnect,
            onCheckedChange = { autoReconnect = it }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    mqttConfigManager.setBrokerUrl(brokerUrl.trim())
                    mqttConfigManager.setUsername(username.trim())
                    mqttConfigManager.setPassword(password)
                    mqttConfigManager.setClientIdPrefix(clientIdPrefix.trim())
                    mqttConfigManager.setQos(qos)
                    mqttConfigManager.setConnectionTimeout(
                        connectionTimeout.toIntOrNull()?.coerceIn(1, 120) ?: MqttConfigManager.DEFAULT_CONNECTION_TIMEOUT
                    )
                    mqttConfigManager.setKeepAliveInterval(
                        keepAliveInterval.toIntOrNull()?.coerceIn(5, 300) ?: MqttConfigManager.DEFAULT_KEEP_ALIVE_INTERVAL
                    )
                    mqttConfigManager.setAutoReconnect(autoReconnect)
                    onSaved()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = CardBlack
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 42.dp else 46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(text = "保存配置", fontSize = if (compact) 13.sp else 14.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { resetNetwork() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, TextWhite.copy(alpha = 0.22f)),
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 42.dp else 46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(text = "恢复默认", fontSize = if (compact) 13.sp else 14.sp)
            }
        }
    }
}

@Composable
private fun QosOption(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) Gold500 else Color.Black.copy(alpha = 0.20f))
            .border(
                1.dp,
                if (selected) Gold500 else TextWhite.copy(alpha = 0.18f),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "QoS $value",
            color = if (selected) CardBlack else TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            focusedBorderColor = Gold500,
            unfocusedBorderColor = TextWhite.copy(alpha = 0.22f),
            focusedLabelColor = Gold500,
            unfocusedLabelColor = TextGray,
            focusedPlaceholderColor = TextGray,
            unfocusedPlaceholderColor = TextGray,
            cursorColor = Gold500,
            focusedContainerColor = Color.Black.copy(alpha = 0.22f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.18f)
        )
    )
}

@Composable
private fun ProfileAvatar(
    avatarKey: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF1E3D2B))
            .border(3.dp, Gold500.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(id = avatarResource(avatarKey)),
            contentDescription = "当前头像",
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 142.dp else 176.dp)
                .offset(y = if (avatarKey == "daheng") (-2).dp else 3.dp),
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
private fun AvatarOption(
    avatarKey: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) Gold500.copy(alpha = 0.18f)
                else Color.Black.copy(alpha = 0.16f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Gold500 else TextWhite.copy(alpha = 0.14f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = if (compact) 7.dp else 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        ProfileAvatar(
            avatarKey = avatarKey,
            compact = true,
            modifier = Modifier.size(if (compact) 48.dp else 58.dp)
        )
        Text(
            text = avatarDisplayName(avatarKey),
            color = if (selected) Gold500 else TextWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun avatarResource(avatarKey: String): Int =
    when (avatarKey) {
        "daheng" -> R.drawable.avatar_daheng
        "luoli" -> R.drawable.avatar_luoli
        else -> R.drawable.avatar_yujie
    }

private fun avatarDisplayName(avatarKey: String): String =
    when (avatarKey) {
        "daheng" -> "大亨"
        "luoli" -> "萝莉"
        else -> "御姐"
    }

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java)
    if (vibrator?.hasVibrator() != true) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                28L,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(28L)
    }
}
