package com.happy.poker.core.ai

import com.happy.poker.core.model.*

/**
 * AI评估器
 * 提供更精确的手牌评估和出牌决策
 */
object AiEvaluator {

    /**
     * 评估手牌强度（用于叫地主决策）
     * 返回0-100的分数
     */
    fun evaluateHandStrength(hand: List<Card>): Int {
        val analysis = PatternAnalyzer.analyzeHand(hand)
        var score = 0

        // 大小王评估
        val hasSmallJoker = hand.any { it.rank == Rank.SmallJoker }
        val hasBigJoker = hand.any { it.rank == Rank.BigJoker }
        if (hasSmallJoker && hasBigJoker) score += 15
        else if (hasBigJoker) score += 10
        else if (hasSmallJoker) score += 7

        // 2的评估
        val twoCount = hand.count { it.rank == Rank.Two }
        score += twoCount * 5

        // 火箭评估
        if (analysis.rocket != null) score += 20

        // 炸弹评估
        score += analysis.bombs.size * 10

        // 三带评估
        score += analysis.triples.size * 3
        score += analysis.tripleWithOnes.size * 4
        score += analysis.tripleWithPairs.size * 5

        // 顺子/连对评估（长度越长越好）
        score += analysis.straights.sumOf { it.ranks.size } * 2
        score += analysis.consecutivePairs.sumOf { it.ranks.size } * 2
        score += analysis.planes.sumOf { it.ranks.size } * 3

        // 对子和单张评估
        score += analysis.pairs.size
        score += analysis.singles.size / 2

        return score.coerceIn(0, 100)
    }

    /**
     * 评估一手牌的出手价值
     * 用于决定是否要出这手牌
     */
    fun evaluatePlayValue(
        cards: List<Card>,
        pattern: HandPattern,
        hand: List<Card>,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): Int {
        var value = 0

        when (pattern.type) {
            PatternType.Bomb -> value += 80
            PatternType.Rocket -> value += 100
            PatternType.Straight -> value += pattern.cardCount * 2
            PatternType.ConsecutivePairs -> value += pattern.cardCount * 2
            PatternType.Plane, PatternType.PlaneWithWings -> value += pattern.cardCount * 3
            PatternType.TripleWithPair -> value += 6
            PatternType.TripleWithOne -> value += 5
            PatternType.Triple -> value += 4
            PatternType.Pair -> value += 2
            PatternType.Single -> value += 1
            else -> {}
        }

        // 如果是地主，剩余牌数少时价值更高
        if (isLandlord && hand.size - cards.size <= 2) {
            value += 30
        }

        // 如果对手牌数少，阻止对手出牌的价值更高
        if (!isLandlord && landlordHandSize <= 3) {
            value += 20
        }

        return value
    }

    /**
     * 评估是否应该叫地主
     * 返回叫分建议 (0-3)
     */
    fun suggestBid(hand: List<Card>, currentBid: Int): Int {
        val strength = evaluateHandStrength(hand)

        return when {
            strength >= 70 -> 3
            strength >= 50 -> minOf(2, currentBid + 1)
            strength >= 30 -> minOf(1, currentBid + 1)
            else -> 0
        }
    }

    /**
     * 评估手牌的牌型组合效率
     * 返回将手牌分解为最优牌型组合的效率 (0-1)
     */
    fun evaluateCombinationEfficiency(hand: List<Card>): Float {
        if (hand.isEmpty()) return 1f

        val analysis = PatternAnalyzer.analyzeHand(hand)
        val totalCards = hand.size

        // 计算有效出牌的牌数
        var effectiveCards = 0

        // 炸弹和火箭
        effectiveCards += analysis.bombs.size * 4
        effectiveCards += analysis.rocket?.ranks?.size ?: 0

        // 三带
        effectiveCards += analysis.tripleWithPairs.size * 5
        effectiveCards += analysis.tripleWithOnes.size * 4
        effectiveCards += analysis.triples.size * 3

        // 顺子和连对
        effectiveCards += analysis.straights.sumOf { it.ranks.size }
        effectiveCards += analysis.consecutivePairs.sumOf { it.ranks.size }
        effectiveCards += analysis.planes.sumOf { it.ranks.size }

        // 对子
        effectiveCards += analysis.pairs.size * 2

        return (effectiveCards.toFloat() / totalCards).coerceIn(0f, 1f)
    }

    /**
     * 评估手牌的控制能力
     * 返回控制力分数 (0-100)
     */
    fun evaluateControlPower(hand: List<Card>): Int {
        val analysis = PatternAnalyzer.analyzeHand(hand)
        var power = 0

        // 大小王的控制力
        val hasSmallJoker = hand.any { it.rank == Rank.SmallJoker }
        val hasBigJoker = hand.any { it.rank == Rank.BigJoker }
        if (hasSmallJoker && hasBigJoker) power += 30
        else if (hasBigJoker) power += 20
        else if (hasSmallJoker) power += 15

        // 2的控制力
        val twoCount = hand.count { it.rank == Rank.Two }
        power += twoCount * 8

        // 炸弹和火箭的控制力
        power += analysis.bombs.size * 25
        power += (analysis.rocket != null).let { if (it) 40 else 0 }

        return power.coerceIn(0, 100)
    }

    /**
     * 获取手牌的最优出牌建议
     */
    fun suggestBestPlay(
        hand: List<Card>,
        lastPattern: HandPattern?,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        val analysis = PatternAnalyzer.analyzeHand(hand)

        if (lastPattern == null || lastPattern == HandPattern.Invalid) {
            // 自由出牌 - 选择价值最高的牌
            return suggestFreePlay(hand, analysis, isLandlord, landlordHandSize)
        } else {
            // 跟牌 - 选择能赢且价值最高的牌
            return suggestFollowPlay(hand, analysis, lastPattern, isLandlord, landlordHandSize)
        }
    }

    private fun suggestFreePlay(
        hand: List<Card>,
        analysis: HandAnalysis,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        if (hand.isEmpty()) return null

        // 如果能一手出完，直接出
        if (canWinInOnePlay(hand, analysis)) return hand

        // 按优先级选择出牌
        val candidates = mutableListOf<Pair<List<Card>, Int>>()

        // 顺子（优先出长顺子）
        analysis.straights.sortedByDescending { it.ranks.size }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        // 连对
        analysis.consecutivePairs.sortedByDescending { it.ranks.size }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        // 飞机
        analysis.planes.sortedByDescending { it.ranks.size }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        // 三带
        analysis.tripleWithPairs.sortedByDescending { it.ranks.first().value }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        analysis.tripleWithOnes.sortedByDescending { it.ranks.first().value }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        // 对子（优先出小对子）
        analysis.pairs.sortedBy { it.ranks.first().value }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        // 单张（优先出小单张）
        analysis.singles.sortedBy { it.ranks.first().value }.forEach { pattern ->
            val cards = convertRanksToCards(hand, pattern.ranks)
            if (cards.isNotEmpty()) {
                val value = evaluatePlayValue(cards, pattern.pattern, hand, isLandlord, landlordHandSize)
                candidates.add(Pair(cards, value))
            }
        }

        // 选择价值最高的出牌
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun suggestFollowPlay(
        hand: List<Card>,
        analysis: HandAnalysis,
        lastPattern: HandPattern,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        val validPlays = findValidPlays(hand, analysis, lastPattern)

        if (validPlays.isEmpty()) return null

        // 选择价值最高的出牌
        return validPlays.maxByOrNull { pattern ->
            evaluatePlayValue(
                convertRanksToCards(hand, pattern.ranks),
                pattern.pattern,
                hand,
                isLandlord,
                landlordHandSize
            )
        }?.let { convertRanksToCards(hand, it.ranks) }
    }

    private fun findValidPlays(
        hand: List<Card>,
        analysis: HandAnalysis,
        lastPattern: HandPattern
    ): List<CardPattern> {
        val validPlays = mutableListOf<CardPattern>()

        when (lastPattern.type) {
            PatternType.Single -> {
                validPlays.addAll(
                    analysis.singles.filter { it.ranks.first().value > lastPattern.mainRank.value }
                )
            }
            PatternType.Pair -> {
                validPlays.addAll(
                    analysis.pairs.filter { it.ranks.first().value > lastPattern.mainRank.value }
                )
            }
            PatternType.Triple, PatternType.TripleWithOne, PatternType.TripleWithPair -> {
                val patterns = when (lastPattern.type) {
                    PatternType.Triple -> analysis.triples
                    PatternType.TripleWithOne -> analysis.tripleWithOnes
                    PatternType.TripleWithPair -> analysis.tripleWithPairs
                    else -> emptyList()
                }
                validPlays.addAll(
                    patterns.filter { it.ranks.first().value > lastPattern.mainRank.value }
                )
            }
            PatternType.Straight -> {
                validPlays.addAll(
                    analysis.straights.filter {
                        it.ranks.size == lastPattern.cardCount &&
                        it.ranks.first().value > lastPattern.mainRank.value
                    }
                )
            }
            PatternType.ConsecutivePairs -> {
                validPlays.addAll(
                    analysis.consecutivePairs.filter {
                        it.ranks.size == lastPattern.cardCount &&
                        it.ranks.first().value > lastPattern.mainRank.value
                    }
                )
            }
            PatternType.Plane, PatternType.PlaneWithWings -> {
                validPlays.addAll(
                    analysis.planes.filter {
                        it.ranks.size == lastPattern.groupCount &&
                        it.ranks.first().value > lastPattern.mainRank.value
                    }
                )
            }
            PatternType.Bomb -> {
                validPlays.addAll(
                    analysis.bombs.filter { it.ranks.first().value > lastPattern.mainRank.value }
                )
                analysis.rocket?.let { validPlays.add(it) }
            }
            else -> {}
        }

        // 如果不是炸弹或火箭，可以出炸弹或火箭
        if (lastPattern.type != PatternType.Bomb && lastPattern.type != PatternType.Rocket) {
            validPlays.addAll(analysis.bombs)
            analysis.rocket?.let { validPlays.add(it) }
        }

        return validPlays
    }

    private fun canWinInOnePlay(hand: List<Card>, analysis: HandAnalysis): Boolean {
        val allPatterns = analysis.getAllFlatPatterns()
        return allPatterns.any { pattern ->
            pattern.ranks.size == hand.size &&
            hand.groupingBy { it.rank }.eachCount().let { counts ->
                pattern.ranks.groupingBy { it }.eachCount().all { (rank, count) ->
                    counts[rank] == count
                }
            }
        }
    }

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
}
