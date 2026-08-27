package com.happy.poker.core

import com.happy.poker.core.model.Card
import com.happy.poker.core.model.HandPattern
import com.happy.poker.core.model.PatternType
import com.happy.poker.core.model.Rank
import com.happy.poker.core.model.Suit
import com.happy.poker.core.rules.Validator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ValidatorTest {
    @Test
    fun testIdentifySingle() {
        val cards = listOf(Card(Rank.Three, Suit.Spades))
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Single, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyPair() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Pair, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyTriple() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Triple, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyBomb() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds),
            Card(Rank.Three, Suit.Clubs)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Bomb, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyRocket() {
        val cards = listOf(
            Card(Rank.SmallJoker, Suit.Joker),
            Card(Rank.BigJoker, Suit.Joker)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Rocket, result.pattern.type)
    }

    @Test
    fun testIdentifyTripleWithOne() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds),
            Card(Rank.Four, Suit.Clubs)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.TripleWithOne, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyTripleWithPair() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds),
            Card(Rank.Four, Suit.Spades),
            Card(Rank.Four, Suit.Hearts)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.TripleWithPair, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyStraight() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Four, Suit.Hearts),
            Card(Rank.Five, Suit.Diamonds),
            Card(Rank.Six, Suit.Clubs),
            Card(Rank.Seven, Suit.Spades)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Straight, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
        assertEquals(5, result.pattern.cardCount)
    }

    @Test
    fun testIdentifyConsecutivePairs() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Four, Suit.Diamonds),
            Card(Rank.Four, Suit.Clubs),
            Card(Rank.Five, Suit.Spades),
            Card(Rank.Five, Suit.Hearts)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.ConsecutivePairs, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
        assertEquals(6, result.pattern.cardCount)
    }

    @Test
    fun testIdentifyPlane() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds),
            Card(Rank.Four, Suit.Clubs),
            Card(Rank.Four, Suit.Spades),
            Card(Rank.Four, Suit.Hearts)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.Plane, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
        assertEquals(2, result.pattern.groupCount)
    }

    @Test
    fun testIdentifyFourWithTwo() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds),
            Card(Rank.Three, Suit.Clubs),
            Card(Rank.Four, Suit.Spades),
            Card(Rank.Five, Suit.Hearts)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.FourWithTwo, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testIdentifyFourWithPairs() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Three, Suit.Diamonds),
            Card(Rank.Three, Suit.Clubs),
            Card(Rank.Four, Suit.Spades),
            Card(Rank.Four, Suit.Hearts),
            Card(Rank.Five, Suit.Diamonds),
            Card(Rank.Five, Suit.Clubs)
        )
        val result = Validator.identify(cards)
        
        assertTrue(result.isValid)
        assertEquals(PatternType.FourWithPairs, result.pattern.type)
        assertEquals(Rank.Three, result.pattern.mainRank)
    }

    @Test
    fun testInvalidPattern() {
        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Four, Suit.Hearts),
            Card(Rank.Six, Suit.Diamonds)
        )
        val result = Validator.identify(cards)
        
        assertFalse(result.isValid)
    }

    @Test
    fun testValidatePlay() {
        val cards = listOf(Card(Rank.Four, Suit.Spades))
        val previousPattern = HandPattern.single(Rank.Three)
        
        val result = Validator.validatePlay(cards, previousPattern)
        assertTrue(result.isValid)
    }

    @Test
    fun testValidatePlayCannotBeat() {
        val cards = listOf(Card(Rank.Three, Suit.Spades))
        val previousPattern = HandPattern.single(Rank.Four)
        
        val result = Validator.validatePlay(cards, previousPattern)
        assertFalse(result.isValid)
    }
}
