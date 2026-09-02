package com.happy.poker.app.navigation

import androidx.activity.ComponentActivity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.happy.poker.app.ui.screens.*
import com.happy.poker.app.viewmodel.GameViewModel
import com.happy.poker.app.viewmodel.MultiplayerGameViewModel
import com.happy.poker.app.network.MqttConfigManager
import com.happy.poker.app.settings.AppSettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object RoomList : Screen("room_list")
    object CreateRoom : Screen("create_room")
    object Game : Screen("game")
    object MultiplayerGame : Screen("multiplayer_game")
    object Result : Screen("result")
    object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    val context = LocalContext.current
    val activity = context.findComponentActivity()
    val mqttConfigManager = MqttConfigManager(context)
    val appSettingsManager = AppSettingsManager(context)
    val multiplayerActionScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onSinglePlayerClick = {
                    navController.navigate(Screen.Game.route)
                },
                onMultiplayerClick = {
                    navController.navigate(Screen.RoomList.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onBackClick = {
                    activity?.finish()
                }
            )
        }
        
        composable(Screen.RoomList.route) {
            val activityOwner = requireNotNull(activity) { "NavGraph requires a ComponentActivity context" }
            val multiplayerViewModel: MultiplayerGameViewModel =
                viewModel(viewModelStoreOwner = activityOwner)
            val multiplayerUiState by multiplayerViewModel.uiState.collectAsState()
            LaunchedEffect(Unit) {
                while (true) {
                    multiplayerViewModel.refreshRoomList()
                    delay(3000)
                }
            }
            RoomListScreen(
                rooms = multiplayerUiState.availableRooms.map { room ->
                    RoomInfo(
                        id = room.id,
                        name = room.name,
                        playerCount = room.playerCount,
                        maxPlayers = room.maxPlayers,
                        state = room.state.displayName
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onRoomClick = { room ->
                    multiplayerActionScope.launch {
                        if (multiplayerViewModel.joinRoom(room.id, room.maxPlayers, room.name)) {
                            navController.navigate(Screen.MultiplayerGame.route)
                        }
                    }
                },
                onCreateRoomClick = {
                    navController.navigate(Screen.CreateRoom.route)
                }
            )
        }
        
        composable(Screen.CreateRoom.route) {
            val activityOwner = requireNotNull(activity) { "NavGraph requires a ComponentActivity context" }
            val multiplayerViewModel: MultiplayerGameViewModel =
                viewModel(viewModelStoreOwner = activityOwner)
            val multiplayerUiState by multiplayerViewModel.uiState.collectAsState()
            CreateRoomScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                feedbackMessage = multiplayerUiState.feedbackMessage,
                feedbackId = multiplayerUiState.feedbackId,
                onFeedbackDismiss = {
                    multiplayerViewModel.clearError()
                },
                onCreateClick = { name, maxPlayers ->
                    multiplayerActionScope.launch {
                        if (multiplayerViewModel.createRoom(name, maxPlayers)) {
                            navController.navigate(Screen.MultiplayerGame.route)
                        }
                    }
                }
            )
        }
        
        composable(Screen.Game.route) {
            val activityOwner = requireNotNull(activity) { "NavGraph requires a ComponentActivity context" }
            val gameViewModel: GameViewModel =
                viewModel(viewModelStoreOwner = activityOwner)
            GameScreen(
                viewModel = gameViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.MultiplayerGame.route) {
            val activityOwner = requireNotNull(activity) { "NavGraph requires a ComponentActivity context" }
            val multiplayerViewModel: MultiplayerGameViewModel =
                viewModel(viewModelStoreOwner = activityOwner)
            MultiplayerGameScreen(
                viewModel = multiplayerViewModel,
                onBackClick = {
                    multiplayerViewModel.leaveRoom()
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Result.route) {
            val activityOwner = requireNotNull(activity) { "NavGraph requires a ComponentActivity context" }
            val gameViewModel: GameViewModel =
                viewModel(viewModelStoreOwner = activityOwner)
            ResultScreen(
                onBackToHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                },
                onPlayAgainClick = {
                    gameViewModel.startGame()
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                appSettingsManager = appSettingsManager,
                mqttConfigManager = mqttConfigManager,
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveClick = {
                    // 保存设置后可以显示提示
                }
            )
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
