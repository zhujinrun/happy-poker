package com.happy.poker.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.happy.poker.app.R

/**
 * 音效类型
 */
enum class SoundType(val resId: Int) {
    CARD_PLAY(R.raw.card_play),      // 出牌音效
    CARD_PASS(R.raw.card_pass),      // 过牌音效
    BID_HIGH(R.raw.bid_high),       // 叫高分音效
    BID_PASS(R.raw.bid_pass),       // 不叫音效
    WIN(R.raw.win),            // 胜利音效
    LOSE(R.raw.lose),           // 失败音效
    BOMB(R.raw.bomb),           // 炸弹音效
    ROCKET(R.raw.rocket),         // 火箭音效
    SPRING(R.raw.spring),         // 春天音效
    BUTTON_CLICK(R.raw.button_click),   // 按钮点击音效
    CARD_SELECT(R.raw.card_select)     // 选牌音效
}

/**
 * 游戏音效管理器
 */
class SoundManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private var isLoaded = false
    private var isEnabled = true

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        // 加载音效资源
        loadSounds()
    }

    private fun loadSounds() {
        try {
            SoundType.entries.forEach { soundType ->
                val soundId = soundPool?.load(context, soundType.resId, 1)
                if (soundId != null) {
                    soundMap[soundType] = soundId
                }
            }
            isLoaded = true
        } catch (e: Exception) {
            // 加载失败，使用静音模式
            isLoaded = false
        }
    }

    /**
     * 播放音效
     */
    fun play(soundType: SoundType) {
        if (!isEnabled || !isLoaded) return

        soundMap[soundType]?.let { soundId ->
            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    /**
     * 播放音效（带音量控制）
     */
    fun play(soundType: SoundType, volume: Float) {
        if (!isEnabled || !isLoaded) return

        soundMap[soundType]?.let { soundId ->
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    /**
     * 设置音效开关
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * 获取音效开关状态
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * 释放资源
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isLoaded = false
    }
}
