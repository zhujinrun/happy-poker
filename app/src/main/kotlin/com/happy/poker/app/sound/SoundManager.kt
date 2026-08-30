package com.happy.poker.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.happy.poker.app.R

/**
 * 音效类型
 */
enum class SoundType(val resId: Int) {
    CARD_PLAY(R.raw.card_single),       // 出牌音效
    CARD_PAIR(R.raw.card_pair),         // 对子音效
    CARD_TRIPLE(R.raw.card_triple),     // 三条音效
    CARD_STRAIGHT(R.raw.card_straight), // 顺子音效
    CARD_CONSECUTIVE_PAIRS(R.raw.card_consecutive_pairs), // 连对音效
    CARD_PLANE(R.raw.card_plane),       // 飞机音效
    CARD_TRIPLE_WITH_ONE(R.raw.voice_triple_with_one), // 三带一
    CARD_TRIPLE_WITH_PAIR(R.raw.voice_triple_with_pair), // 三带二
    CARD_FOUR_WITH_TWO(R.raw.voice_four_with_two), // 四带二
    CARD_FOUR_WITH_PAIRS(R.raw.voice_four_with_pairs), // 四带两对
    CARD_SINGLE_3(R.raw.voice_single_3),
    CARD_SINGLE_4(R.raw.voice_single_4),
    CARD_SINGLE_5(R.raw.voice_single_5),
    CARD_SINGLE_6(R.raw.voice_single_6),
    CARD_SINGLE_7(R.raw.voice_single_7),
    CARD_SINGLE_8(R.raw.voice_single_8),
    CARD_SINGLE_9(R.raw.voice_single_9),
    CARD_SINGLE_10(R.raw.voice_single_10),
    CARD_SINGLE_J(R.raw.voice_single_j),
    CARD_SINGLE_Q(R.raw.voice_single_q),
    CARD_SINGLE_K(R.raw.voice_single_k),
    CARD_SINGLE_A(R.raw.voice_single_a),
    CARD_SINGLE_2(R.raw.voice_single_2),
    CARD_SINGLE_SMALL_JOKER(R.raw.voice_single_small_joker),
    CARD_SINGLE_BIG_JOKER(R.raw.voice_single_big_joker),
    CARD_PAIR_3(R.raw.voice_pair_3),
    CARD_PAIR_4(R.raw.voice_pair_4),
    CARD_PAIR_5(R.raw.voice_pair_5),
    CARD_PAIR_6(R.raw.voice_pair_6),
    CARD_PAIR_7(R.raw.voice_pair_7),
    CARD_PAIR_8(R.raw.voice_pair_8),
    CARD_PAIR_9(R.raw.voice_pair_9),
    CARD_PAIR_10(R.raw.voice_pair_10),
    CARD_PAIR_J(R.raw.voice_pair_j),
    CARD_PAIR_Q(R.raw.voice_pair_q),
    CARD_PAIR_K(R.raw.voice_pair_k),
    CARD_PAIR_A(R.raw.voice_pair_a),
    CARD_PAIR_2(R.raw.voice_pair_2),
    CARD_TRIPLE_3(R.raw.voice_triple_3),
    CARD_TRIPLE_4(R.raw.voice_triple_4),
    CARD_TRIPLE_5(R.raw.voice_triple_5),
    CARD_TRIPLE_6(R.raw.voice_triple_6),
    CARD_TRIPLE_7(R.raw.voice_triple_7),
    CARD_TRIPLE_8(R.raw.voice_triple_8),
    CARD_TRIPLE_9(R.raw.voice_triple_9),
    CARD_TRIPLE_10(R.raw.voice_triple_10),
    CARD_TRIPLE_J(R.raw.voice_triple_j),
    CARD_TRIPLE_Q(R.raw.voice_triple_q),
    CARD_TRIPLE_K(R.raw.voice_triple_k),
    CARD_TRIPLE_A(R.raw.voice_triple_a),
    CARD_TRIPLE_2(R.raw.voice_triple_2),
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
    BUTTON_CLICK(R.raw.card_single),    // 按钮点击音效
    CARD_SELECT(R.raw.card_single)      // 选牌音效
}

/**
 * 游戏音效管理器
 */
class SoundManager(private val context: Context) {
    private companion object {
        const val TAG = "SoundManager"
    }

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundType, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private var totalLoadCount = 0
    private var completedLoadCount = 0
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
            .also { pool ->
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    onSoundLoaded(sampleId, status)
                }
            }

        loadSounds()
    }

    @Synchronized
    private fun onSoundLoaded(sampleId: Int, status: Int) {
        completedLoadCount += 1
        if (status == 0) {
            loadedSoundIds.add(sampleId)
        }
        if (totalLoadCount > 0 && completedLoadCount >= totalLoadCount) {
            isLoaded = true
        }
    }

    private fun loadSounds() {
        try {
            totalLoadCount = SoundType.entries.size
            completedLoadCount = 0
            loadedSoundIds.clear()
            soundMap.clear()
            SoundType.entries.forEach { soundType ->
                val soundId = soundPool?.load(context, soundType.resId, 1) ?: 0
                if (soundId > 0) {
                    soundMap[soundType] = soundId
                } else {
                    completedLoadCount += 1
                    Log.w(TAG, "Failed to queue sound: ${soundType.name}")
                }
            }
            if (completedLoadCount >= totalLoadCount) {
                isLoaded = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sounds", e)
            isLoaded = false
        }
    }

    /**
     * 播放音效
     */
    fun play(soundType: SoundType) {
        if (!isEnabled) return

        soundMap[soundType]?.let { soundId ->
            if (soundId !in loadedSoundIds) return
            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    /**
     * 播放音效（带音量控制）
     */
    fun play(soundType: SoundType, volume: Float) {
        if (!isEnabled) return

        soundMap[soundType]?.let { soundId ->
            if (soundId !in loadedSoundIds) return
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
