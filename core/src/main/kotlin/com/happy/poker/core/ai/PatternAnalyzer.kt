package com.happy.poker.core.ai

import com.happy.poker.core.model.*

/**
 * 牌型分析器
 * 分析手牌，找出所有可能的出牌组合
 */
object PatternAnalyzer {

    /**
     * 分析手牌，返回所有可能的牌型
     */
    fun analyzeHand(hand: List<Card>): HandAnalysis {
        val countByRank = hand.groupingBy { it.rank }.eachCount()
        val sortedRanks = hand.map { it.rank }.distinct().sortedBy { it.value }

        return HandAnalysis(
            singles = findSingles(countByRank),
            pairs = findPairs(countByRank),
            triples = findTriples(countByRank),
            bombs = findBombs(countByRank),
            straights = findStraights(sortedRanks, countByRank),
            consecutivePairs = findConsecutivePairs(sortedRanks, countByRank),
            planes = findPlanes(sortedRanks, countByRank),
            tripleWithOnes = findTripleWithOnes(countByRank),
            tripleWithPairs = findTripleWithPairs(countByRank),
            fourWithTwos = findFourWithTwos(countByRank),
            rocket = findRocket(hand)
        )
    }

    /**
     * 找出所有单张
     */
    private fun findSingles(countByRank: Map<Rank, Int>): List<CardPattern> {
        return countByRank.entries.flatMap { (rank, count) ->
            if (count >= 1) listOf(CardPattern(HandPattern.single(rank), listOf(rank))) else emptyList()
        }
    }

    /**
     * 找出所有对子
     */
    private fun findPairs(countByRank: Map<Rank, Int>): List<CardPattern> {
        return countByRank.entries.filter { it.value >= 2 }.map { (rank, _) ->
            CardPattern(HandPattern.pair(rank), listOf(rank, rank))
        }
    }

    /**
     * 找出所有三条
     */
    private fun findTriples(countByRank: Map<Rank, Int>): List<CardPattern> {
        return countByRank.entries.filter { it.value >= 3 }.map { (rank, _) ->
            CardPattern(HandPattern.triple(rank), listOf(rank, rank, rank))
        }
    }

    /**
     * 找出所有炸弹
     */
    private fun findBombs(countByRank: Map<Rank, Int>): List<CardPattern> {
        return countByRank.entries.filter { it.value >= 4 }.map { (rank, _) ->
            CardPattern(HandPattern.bomb(rank), listOf(rank, rank, rank, rank))
        }
    }

    /**
     * 找出所有顺子
     */
    private fun findStraights(sortedRanks: List<Rank>, countByRank: Map<Rank, Int>): List<CardPattern> {
        val results = mutableListOf<CardPattern>()
        val validRanks = sortedRanks.filter { it != Rank.Two && it != Rank.SmallJoker && it != Rank.BigJoker }

        // 尝试不同长度的顺子
        for (length in 5..12) {
            for (i in 0..validRanks.size - length) {
                val straightRanks = validRanks.subList(i, i + length)
                val isConsecutive = straightRanks.zipWithNext().all { (a, b) -> b.value == a.value + 1 }

                if (isConsecutive && straightRanks.all { countByRank[it]!! >= 1 }) {
                    results.add(CardPattern(HandPattern.straight(straightRanks.first(), length), straightRanks))
                }
            }
        }

        return results
    }

    /**
     * 找出所有连对
     */
    private fun findConsecutivePairs(sortedRanks: List<Rank>, countByRank: Map<Rank, Int>): List<CardPattern> {
        val results = mutableListOf<CardPattern>()
        val pairRanks = sortedRanks.filter { countByRank[it]!! >= 2 && it != Rank.Two && it != Rank.SmallJoker && it != Rank.BigJoker }

        // 尝试不同长度的连对（至少3对）
        for (length in 3..10) {
            for (i in 0..pairRanks.size - length) {
                val consecutiveRanks = pairRanks.subList(i, i + length)
                val isConsecutive = consecutiveRanks.zipWithNext().all { (a, b) -> b.value == a.value + 1 }

                if (isConsecutive) {
                    results.add(CardPattern(HandPattern.consecutivePairs(consecutiveRanks.first(), length), consecutiveRanks))
                }
            }
        }

        return results
    }

    /**
     * 找出所有飞机
     */
    private fun findPlanes(sortedRanks: List<Rank>, countByRank: Map<Rank, Int>): List<CardPattern> {
        val results = mutableListOf<CardPattern>()
        val tripleRanks = sortedRanks.filter { countByRank[it]!! >= 3 && it != Rank.Two }

        // 找出连续的三条
        var i = 0
        while (i < tripleRanks.size) {
            var j = i
            while (j < tripleRanks.size - 1 && tripleRanks[j + 1].value == tripleRanks[j].value + 1) {
                j++
            }

            val consecutiveTriples = tripleRanks.subList(i, j + 1)
            if (consecutiveTriples.size >= 2) {
                // 飞机不带翅膀
                results.add(CardPattern(
                    HandPattern.plane(consecutiveTriples.first(), consecutiveTriples.size),
                    consecutiveTriples
                ))

                // 飞机带单翅膀
                results.add(CardPattern(
                    HandPattern.planeWithWings(consecutiveTriples.first(), consecutiveTriples.size, consecutiveTriples.size),
                    consecutiveTriples
                ))

                // 飞机带对翅膀
                results.add(CardPattern(
                    HandPattern.planeWithWings(consecutiveTriples.first(), consecutiveTriples.size, consecutiveTriples.size * 2),
                    consecutiveTriples
                ))
            }

            i = j + 1
        }

        return results
    }

    /**
     * 找出所有三带一
     */
    private fun findTripleWithOnes(countByRank: Map<Rank, Int>): List<CardPattern> {
        val results = mutableListOf<CardPattern>()
        val tripleRanks = countByRank.entries.filter { it.value >= 3 }.map { it.key }

        for (tripleRank in tripleRanks) {
            val singleRanks = countByRank.keys.filter { it != tripleRank }
            for (singleRank in singleRanks) {
                results.add(CardPattern(
                    HandPattern.tripleWithOne(tripleRank),
                    listOf(tripleRank, tripleRank, tripleRank, singleRank)
                ))
            }
        }

        return results
    }

    /**
     * 找出所有三带二
     */
    private fun findTripleWithPairs(countByRank: Map<Rank, Int>): List<CardPattern> {
        val results = mutableListOf<CardPattern>()
        val tripleRanks = countByRank.entries.filter { it.value >= 3 }.map { it.key }
        val pairRanks = countByRank.entries.filter { it.value >= 2 }.map { it.key }

        for (tripleRank in tripleRanks) {
            for (pairRank in pairRanks) {
                if (tripleRank != pairRank) {
                    results.add(CardPattern(
                        HandPattern.tripleWithPair(tripleRank),
                        listOf(tripleRank, tripleRank, tripleRank, pairRank, pairRank)
                    ))
                }
            }
        }

        return results
    }

    /**
     * 找出所有四带二
     */
    private fun findFourWithTwos(countByRank: Map<Rank, Int>): List<CardPattern> {
        val results = mutableListOf<CardPattern>()
        val fourRanks = countByRank.entries.filter { it.value >= 4 }.map { it.key }

        for (fourRank in fourRanks) {
            val otherRanks = countByRank.keys.filter { it != fourRank }
            // 四带二单
            for (i in otherRanks.indices) {
                for (j in i + 1 until otherRanks.size) {
                    results.add(CardPattern(
                        HandPattern.fourWithTwo(fourRank),
                        listOf(fourRank, fourRank, fourRank, fourRank, otherRanks[i], otherRanks[j])
                    ))
                }
            }
        }

        return results
    }

    /**
     * 找出火箭
     */
    private fun findRocket(hand: List<Card>): CardPattern? {
        val hasSmallJoker = hand.any { it.rank == Rank.SmallJoker }
        val hasBigJoker = hand.any { it.rank == Rank.BigJoker }

        return if (hasSmallJoker && hasBigJoker) {
            CardPattern(HandPattern.rocket(), listOf(Rank.SmallJoker, Rank.BigJoker))
        } else {
            null
        }
    }
}

/**
 * 牌型组合
 */
data class CardPattern(
    val pattern: HandPattern,
    val ranks: List<Rank>
)

/**
 * 手牌分析结果
 */
data class HandAnalysis(
    val singles: List<CardPattern>,
    val pairs: List<CardPattern>,
    val triples: List<CardPattern>,
    val bombs: List<CardPattern>,
    val straights: List<CardPattern>,
    val consecutivePairs: List<CardPattern>,
    val planes: List<CardPattern>,
    val tripleWithOnes: List<CardPattern>,
    val tripleWithPairs: List<CardPattern>,
    val fourWithTwos: List<CardPattern>,
    val rocket: CardPattern?
) {
    /**
     * 获取所有可能的牌型（按类型分组）
     */
    fun getAllPatterns(): Map<PatternType, List<CardPattern>> {
        return mapOf(
            PatternType.Single to singles,
            PatternType.Pair to pairs,
            PatternType.Triple to triples,
            PatternType.Bomb to bombs,
            PatternType.Straight to straights,
            PatternType.ConsecutivePairs to consecutivePairs,
            PatternType.Plane to planes,
            PatternType.TripleWithOne to tripleWithOnes,
            PatternType.TripleWithPair to tripleWithPairs,
            PatternType.FourWithTwo to fourWithTwos
        ).filter { it.value.isNotEmpty() }
    }

    /**
     * 获取所有可能的牌型（平铺）
     */
    fun getAllFlatPatterns(): List<CardPattern> {
        val patterns = mutableListOf<CardPattern>()
        patterns.addAll(singles)
        patterns.addAll(pairs)
        patterns.addAll(triples)
        patterns.addAll(bombs)
        patterns.addAll(straights)
        patterns.addAll(consecutivePairs)
        patterns.addAll(planes)
        patterns.addAll(tripleWithOnes)
        patterns.addAll(tripleWithPairs)
        patterns.addAll(fourWithTwos)
        rocket?.let { patterns.add(it) }
        return patterns
    }
}
