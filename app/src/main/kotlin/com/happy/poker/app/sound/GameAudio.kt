package com.happy.poker.app.sound

import android.content.Context
import com.happy.poker.app.settings.AppHaptics
import com.happy.poker.core.model.HandPattern
import com.happy.poker.core.model.PatternType
import com.happy.poker.core.model.Rank

object GameAudio {
    @Volatile
    private var soundManager: SoundManager? = null
    @Volatile
    private var appContext: Context? = null

    private val singleCardSounds = mapOf(
        Rank.Three to SoundType.CARD_SINGLE_3,
        Rank.Four to SoundType.CARD_SINGLE_4,
        Rank.Five to SoundType.CARD_SINGLE_5,
        Rank.Six to SoundType.CARD_SINGLE_6,
        Rank.Seven to SoundType.CARD_SINGLE_7,
        Rank.Eight to SoundType.CARD_SINGLE_8,
        Rank.Nine to SoundType.CARD_SINGLE_9,
        Rank.Ten to SoundType.CARD_SINGLE_10,
        Rank.Jack to SoundType.CARD_SINGLE_J,
        Rank.Queen to SoundType.CARD_SINGLE_Q,
        Rank.King to SoundType.CARD_SINGLE_K,
        Rank.Ace to SoundType.CARD_SINGLE_A,
        Rank.Two to SoundType.CARD_SINGLE_2,
        Rank.SmallJoker to SoundType.CARD_SINGLE_SMALL_JOKER,
        Rank.BigJoker to SoundType.CARD_SINGLE_BIG_JOKER
    )

    private val pairSounds = mapOf(
        Rank.Three to SoundType.CARD_PAIR_3,
        Rank.Four to SoundType.CARD_PAIR_4,
        Rank.Five to SoundType.CARD_PAIR_5,
        Rank.Six to SoundType.CARD_PAIR_6,
        Rank.Seven to SoundType.CARD_PAIR_7,
        Rank.Eight to SoundType.CARD_PAIR_8,
        Rank.Nine to SoundType.CARD_PAIR_9,
        Rank.Ten to SoundType.CARD_PAIR_10,
        Rank.Jack to SoundType.CARD_PAIR_J,
        Rank.Queen to SoundType.CARD_PAIR_Q,
        Rank.King to SoundType.CARD_PAIR_K,
        Rank.Ace to SoundType.CARD_PAIR_A,
        Rank.Two to SoundType.CARD_PAIR_2
    )

    private val tripleSounds = mapOf(
        Rank.Three to SoundType.CARD_TRIPLE_3,
        Rank.Four to SoundType.CARD_TRIPLE_4,
        Rank.Five to SoundType.CARD_TRIPLE_5,
        Rank.Six to SoundType.CARD_TRIPLE_6,
        Rank.Seven to SoundType.CARD_TRIPLE_7,
        Rank.Eight to SoundType.CARD_TRIPLE_8,
        Rank.Nine to SoundType.CARD_TRIPLE_9,
        Rank.Ten to SoundType.CARD_TRIPLE_10,
        Rank.Jack to SoundType.CARD_TRIPLE_J,
        Rank.Queen to SoundType.CARD_TRIPLE_Q,
        Rank.King to SoundType.CARD_TRIPLE_K,
        Rank.Ace to SoundType.CARD_TRIPLE_A,
        Rank.Two to SoundType.CARD_TRIPLE_2
    )

    fun init(context: Context) {
        appContext = context.applicationContext
        if (soundManager != null) return
        synchronized(this) {
            if (soundManager == null) {
                soundManager = SoundManager(context.applicationContext)
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        soundManager?.setEnabled(enabled)
    }

    fun play(soundType: SoundType) {
        runCatching {
            soundManager?.play(soundType)
        }
    }

    fun buttonClick() {
        runCatching {
            appContext?.let(AppHaptics::tap)
        }
    }

    fun cardSelect() {
        runCatching {
            appContext?.let(AppHaptics::tap)
        }
    }

    fun playBid(bid: Int, isPass: Boolean) {
        if (isPass || bid <= 0) {
            play(SoundType.BID_PASS)
        } else {
            play(SoundType.BID_HIGH)
        }
    }

    fun playPass() {
        play(SoundType.CARD_PASS)
    }

    fun playPattern(pattern: HandPattern, multiplier: Int = 1) {
        when (pattern.type) {
            PatternType.Single -> play(singleCardSounds[pattern.mainRank] ?: SoundType.CARD_PLAY)
            PatternType.Pair -> play(pairSounds[pattern.mainRank] ?: SoundType.CARD_PAIR)
            PatternType.Triple -> play(tripleSounds[pattern.mainRank] ?: SoundType.CARD_TRIPLE)
            PatternType.TripleWithOne -> play(SoundType.CARD_TRIPLE_WITH_ONE)
            PatternType.TripleWithPair -> play(SoundType.CARD_TRIPLE_WITH_PAIR)
            PatternType.FourWithTwo -> play(SoundType.CARD_FOUR_WITH_TWO)
            PatternType.FourWithPairs -> play(SoundType.CARD_FOUR_WITH_PAIRS)
            PatternType.Straight -> play(SoundType.CARD_STRAIGHT)
            PatternType.ConsecutivePairs -> play(SoundType.CARD_CONSECUTIVE_PAIRS)
            PatternType.Plane,
            PatternType.PlaneWithWings -> play(SoundType.CARD_PLANE)
            PatternType.Bomb -> play(if (multiplier > 1) SoundType.BOMB_SPECIAL else SoundType.BOMB)
            PatternType.Rocket -> play(if (multiplier > 1) SoundType.ROCKET_SPECIAL else SoundType.ROCKET)
            else -> Unit
        }
    }

    fun playSpring() {
        play(SoundType.SPRING)
    }

    fun playWin() {
        play(SoundType.WIN)
    }

    fun playLose() {
        play(SoundType.LOSE)
    }
}
