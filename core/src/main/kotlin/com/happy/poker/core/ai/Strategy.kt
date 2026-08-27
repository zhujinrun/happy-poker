package com.happy.poker.core.ai

import com.happy.poker.core.model.*

/**
 * AI策略接口
 */
interface Strategy {
    /**
     * 决定是否叫地主
     * @param hand 当前手牌
     * @param currentBid 当前最高叫分
     * @param playerCount 玩家数量
     * @return 叫分 (0=不叫, 1/2/3)
     */
    fun decideBid(hand: List<Card>, currentBid: Int, playerCount: Int): Int

    /**
     * 决定出什么牌
     * @param hand 当前手牌
     * @param lastPattern 上家出的牌型
     * @param isLandlord 自己是否是地主
     * @param landlordHandSize 地主手牌数量
     * @return 要出的牌，null表示过牌
     */
    fun decidePlay(
        hand: List<Card>,
        lastPattern: HandPattern?,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>?
}

/**
 * 简单策略
 * 基于牌型分析的简单AI
 */
class SimpleStrategy : Strategy {

    override fun decideBid(hand: List<Card>, currentBid: Int, playerCount: Int): Int {
        val analysis = PatternAnalyzer.analyzeHand(hand)
        val score = evaluateHand(hand, analysis)

        return when {
            score >= 8 -> 3
            score >= 5 -> 2
            score >= 3 -> 1
            else -> 0
        }
    }

    override fun decidePlay(
        hand: List<Card>,
        lastPattern: HandPattern?,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        if (hand.isEmpty()) return null

        val analysis = PatternAnalyzer.analyzeHand(hand)

        // 没有上家出牌，自由出牌
        if (lastPattern == null || lastPattern == HandPattern.Invalid) {
            return decideFreePlay(hand, analysis, isLandlord, landlordHandSize)
        }

        // 需要跟牌
        return decideFollowPlay(hand, analysis, lastPattern, isLandlord, landlordHandSize)
    }

    /**
     * 自由出牌策略
     */
    private fun decideFreePlay(
        hand: List<Card>,
        analysis: HandAnalysis,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        // 如果只剩一手牌，直接出完
        val allPatterns = analysis.getAllFlatPatterns()
        val oneShotPatterns = allPatterns.filter { pattern ->
            pattern.ranks.size == hand.size &&
            hand.groupingBy { it.rank }.eachCount().let { counts ->
                pattern.ranks.groupingBy { it }.eachCount().all { (rank, count) ->
                    counts[rank] == count
                }
            }
        }

        if (oneShotPatterns.isNotEmpty()) {
            return hand
        }

        // 如果手牌很少，尝试出顺子或连对
        if (hand.size <= 5) {
            val singles = analysis.singles
            if (singles.size == hand.size) {
                return hand.sortedBy { it.rank.value }
            }
        }

        // 优先出顺子、连对、飞机
        val priorityPatterns = listOf(
            analysis.straights,
            analysis.consecutivePairs,
            analysis.planes,
            analysis.tripleWithOnes,
            analysis.tripleWithPairs,
            analysis.fourWithTwos
        ).flatten()

        if (priorityPatterns.isNotEmpty()) {
            val pattern = priorityPatterns.first()
            return convertRanksToCards(hand, pattern.ranks)
        }

        // 出对子
        if (analysis.pairs.isNotEmpty()) {
            val pair = analysis.pairs.first()
            return convertRanksToCards(hand, pair.ranks)
        }

        // 出单张（从小到大）
        if (analysis.singles.isNotEmpty()) {
            val single = analysis.singles.minByOrNull { it.ranks.first().value }
            if (single != null) {
                return convertRanksToCards(hand, single.ranks)
            }
        }

        return hand.firstOrNull()?.let { listOf(it) }
    }

    /**
     * 跟牌策略
     */
    private fun decideFollowPlay(
        hand: List<Card>,
        analysis: HandAnalysis,
        lastPattern: HandPattern,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        val validPlays = findValidPlays(hand, analysis, lastPattern)

        if (validPlays.isEmpty()) {
            return null // 过牌
        }

        // 如果对手快出完了，优先出大牌
        if (landlordHandSize <= 2 && !isLandlord) {
            return validPlays.lastOrNull()
        }

        // 如果自己快出完了，优先出能赢的最小牌
        if (hand.size <= 5) {
            return validPlays.firstOrNull()
        }

        // 一般情况下出最小能赢的牌
        return validPlays.firstOrNull()
    }

    /**
     * 找出所有能压过上家的牌
     */
    private fun findValidPlays(
        hand: List<Card>,
        analysis: HandAnalysis,
        lastPattern: HandPattern
    ): List<List<Card>> {
        val validPlays = mutableListOf<List<Card>>()

        when (lastPattern.type) {
            PatternType.Single -> {
                // 找出所有比上家大的单张
                val singleRanks = analysis.singles
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (single in singleRanks) {
                    validPlays.add(convertRanksToCards(hand, single.ranks))
                }
            }

            PatternType.Pair -> {
                // 找出所有比上家大的对子
                val pairRanks = analysis.pairs
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (pair in pairRanks) {
                    validPlays.add(convertRanksToCards(hand, pair.ranks))
                }
            }

            PatternType.Triple -> {
                val tripleRanks = analysis.triples
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (triple in tripleRanks) {
                    validPlays.add(convertRanksToCards(hand, triple.ranks))
                }
            }

            PatternType.TripleWithOne -> {
                val tripleOnes = analysis.tripleWithOnes
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (tripleOne in tripleOnes) {
                    validPlays.add(convertRanksToCards(hand, tripleOne.ranks))
                }
            }

            PatternType.TripleWithPair -> {
                val triplePairs = analysis.tripleWithPairs
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (triplePair in triplePairs) {
                    validPlays.add(convertRanksToCards(hand, triplePair.ranks))
                }
            }

            PatternType.Straight -> {
                val straights = analysis.straights
                    .filter {
                        it.ranks.size == lastPattern.cardCount &&
                        it.ranks.first().value > lastPattern.mainRank.value
                    }
                    .sortedBy { it.ranks.first().value }

                for (straight in straights) {
                    validPlays.add(convertRanksToCards(hand, straight.ranks))
                }
            }

            PatternType.ConsecutivePairs -> {
                val consecutivePairs = analysis.consecutivePairs
                    .filter {
                        it.ranks.size == lastPattern.cardCount &&
                        it.ranks.first().value > lastPattern.mainRank.value
                    }
                    .sortedBy { it.ranks.first().value }

                for (pair in consecutivePairs) {
                    validPlays.add(convertRanksToCards(hand, pair.ranks))
                }
            }

            PatternType.Plane, PatternType.PlaneWithWings -> {
                val planes = analysis.planes
                    .filter {
                        it.ranks.size == lastPattern.groupCount &&
                        it.ranks.first().value > lastPattern.mainRank.value
                    }
                    .sortedBy { it.ranks.first().value }

                for (plane in planes) {
                    validPlays.add(convertRanksToCards(hand, plane.ranks))
                }
            }

            PatternType.FourWithTwo -> {
                val fourWithTwos = analysis.fourWithTwos
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (fourWithTwo in fourWithTwos) {
                    validPlays.add(convertRanksToCards(hand, fourWithTwo.ranks))
                }
            }

            PatternType.Bomb -> {
                // 炸弹只能用更大的炸弹或火箭压
                val bombs = analysis.bombs
                    .filter { it.ranks.first().value > lastPattern.mainRank.value }
                    .sortedBy { it.ranks.first().value }

                for (bomb in bombs) {
                    validPlays.add(convertRanksToCards(hand, bomb.ranks))
                }

                // 火箭可以压任何炸弹
                analysis.rocket?.let {
                    validPlays.add(convertRanksToCards(hand, it.ranks))
                }
            }

            PatternType.Rocket -> {
                // 火箭是最大的，无法被压
            }

            else -> {}
        }

        // 如果不是炸弹或火箭，可以用炸弹或火箭压
        if (lastPattern.type != PatternType.Bomb && lastPattern.type != PatternType.Rocket) {
            for (bomb in analysis.bombs) {
                validPlays.add(convertRanksToCards(hand, bomb.ranks))
            }
            analysis.rocket?.let {
                validPlays.add(convertRanksToCards(hand, it.ranks))
            }
        }

        return validPlays
    }

    /**
     * 将点数转换为手牌
     */
    private fun convertRanksToCards(hand: List<Card>, ranks: List<Rank>): List<Card> {
        val cards = mutableListOf<Card>()
        val handCopy = hand.toMutableList()

        for (rank in ranks) {
            val card = handCopy.find { it.rank == rank }
            if (card != null) {
                cards.add(card)
                handCopy.remove(card)
            }
        }

        return cards
    }

    /**
     * 评估手牌强度
     */
    private fun evaluateHand(hand: List<Card>, analysis: HandAnalysis): Int {
        var score = 0

        // 大小王
        val hasSmallJoker = hand.any { it.rank == Rank.SmallJoker }
        val hasBigJoker = hand.any { it.rank == Rank.BigJoker }
        if (hasSmallJoker && hasBigJoker) score += 4
        else if (hasBigJoker) score += 2
        else if (hasSmallJoker) score += 1

        // 2的数量
        val twoCount = hand.count { it.rank == Rank.Two }
        score += twoCount

        // 炸弹
        score += analysis.bombs.size * 2

        // 三条
        score += analysis.triples.size

        // 对子
        score += analysis.pairs.size / 2

        return score
    }
}
