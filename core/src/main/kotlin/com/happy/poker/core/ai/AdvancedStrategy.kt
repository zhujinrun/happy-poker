package com.happy.poker.core.ai

import com.happy.poker.core.model.*

/**
 * 高级AI策略
 * 区分地主和农民的不同玩法
 */
class AdvancedStrategy : Strategy {

    private val landlordStrategy = LandlordStrategy()
    private val farmerStrategy = FarmerStrategy()

    override fun decideBid(hand: List<Card>, currentBid: Int, playerCount: Int): Int {
        return AiEvaluator.suggestBid(hand, currentBid)
    }

    override fun decidePlay(
        hand: List<Card>,
        lastPattern: HandPattern?,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        return AiEvaluator.suggestBestPlay(hand, lastPattern, isLandlord, landlordHandSize)
    }
}

/**
 * 地主策略
 */
class LandlordStrategy {

    fun decidePlay(hand: List<Card>, lastPattern: HandPattern?, landlordHandSize: Int): List<Card>? {
        if (hand.isEmpty()) return null
        val analysis = PatternAnalyzer.analyzeHand(hand)
        if (lastPattern == null || lastPattern == HandPattern.Invalid) {
            return decideFreePlay(hand, analysis)
        }
        return decideFollowPlay(hand, analysis, lastPattern)
    }

    private fun decideFreePlay(hand: List<Card>, analysis: HandAnalysis): List<Card>? {
        if (canWinInOnePlay(hand, analysis)) return hand
        val multiCardPatterns = listOf(
            analysis.straights.sortedByDescending { it.ranks.first().value },
            analysis.consecutivePairs.sortedByDescending { it.ranks.first().value },
            analysis.planes.sortedByDescending { it.ranks.first().value }
        ).flatten()
        if (multiCardPatterns.isNotEmpty()) {
            return convertRanksToCards(hand, multiCardPatterns.first().ranks)
        }
        if (analysis.tripleWithPairs.isNotEmpty()) {
            return convertRanksToCards(hand, analysis.tripleWithPairs.first().ranks)
        }
        if (analysis.tripleWithOnes.isNotEmpty()) {
            return convertRanksToCards(hand, analysis.tripleWithOnes.first().ranks)
        }
        if (analysis.triples.isNotEmpty()) {
            return convertRanksToCards(hand, analysis.triples.first().ranks)
        }
        if (analysis.pairs.isNotEmpty()) {
            val pair = analysis.pairs.minByOrNull { it.ranks.first().value }
            if (pair != null) return convertRanksToCards(hand, pair.ranks)
        }
        if (analysis.singles.isNotEmpty()) {
            val single = analysis.singles.minByOrNull { it.ranks.first().value }
            if (single != null) return convertRanksToCards(hand, single.ranks)
        }
        return hand.firstOrNull()?.let { listOf(it) }
    }

    private fun decideFollowPlay(hand: List<Card>, analysis: HandAnalysis, lastPattern: HandPattern): List<Card>? {
        val validPlays = findValidPlays(hand, analysis, lastPattern)
        if (validPlays.isEmpty()) return null
        if (hand.size <= 6) return validPlays.firstOrNull()
        return validPlays.firstOrNull()
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

    private fun findValidPlays(hand: List<Card>, analysis: HandAnalysis, lastPattern: HandPattern): List<List<Card>> {
        val validPlays = mutableListOf<List<Card>>()
        when (lastPattern.type) {
            PatternType.Single -> {
                for (single in analysis.singles.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, single.ranks))
                }
            }
            PatternType.Pair -> {
                for (pair in analysis.pairs.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, pair.ranks))
                }
            }
            PatternType.Triple, PatternType.TripleWithOne, PatternType.TripleWithPair -> {
                val patterns = when (lastPattern.type) {
                    PatternType.Triple -> analysis.triples
                    PatternType.TripleWithOne -> analysis.tripleWithOnes
                    PatternType.TripleWithPair -> analysis.tripleWithPairs
                    else -> emptyList()
                }
                for (pattern in patterns.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, pattern.ranks))
                }
            }
            PatternType.Straight -> {
                for (straight in analysis.straights.filter { it.ranks.size == lastPattern.cardCount && it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, straight.ranks))
                }
            }
            PatternType.ConsecutivePairs -> {
                for (pair in analysis.consecutivePairs.filter { it.ranks.size == lastPattern.cardCount && it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, pair.ranks))
                }
            }
            PatternType.Plane, PatternType.PlaneWithWings -> {
                for (plane in analysis.planes.filter { it.ranks.size == lastPattern.groupCount && it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, plane.ranks))
                }
            }
            PatternType.Bomb -> {
                for (bomb in analysis.bombs.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, bomb.ranks))
                }
                analysis.rocket?.let { validPlays.add(convertRanksToCards(hand, it.ranks)) }
            }
            else -> {}
        }
        if (lastPattern.type != PatternType.Bomb && lastPattern.type != PatternType.Rocket) {
            for (bomb in analysis.bombs) {
                validPlays.add(convertRanksToCards(hand, bomb.ranks))
            }
            analysis.rocket?.let { validPlays.add(convertRanksToCards(hand, it.ranks)) }
        }
        return validPlays
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

/**
 * 农民策略
 */
class FarmerStrategy {

    fun decidePlay(hand: List<Card>, lastPattern: HandPattern?, landlordHandSize: Int): List<Card>? {
        if (hand.isEmpty()) return null
        val analysis = PatternAnalyzer.analyzeHand(hand)
        if (lastPattern == null || lastPattern == HandPattern.Invalid) {
            return decideFreePlay(hand, analysis, landlordHandSize)
        }
        return decideFollowPlay(hand, analysis, lastPattern, landlordHandSize)
    }

    private fun decideFreePlay(hand: List<Card>, analysis: HandAnalysis, landlordHandSize: Int): List<Card>? {
        if (canWinInOnePlay(hand, analysis)) return hand
        if (landlordHandSize <= 3) {
            if (analysis.singles.isNotEmpty()) {
                val single = analysis.singles.minByOrNull { it.ranks.first().value }
                if (single != null) return convertRanksToCards(hand, single.ranks)
            }
        }
        val multiCardPatterns = listOf(analysis.straights, analysis.consecutivePairs, analysis.planes).flatten()
        if (multiCardPatterns.isNotEmpty()) {
            return convertRanksToCards(hand, multiCardPatterns.first().ranks)
        }
        if (analysis.tripleWithPairs.isNotEmpty()) {
            return convertRanksToCards(hand, analysis.tripleWithPairs.first().ranks)
        }
        if (analysis.tripleWithOnes.isNotEmpty()) {
            return convertRanksToCards(hand, analysis.tripleWithOnes.first().ranks)
        }
        if (analysis.pairs.isNotEmpty()) {
            val pair = analysis.pairs.minByOrNull { it.ranks.first().value }
            if (pair != null) return convertRanksToCards(hand, pair.ranks)
        }
        if (analysis.singles.isNotEmpty()) {
            val single = analysis.singles.minByOrNull { it.ranks.first().value }
            if (single != null) return convertRanksToCards(hand, single.ranks)
        }
        return hand.firstOrNull()?.let { listOf(it) }
    }

    private fun decideFollowPlay(hand: List<Card>, analysis: HandAnalysis, lastPattern: HandPattern, landlordHandSize: Int): List<Card>? {
        val validPlays = findValidPlays(hand, analysis, lastPattern)
        if (validPlays.isEmpty()) return null
        if (landlordHandSize <= 2) return validPlays.lastOrNull()
        return validPlays.firstOrNull()
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

    private fun findValidPlays(hand: List<Card>, analysis: HandAnalysis, lastPattern: HandPattern): List<List<Card>> {
        val validPlays = mutableListOf<List<Card>>()
        when (lastPattern.type) {
            PatternType.Single -> {
                for (single in analysis.singles.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, single.ranks))
                }
            }
            PatternType.Pair -> {
                for (pair in analysis.pairs.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, pair.ranks))
                }
            }
            PatternType.Triple, PatternType.TripleWithOne, PatternType.TripleWithPair -> {
                val patterns = when (lastPattern.type) {
                    PatternType.Triple -> analysis.triples
                    PatternType.TripleWithOne -> analysis.tripleWithOnes
                    PatternType.TripleWithPair -> analysis.tripleWithPairs
                    else -> emptyList()
                }
                for (pattern in patterns.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, pattern.ranks))
                }
            }
            PatternType.Straight -> {
                for (straight in analysis.straights.filter { it.ranks.size == lastPattern.cardCount && it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, straight.ranks))
                }
            }
            PatternType.ConsecutivePairs -> {
                for (pair in analysis.consecutivePairs.filter { it.ranks.size == lastPattern.cardCount && it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, pair.ranks))
                }
            }
            PatternType.Plane, PatternType.PlaneWithWings -> {
                for (plane in analysis.planes.filter { it.ranks.size == lastPattern.groupCount && it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, plane.ranks))
                }
            }
            PatternType.Bomb -> {
                for (bomb in analysis.bombs.filter { it.ranks.first().value > lastPattern.mainRank.value }.sortedBy { it.ranks.first().value }) {
                    validPlays.add(convertRanksToCards(hand, bomb.ranks))
                }
                analysis.rocket?.let { validPlays.add(convertRanksToCards(hand, it.ranks)) }
            }
            else -> {}
        }
        if (lastPattern.type != PatternType.Bomb && lastPattern.type != PatternType.Rocket) {
            for (bomb in analysis.bombs) {
                validPlays.add(convertRanksToCards(hand, bomb.ranks))
            }
            analysis.rocket?.let { validPlays.add(convertRanksToCards(hand, it.ranks)) }
        }
        return validPlays
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
