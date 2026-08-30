package com.happy.poker.core.model

enum class PlayerRole(val displayName: String) {
    Unknown("未知"),
    Landlord("地主"),
    Farmer("农民")
}

class Player(
    val id: String,
    var name: String,
    val isAI: Boolean = false
) {
    var role: PlayerRole = PlayerRole.Unknown
    val hand: MutableList<Card> = mutableListOf()
    var isReady: Boolean = false
    var isOnline: Boolean = true
    var score: Int = 0

    val handSize: Int get() = hand.size
    val isLandlord: Boolean get() = role == PlayerRole.Landlord
    val isFarmer: Boolean get() = role == PlayerRole.Farmer
    val hasCards: Boolean get() = hand.isNotEmpty()

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
        val sorted = hand.sortedByGameOrder()
        hand.clear()
        hand.addAll(sorted)
    }

    fun removeCards(cards: List<Card>): Boolean = hand.removeAll(cards.toSet())

    fun clearHand() { hand.clear() }

    fun getCardsByRank(rank: Rank): List<Card> = hand.filter { it.rank == rank }

    fun countByRank(rank: Rank): Int = hand.count { it.rank == rank }

    fun hasCards(cards: List<Card>): Boolean {
        val handCopy = hand.toMutableList()
        return cards.all { card -> handCopy.remove(card) }
    }

    fun distinctRanks(): List<Rank> = hand.map { it.rank }.distinct().sortedBy { it.value }

    fun countByRankMap(): Map<Rank, Int> = hand.groupingBy { it.rank }.eachCount()
}
