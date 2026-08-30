package com.happy.poker.app.settings

import android.content.Context
import android.content.SharedPreferences

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_AVATAR = "avatar"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        const val DEFAULT_NICKNAME = "我"
        const val DEFAULT_AVATAR = "yujie"
        const val DEFAULT_SOUND_ENABLED = true
        const val DEFAULT_VIBRATION_ENABLED = true

        val AVATAR_OPTIONS = listOf("yujie", "daheng", "luoli")
    }

    fun getNickname(): String =
        prefs.getString(KEY_NICKNAME, DEFAULT_NICKNAME)?.ifBlank { DEFAULT_NICKNAME }
            ?: DEFAULT_NICKNAME

    fun setNickname(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname.trim()).apply()
    }

    fun getAvatarKey(): String =
        prefs.getString(KEY_AVATAR, DEFAULT_AVATAR) ?: DEFAULT_AVATAR

    fun setAvatarKey(avatarKey: String) {
        prefs.edit().putString(KEY_AVATAR, avatarKey).apply()
    }

    fun isSoundEnabled(): Boolean =
        prefs.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun isVibrationEnabled(): Boolean =
        prefs.getBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun resetExperienceDefaults() {
        prefs.edit()
            .putBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
            .putBoolean(KEY_VIBRATION_ENABLED, DEFAULT_VIBRATION_ENABLED)
            .apply()
    }
}
