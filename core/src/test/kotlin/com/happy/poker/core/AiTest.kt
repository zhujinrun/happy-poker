package com.happy.poker.core

import com.happy.poker.core.ai.AiManager
import com.happy.poker.core.ai.AiPlayer
import com.happy.poker.core.ai.PatternAnalyzer
import com.happy.poker.core.ai.SimpleStrategy
import com.happy.poker.core.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiTest {
    private lateinit var strategy: SimpleStrategy

    @BeforeEach
    fun setup() {
        strategy = SimpleStrategy()
    }

    @Test
    fun testAnalyzeHand() {
        val hand = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Four, Suit.Diamonds),
            Card(Rank.Four, Suit.Clubs),
            Card(Rank.Five, Suit.Spades)
        )

        val analysis = PatternAnalyzer.analyzeHand(hand)

        // Three=2, Four=2, Five=1
        // 所有不同点数都可以作为单张出
        assertEquals(3, analysis.singles.size) // Three, Four, Five
        assertEquals(2, analysis.pairs.size)   // Three, Four
        assertEquals(0, analysis.triples.size)
    }

    @Test
    fun testFindBomb() {
        val hand = listOf(
            Card(Rank.Seven, Suit.Spades),
            Card(Rank.Seven, Suit.Hearts),
            Card(Rank.Seven, Suit.Diamonds),
            Card(Rank.Seven, Suit.Clubs)
        )

        val analysis = PatternAnalyzer.analyzeHand(hand)

        assertEquals(1, analysis.bombs.size)
        assertEquals(PatternType.Bomb, analysis.bombs.first().pattern.type)
    }

    @Test
    fun testFindRocket() {
        val hand = listOf(
            Card(Rank.SmallJoker, Suit.Joker),
            Card(Rank.BigJoker, Suit.Joker)
        )

        val analysis = PatternAnalyzer.analyzeHand(hand)

        assertNotNull(analysis.rocket)
        assertEquals(PatternType.Rocket, analysis.rocket!!.pattern.type)
    }

    @Test
    fun testFindStraight() {
        val hand = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Four, Suit.Hearts),
            Card(Rank.Five, Suit.Diamonds),
            Card(Rank.Six, Suit.Clubs),
            Card(Rank.Seven, Suit.Spades)
        )

        val analysis = PatternAnalyzer.analyzeHand(hand)

        assertTrue(analysis.straights.isNotEmpty())
        assertEquals(PatternType.Straight, analysis.straights.first().pattern.type)
    }

    @Test
    fun testDecideBidStrongHand() {
        val hand = listOf(
            Card(Rank.BigJoker, Suit.Joker),
            Card(Rank.SmallJoker, Suit.Joker),
            Card(Rank.Two, Suit.Spades),
            Card(Rank.Two, Suit.Hearts),
            Card(Rank.Ace, Suit.Diamonds),
            Card(Rank.Ace, Suit.Clubs),
            Card(Rank.King, Suit.Spades),
            Card(Rank.King, Suit.Hearts),
            Card(Rank.Queen, Suit.Diamonds),
            Card(Rank.Queen, Suit.Clubs),
            Card(Rank.Jack, Suit.Spades),
            Card(Rank.Jack, Suit.Hearts),
            Card(Rank.Ten, Suit.Diamonds),
            Card(Rank.Ten, Suit.Clubs),
            Card(Rank.Nine, Suit.Spades),
            Card(Rank.Nine, Suit.Hearts),
            Card(Rank.Eight, Suit.Diamonds)
        )

        val bid = strategy.decideBid(hand, 0, 3)

        assertEquals(3, bid) // 强手牌应该叫3分
    }

    @Test
    fun testDecideBidWeakHand() {
        val hand = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Four, Suit.Hearts),
            Card(Rank.Five, Suit.Diamonds),
            Card(Rank.Six, Suit.Clubs),
            Card(Rank.Seven, Suit.Spades),
            Card(Rank.Eight, Suit.Hearts),
            Card(Rank.Nine, Suit.Diamonds),
            Card(Rank.Ten, Suit.Clubs),
            Card(Rank.Jack, Suit.Spades),
            Card(Rank.Queen, Suit.Hearts),
            Card(Rank.King, Suit.Diamonds),
            Card(Rank.Ace, Suit.Clubs),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Four, Suit.Diamonds),
            Card(Rank.Five, Suit.Clubs),
            Card(Rank.Six, Suit.Spades),
            Card(Rank.Seven, Suit.Hearts)
        )

        val bid = strategy.decideBid(hand, 0, 3)

        assertEquals(0, bid) // 弱手牌不应该叫地主
    }

    @Test
    fun testDecidePlayFree() {
        val hand = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts),
            Card(Rank.Four, Suit.Diamonds),
            Card(Rank.Four, Suit.Clubs),
            Card(Rank.Five, Suit.Spades)
        )

        val cards = strategy.decidePlay(hand, null, false, 17)

        assertNotNull(cards)
        assertTrue(cards!!.isNotEmpty())
    }

    @Test
    fun testDecidePlayFollow() {
        val hand = listOf(
            Card(Rank.Five, Suit.Spades),
            Card(Rank.Six, Suit.Hearts),
            Card(Rank.Seven, Suit.Diamonds),
            Card(Rank.Eight, Suit.Clubs),
            Card(Rank.Nine, Suit.Spades)
        )

        val lastPattern = HandPattern.single(Rank.Four)

        val cards = strategy.decidePlay(hand, lastPattern, false, 17)

        assertNotNull(cards)
        assertTrue(cards!!.isNotEmpty())
    }

    @Test
    fun testDecidePlayPass() {
        val hand = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts)
        )

        val lastPattern = HandPattern.single(Rank.BigJoker)

        val cards = strategy.decidePlay(hand, lastPattern, false, 17)

        assertNull(cards) // 没有能压过火箭的牌，应该过牌
    }

    @Test
    fun testAiPlayerAutoBid() {
        val player = Player(id = "ai1", name = "AI玩家1", isAI = true)
        player.addCards(listOf(
            Card(Rank.BigJoker, Suit.Joker),
            Card(Rank.SmallJoker, Suit.Joker),
            Card(Rank.Two, Suit.Spades),
            Card(Rank.Two, Suit.Hearts)
        ))

        val aiPlayer = AiPlayer(player, strategy)

        // 模拟GameFlow
        val room = Room(id = "test-room", name = "测试房间", hostId = "player1")
        room.addPlayer(player)
        room.addPlayer(Player(id = "player2", name = "玩家2"))
        room.addPlayer(Player(id = "player3", name = "玩家3"))

        val callback = object : com.happy.poker.core.flow.GameCallback {
            override fun onGameStart(players: List<Player>, bottomCards: List<Card>) {}
            override fun onDealCards(playerId: String, cards: List<Card>) {}
            override fun onBidStart(firstBidderId: String) {}
            override fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean) {}
            override fun onLandlordDecided(landlordId: String, bottomCards: List<Card>, multiplier: Int) {}
            override fun onPlayStart(landlordId: String, firstPlayerId: String) {}
            override fun onPlayerPlay(playerId: String, playerName: String, cards: List<Card>, pattern: HandPattern, isPass: Boolean) {}
            override fun onMultiplierChanged(multiplier: Int, bombCount: Int) {}
            override fun onSpring(landlordId: String, isLandlordWin: Boolean) {}
            override fun onGameEnd(winnerId: String, winnerRole: PlayerRole, scores: Map<String, Int>, multiplier: Int) {}
            override fun onError(message: String) {}
            override fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean) {}
        }

        val gameFlow = com.happy.poker.core.flow.GameFlow(room, callback)
        gameFlow.startGame()

        // 设置当前叫地主的玩家为AI
        room.currentBidder = player.id

        val bid = aiPlayer.decideBid(gameFlow, 0)
        assertTrue(bid in 0..3)
    }

    @Test
    fun testAiManager() {
        val manager = AiManager()
        val player = Player(id = "ai1", name = "AI玩家1", isAI = true)

        val aiPlayer = manager.createAiPlayer(player)

        assertEquals(1, manager.getAllAiPlayers().size)
        assertNotNull(manager.getAiPlayer("ai1"))

        manager.removeAiPlayer("ai1")
        assertEquals(0, manager.getAllAiPlayers().size)
    }
}
