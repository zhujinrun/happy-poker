package com.happy.poker.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.happy.poker.app.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object RoomList : Screen("room_list")
    object CreateRoom : Screen("create_room")
    object Game : Screen("game")
    object Result : Screen("result")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
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
                    // TODO: 导航到设置页面
                }
            )
        }
        
        composable(Screen.RoomList.route) {
            RoomListScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRoomClick = { room ->
                    navController.navigate(Screen.Game.route)
                },
                onCreateRoomClick = {
                    navController.navigate(Screen.CreateRoom.route)
                }
            )
        }
        
        composable(Screen.CreateRoom.route) {
            CreateRoomScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onCreateClick = { name, maxPlayers ->
                    // TODO: 创建房间并导航到游戏
                    navController.navigate(Screen.Game.route)
                }
            )
        }
        
        composable(Screen.Game.route) {
            GameScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Result.route) {
            ResultScreen(
                onBackToHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = true
                        }
                    }
                },
                onPlayAgainClick = {
                    navController.navigate(Screen.Game.route) {
                        popUpTo(Screen.Game.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
