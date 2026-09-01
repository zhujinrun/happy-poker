package com.happy.poker.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.happy.poker.app.settings.AppSettingsManager
import com.happy.poker.app.sound.GameAudio
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)
        configurePokerWindow()
        GameAudio.init(applicationContext)
        GameAudio.setEnabled(AppSettingsManager(applicationContext).isSoundEnabled())
        setContent {
            SplashScreen(onTimeout = {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            })
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configurePokerWindow()
        }
    }
}

@Composable
private fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = "Splash",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.4f)
        )
    }
}
