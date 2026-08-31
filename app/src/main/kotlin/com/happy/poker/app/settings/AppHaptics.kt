package com.happy.poker.app.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object AppHaptics {
    fun tap(context: Context) {
        runCatching {
            if (!AppSettingsManager(context).isVibrationEnabled()) return

            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() != true) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        18L,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18L)
            }
        }
    }
}
