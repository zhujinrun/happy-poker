package com.happy.poker.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.happy.poker.app.R

/**
 * 音效类型
 */
enum class SoundType(val resId: Int) {
    CARD_PLAY(R.raw.card_single),       // 出牌音效
    CARD_PAIR(R.raw.card_pair),         // 对子音效
    CARD_TRIPLE(R.raw.card_triple),     // 三条音效
    CARD_STRAIGHT(R.raw.card_straight), // 顺子音效
    CARD_PLANE(R.raw.card_plane),       // 飞机音效
    CARD_PASS(R.raw.pass1),             // 过牌音效
    BID_HIGH(R.raw.multiplier),         // 叫高分音效
    BID_PASS(R.raw.pass2),              // 不叫音效
    WIN(R.raw.win),                     // 胜利音效
    LOSE(R.raw.alarm),                  // 失败音效
    BOMB(R.raw.bomb),                   // 炸弹音效
    BOMB_SPECIAL(R.raw.bomb_special),   // 特殊炸弹音效
    ROCKET(R.raw.rocket),               // 火箭音效
    ROCKET_SPECIAL(R.raw.rocket_special), // 特殊火箭音效
    SPRING(R.raw.spring),               // 春天音效
    MULTIPLIER(R.raw.multiplier),       // 倍数音效
    BUTTON_CLICK(R.raw.pass1),          // 按钮点击音效
    CARD_SELECT(R.raw.card_single)      // 选牌音效
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
