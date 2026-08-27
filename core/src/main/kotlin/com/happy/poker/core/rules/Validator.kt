package com.happy.poker.core.rules

import com.happy.poker.core.model.Card
import com.happy.poker.core.model.HandPattern
import com.happy.poker.core.model.PatternType
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.canBeat

/**
 * 牌型识别结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val pattern: HandPattern,
    val reason: String = ""
)

/**
 * 牌型识别器
 */
object Validator {
    /**
     * 识别牌型
     */
    fun identify(cards: List<Card>): ValidationResult {
        if (cards.isEmpty()) {
            return ValidationResult(false, HandPattern.Invalid, "没有选择牌")
        }

        val countByRank = cards.groupBy { it.rank }.mapValues { it.value.size }
        val totalCards = cards.size

        // 火箭（大小王）
        if (totalCards == 2 && 
            cards.any { it.rank == Rank.SmallJoker } && 
            cards.any { it.rank == Rank.BigJoker }) {
            return ValidationResult(true, HandPattern.rocket())
        }

        // 单张
        if (totalCards == 1) {
            return ValidationResult(true, HandPattern.single(cards.first().rank))
        }

        // 对子
        if (totalCards == 2 && countByRank.size == 1) {
            return ValidationResult(true, HandPattern.pair(cards.first().rank))
        }

        // 三条
        if (totalCards == 3 && countByRank.size == 1) {
            return ValidationResult(true, HandPattern.triple(cards.first().rank))
        }

        // 炸弹（4张相同点数）
        if (totalCards == 4 && countByRank.size == 1) {
            return ValidationResult(true, HandPattern.bomb(cards.first().rank))
        }

        // 三带一
        if (totalCards == 4) {
            val tripleRank = countByRank.entries.find { it.value == 3 }?.key
            if (tripleRank != null && countByRank.size == 2) {
                return ValidationResult(true, HandPattern.tripleWithOne(tripleRank))
            }
        }

        // 三带二
        if (totalCards == 5) {
            val tripleRank = countByRank.entries.find { it.value == 3 }?.key
            val pairRank = countByRank.entries.find { it.value == 2 }?.key
            if (tripleRank != null && pairRank != null && tripleRank != pairRank) {
                return ValidationResult(true, HandPattern.tripleWithPair(tripleRank))
            }
        }

        // 顺子（至少5张连续单牌）
        val straightResult = identifyStraight(cards, countByRank, totalCards)
        if (straightResult != null) {
            return straightResult
        }

        // 连对（至少3对连续对子）
        val consecutivePairsResult = identifyConsecutivePairs(cards, countByRank, totalCards)
        if (consecutivePairsResult != null) {
            return consecutivePairsResult
        }

        // 飞机（至少2个连续三条）
        val planeResult = identifyPlane(cards, countByRank, totalCards)
        if (planeResult != null) {
            return planeResult
        }

        // 四带二
        if (totalCards == 6) {
            val fourRank = countByRank.entries.find { it.value == 4 }?.key
            if (fourRank != null) {
                return ValidationResult(true, HandPattern.fourWithTwo(fourRank))
            }
        }

        // 四带两对
        if (totalCards == 8) {
            val fourRank = countByRank.entries.find { it.value == 4 }?.key
            if (fourRank != null) {
                val otherRanks = countByRank.entries.filter { it.key != fourRank }
                if (otherRanks.size == 2 && otherRanks.all { it.value == 2 }) {
                    return ValidationResult(true, HandPattern.fourWithPairs(fourRank))
                }
            }
        }

        return ValidationResult(false, HandPattern.Invalid, "无法识别牌型")
    }

    /**
     * 识别顺子
     */
    private fun identifyStraight(
        cards: List<Card>,
        countByRank: Map<Rank, Int>,
        totalCards: Int
    ): ValidationResult? {
        if (totalCards < 5) return null
        
        // 检查是否都是单张
        if (countByRank.any { it.value != 1 }) return null
        
        // 检查是否包含2或王
        if (cards.any { it.rank == Rank.Two || it.rank == Rank.SmallJoker || it.rank == Rank.BigJoker }) {
            return null
        }
        
        // 检查是否连续
        val ranks = cards.map { it.rank }.sortedBy { it.value }
        val isConsecutive = ranks.zipWithNext().all { (a, b) -> b.value == a.value + 1 }
        
        if (isConsecutive) {
            return ValidationResult(true, HandPattern.straight(ranks.first(), totalCards))
        }
        
        return null
    }

    /**
     * 识别连对
     */
    private fun identifyConsecutivePairs(
        cards: List<Card>,
        countByRank: Map<Rank, Int>,
        totalCards: Int
    ): ValidationResult? {
        if (totalCards < 6 || totalCards % 2 != 0) return null
        
        // 检查是否都是对子
        if (countByRank.any { it.value != 2 }) return null
        
        // 检查是否包含2或王
        if (cards.any { it.rank == Rank.Two || it.rank == Rank.SmallJoker || it.rank == Rank.BigJoker }) {
            return null
        }
        
        // 检查是否连续
        val ranks = countByRank.keys.sortedBy { it.value }
        val pairCount = ranks.size
        val isConsecutive = ranks.zipWithNext().all { (a, b) -> b.value == a.value + 1 }
        
        if (isConsecutive && pairCount >= 3) {
            return ValidationResult(true, HandPattern.consecutivePairs(ranks.first(), pairCount))
        }
        
        return null
    }

    /**
     * 识别飞机
     */
    private fun identifyPlane(
        cards: List<Card>,
        countByRank: Map<Rank, Int>,
        totalCards: Int
    ): ValidationResult? {
        // 找出所有三条
        val tripleRanks = countByRank.entries
            .filter { it.value >= 3 }
            .map { it.key }
            .sortedBy { it.value }
        
        if (tripleRanks.size < 2) return null
        
        // 检查是否包含2或王
        if (tripleRanks.any { it == Rank.Two }) return null
        
        // 找出连续的三条
        val consecutiveTriples = findConsecutiveTriples(tripleRanks)
        if (consecutiveTriples.isEmpty()) return null
        
        // 尝试不同的组合
        for (triple in consecutiveTriples) {
            val groupCount = triple.size
            val coreCards = groupCount * 3
            val kickerCount = totalCards - coreCards
            
            // 飞机不带翅膀
            if (kickerCount == 0) {
                return ValidationResult(true, HandPattern.plane(triple.first(), groupCount))
            }
            
            // 飞机带翅膀（每个三条带1-2张牌）
            if (kickerCount >= groupCount && kickerCount <= groupCount * 2) {
                return ValidationResult(true, HandPattern.planeWithWings(triple.first(), groupCount, kickerCount))
            }
        }
        
        return null
    }

    /**
     * 查找连续的三条
     */
    private fun findConsecutiveTriples(ranks: List<Rank>): List<List<Rank>> {
        if (ranks.size < 2) return emptyList()
        
        val result = mutableListOf<List<Rank>>()
        var current = mutableListOf(ranks.first())
        
        for (i in 1 until ranks.size) {
            if (ranks[i].value == ranks[i - 1].value + 1) {
                current.add(ranks[i])
            } else {
                if (current.size >= 2) {
                    result.add(current.toList())
                }
                current = mutableListOf(ranks[i])
            }
        }
        
        if (current.size >= 2) {
            result.add(current.toList())
        }
        
        return result
    }

    /**
     * 验证出牌是否有效
     */
    fun validatePlay(
        cards: List<Card>,
        previousPattern: HandPattern?
    ): ValidationResult {
        val result = identify(cards)
        
        if (!result.isValid) {
            return result
        }
        
        // 如果没有上家出牌，任何有效牌型都可以
        if (previousPattern == null || previousPattern == HandPattern.Invalid) {
            return result
        }
        
        // 检查是否能压过上家
        if (result.pattern.canBeat(previousPattern)) {
            return result
        }
        
        return ValidationResult(
            false,
            result.pattern,
            "牌型或点数无法压过上家"
        )
    }
}
