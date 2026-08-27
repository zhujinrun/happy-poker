package com.happy.poker.core

import com.happy.poker.core.model.Card
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit
import com.happy.poker.core.model.sortedByGameOrder
import com.happy.poker.core.model.toCardText
import com.happy.poker.core.model.countByRank
import com.happy.poker.core.model.distinctRanks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class CardTest {
    @Test
    fun testCardCreation() {
        val card = Card(Rank.Three, Suit.Spades)
        assertEquals(Rank.Three, card.rank)
        assertEquals(Suit.Spades, card.suit)
        assertEquals("3♠", card.id)
        assertEquals("黑桃三", card.displayName)
        assertFalse(card.isJoker)
    }

    @Test
    fun testJokerCreation() {
        val smallJoker = Card(Rank.SmallJoker, Suit.Joker)
        val bigJoker = Card(Rank.BigJoker, Suit.Joker)
        
        assertEquals("小王", smallJoker.id)
        assertEquals("大王", bigJoker.id)
        assertTrue(smallJoker.isJoker)
        assertTrue(bigJoker.isJoker)
    }

    @Test
    fun testCardFromId() {
        val card3 = Card.fromId("3♠")
        assertNotNull(card3)
        assertEquals(Rank.Three, card3.rank)
        assertEquals(Suit.Spades, card3.suit)
        
        val smallJoker = Card.fromId("小王")
        assertNotNull(smallJoker)
        assertEquals(Rank.SmallJoker, smallJoker.rank)
        
        val bigJoker = Card.fromId("大王")
        assertNotNull(bigJoker)
        assertEquals(Rank.BigJoker, bigJoker.rank)
    }

    @Test
    fun testSortedByGameOrder() {
        val cards = listOf(
            Card(Rank.Ace, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Two, Suit.Diamonds),
            Card(Rank.Five, Suit.Clubs)
        )
        
        val sorted = cards.sortedByGameOrder()
        assertEquals(Rank.Three, sorted[0].rank)
        assertEquals(Rank.Five, sorted[1].rank)
        assertEquals(Rank.Ace, sorted[2].rank)
        assertEquals(Rank.Two, sorted[3].rank)
    }

    @Test
    fun testToCardText() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Four, Suit.Hearts),
            Card(Rank.Five, Suit.Diamonds)
        )
        
        assertEquals("3♠ 4♥ 5♦", cards.toCardText())
    }

    @Test
    fun testCountByRank() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Four, Suit.Diamonds)
        )
        
        val counts = cards.countByRank()
        assertEquals(2, counts[Rank.Three])
        assertEquals(1, counts[Rank.Four])
        assertEquals(null, counts[Rank.Five])
    }

    @Test
    fun testDistinctRanks() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Four, Suit.Diamonds),
            Card(Rank.Five, Suit.Clubs)
        )
        
        val ranks = cards.distinctRanks()
        assertEquals(3, ranks.size)
        assertTrue(ranks.contains(Rank.Three))
        assertTrue(ranks.contains(Rank.Four))
        assertTrue(ranks.contains(Rank.Five))
    }
}
