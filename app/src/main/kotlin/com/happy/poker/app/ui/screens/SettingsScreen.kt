package com.happy.poker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.sound.GameAudio
import com.happy.poker.app.ui.theme.*
import com.happy.poker.app.network.MqttConfigManager

@Composable
fun SettingsScreen(
    mqttConfigManager: MqttConfigManager,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    var brokerUrl by remember { mutableStateOf(mqttConfigManager.getBrokerUrl()) }
    var username by remember { mutableStateOf(mqttConfigManager.getUsername()) }
    var password by remember { mutableStateOf(mqttConfigManager.getPassword()) }
    var clientIdPrefix by remember { mutableStateOf(mqttConfigManager.getClientIdPrefix()) }
    var qos by remember { mutableIntStateOf(mqttConfigManager.getQos()) }
    var connectionTimeout by remember { mutableIntStateOf(mqttConfigManager.getConnectionTimeout()) }
    var keepAliveInterval by remember { mutableIntStateOf(mqttConfigManager.getKeepAliveInterval()) }
    var autoReconnect by remember { mutableStateOf(mqttConfigManager.getAutoReconnect()) }

    var showSaveSuccess by remember { mutableStateOf(false) }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "MQTT设置",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Gold500,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Broker URL
            OutlinedTextField(
                value = brokerUrl,
                onValueChange = { brokerUrl = it },
                label = { Text("Broker地址", color = TextWhite) },
                placeholder = { Text("tcp://172.16.101.118:1883", color = TextGray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = TextGray
                )
            )

            // 用户名
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名", color = TextWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = TextGray
                )
            )

            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码", color = TextWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = TextGray
                )
            )

            // 客户端ID前缀
            OutlinedTextField(
                value = clientIdPrefix,
                onValueChange = { clientIdPrefix = it },
                label = { Text("客户端ID前缀", color = TextWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = TextGray
                )
            )

            // QoS选择
            Text(
                text = "QoS等级",
                color = TextWhite,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 1, 2).forEach { qosValue ->
                    FilterChip(
                        selected = qos == qosValue,
                        onClick = {
                            GameAudio.buttonClick()
                            qos = qosValue
                        },
                        label = { Text("QoS $qosValue") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Gold500,
                            selectedLabelColor = CardBlack
                        )
                    )
                }
            }

            // 连接超时
            OutlinedTextField(
                value = connectionTimeout.toString(),
                onValueChange = { 
                    it.toIntOrNull()?.let { value -> connectionTimeout = value }
                },
                label = { Text("连接超时(秒)", color = TextWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = TextGray
                )
            )

            // 心跳间隔
            OutlinedTextField(
                value = keepAliveInterval.toString(),
                onValueChange = { 
                    it.toIntOrNull()?.let { value -> keepAliveInterval = value }
                },
                label = { Text("心跳间隔(秒)", color = TextWhite) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = Gold500,
                    unfocusedBorderColor = TextGray
                )
            )

            // 自动重连开关
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "自动重连",
                    color = TextWhite,
                    fontSize = 16.sp
                )
                Switch(
                    checked = autoReconnect,
                    onCheckedChange = {
                        GameAudio.buttonClick()
                        autoReconnect = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold500,
                        checkedTrackColor = Gold500.copy(alpha = 0.5f)
                    )
                )
            }

            // 保存按钮
            Button(
                onClick = {
                    GameAudio.buttonClick()
                    mqttConfigManager.setBrokerUrl(brokerUrl)
                    mqttConfigManager.setUsername(username)
                    mqttConfigManager.setPassword(password)
                    mqttConfigManager.setClientIdPrefix(clientIdPrefix)
                    mqttConfigManager.setQos(qos)
                    mqttConfigManager.setConnectionTimeout(connectionTimeout)
                    mqttConfigManager.setKeepAliveInterval(keepAliveInterval)
                    mqttConfigManager.setAutoReconnect(autoReconnect)
                    showSaveSuccess = true
                    onSaveClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold500,
                    contentColor = CardBlack
                )
            ) {
                Text(
                    text = "保存设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 重置按钮
            OutlinedButton(
                onClick = {
                    GameAudio.buttonClick()
                    mqttConfigManager.resetToDefaults()
                    brokerUrl = mqttConfigManager.getBrokerUrl()
                    username = mqttConfigManager.getUsername()
                    password = mqttConfigManager.getPassword()
                    clientIdPrefix = mqttConfigManager.getClientIdPrefix()
                    qos = mqttConfigManager.getQos()
                    connectionTimeout = mqttConfigManager.getConnectionTimeout()
                    keepAliveInterval = mqttConfigManager.getKeepAliveInterval()
                    autoReconnect = mqttConfigManager.getAutoReconnect()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextWhite
                )
            ) {
                Text(
                    text = "恢复默认",
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 返回按钮
            OutlinedButton(
                onClick = {
                    GameAudio.buttonClick()
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextWhite
                )
            ) {
                Text(
                    text = "返回",
                    fontSize = 16.sp
                )
            }
        }

        // 保存成功提示
        if (showSaveSuccess) {
            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                containerColor = Green600,
                contentColor = TextWhite
            ) {
                Text("设置已保存!")
            }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showSaveSuccess = false
            }
        }
    }
}
