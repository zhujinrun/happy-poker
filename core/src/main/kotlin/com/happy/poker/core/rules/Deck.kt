package com.happy.poker.core.rules

import com.happy.poker.core.model.Card
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit
import kotlin.random.Random

object Deck {
    private val normalRanks = listOf(
        Rank.Three, Rank.Four, Rank.Five, Rank.Six, Rank.Seven,
        Rank.Eight, Rank.Nine, Rank.Ten, Rank.Jack, Rank.Queen,
        Rank.King, Rank.Ace, Rank.Two
    )
    private val normalSuits = listOf(Suit.Spades, Suit.Hearts, Suit.Diamonds, Suit.Clubs)

    fun create(): List<Card> {
        val cards = mutableListOf<Card>()
        for (suit in normalSuits) {
            for (rank in normalRanks) {
                cards.add(Card(rank, suit))
            }
        }
        cards.add(Card(Rank.SmallJoker, Suit.Joker))
        cards.add(Card(Rank.BigJoker, Suit.Joker))
        return cards
    }

    fun shuffle(cards: List<Card>, seed: Long = System.currentTimeMillis()): List<Card> {
        val random = Random(seed)
        val shuffled = cards.toMutableList()
        for (i in shuffled.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }
        return shuffled
    }

    fun size(): Int = 54

    fun validate(cards: List<Card>): Boolean {
        if (cards.size != 54) return false
        val cardIds = cards.map { it.id }.toSet()
        val expectedIds = create().map { it.id }.toSet()
        return cardIds == expectedIds
    }
}
