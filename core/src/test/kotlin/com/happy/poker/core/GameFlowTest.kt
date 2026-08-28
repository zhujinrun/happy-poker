package com.happy.poker.core

import com.happy.poker.core.flow.GameCallback
import com.happy.poker.core.flow.GameFlow
import com.happy.poker.core.model.*
import com.happy.poker.core.rules.Deal
import com.happy.poker.core.rules.Play
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameFlowTest {
    private lateinit var room: Room
    private lateinit var callback: TestCallback
    private lateinit var gameFlow: GameFlow

    private class TestCallback : GameCallback {
        var gameStarted = false
        var bidStarted = false
        var playStarted = false
        var gameEnded = false
        var lastError = ""
        var lastLandlordId: String? = null
        var lastWinnerId: String? = null
        var lastScores: Map<String, Int> = emptyMap()

        override fun onGameStart(players: List<Player>, bottomCards: List<Card>) {
            gameStarted = true
        }

        override fun onDealCards(playerId: String, cards: List<Card>) {}
        override fun onBidStart(firstBidderId: String) { bidStarted = true }

        override fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean) {}

        override fun onLandlordDecided(landlordId: String, bottomCards: List<Card>, multiplier: Int) {
            lastLandlordId = landlordId
        }

        override fun onPlayStart(landlordId: String, firstPlayerId: String) { playStarted = true }

        override fun onPlayerPlay(
            playerId: String,
            playerName: String,
            cards: List<Card>,
            pattern: HandPattern,
            isPass: Boolean
        ) {}

        override fun onMultiplierChanged(multiplier: Int, bombCount: Int) {}
        override fun onSpring(landlordId: String, isLandlordWin: Boolean) {}

        override fun onGameEnd(
            winnerId: String,
            winnerRole: PlayerRole,
            scores: Map<String, Int>,
            multiplier: Int
        ) {
            gameEnded = true
            lastWinnerId = winnerId
            lastScores = scores
        }

        override fun onError(message: String) { lastError = message }
        override fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean) {}
    }

    @BeforeEach
    fun setup() {
        room = Room(id = "test-room", name = "测试房间", hostId = "player1")
        room.addPlayer(Player(id = "player1", name = "玩家1"))
        room.addPlayer(Player(id = "player2", name = "玩家2"))
        room.addPlayer(Player(id = "player3", name = "玩家3"))
        callback = TestCallback()
        gameFlow = GameFlow(room, callback)
    }

    @Test
    fun testStartGame() {
        gameFlow.startGame()

        assertTrue(callback.gameStarted)
        assertEquals(RoomState.Bidding, room.state)
        assertEquals(3, room.playerCount)
        room.players.forEach { player ->
            assertEquals(17, player.handSize)
        }
    }

    @Test
    fun testBidToBecomeLandlord() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!

        gameFlow.playerBid(firstBidder, 3)

        assertEquals(RoomState.Playing, room.state)
        assertEquals(firstBidder, room.landlordId)
        val landlord = room.findPlayer(firstBidder)
        assertEquals(PlayerRole.Landlord, landlord?.role)
        assertEquals(20, landlord?.handSize)
    }

    @Test
    fun testPassAndNextBidder() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!

        gameFlow.playerBid(firstBidder, 0)

        assertNotEquals(firstBidder, room.currentBidder)
        assertEquals(0, room.currentBid)
    }

    @Test
    fun testStateUsesCurrentBidderDuringBidding() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!

        assertEquals(firstBidder, gameFlow.getState().currentPlayerId)

        gameFlow.playerBid(firstBidder, 0)
        val secondBidder = room.currentBidder!!

        assertEquals(secondBidder, gameFlow.getState().currentPlayerId)
    }

    @Test
    fun testCannotBidLower() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!

        gameFlow.playerBid(firstBidder, 2)
        val secondBidder = room.currentBidder!!

        gameFlow.playerBid(secondBidder, 1)

        assertEquals("叫分必须高于当前最高分", callback.lastError)
    }

    @Test
    fun testCannotPlayDuringBidding() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!

        val cards = listOf(
            Card(Rank.Three, Suit.Spades),
            Card(Rank.Three, Suit.Hearts)
        )
        gameFlow.playerPlay(firstBidder, cards)

        assertEquals("当前不是出牌阶段", callback.lastError)
    }

    @Test
    fun testMustPlayFirstTurn() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!
        gameFlow.playerBid(firstBidder, 3)

        gameFlow.playerPass(firstBidder)

        assertEquals("你必须出牌", callback.lastError)
    }

    @Test
    fun testTwoPlayerPassResetsToLastPlayer() {
        val leader = Player(id = "player1", name = "玩家1")
        val follower = Player(id = "player2", name = "玩家2")
        leader.addCards(
            listOf(
                Card(Rank.Three, Suit.Spades),
                Card(Rank.Five, Suit.Spades)
            )
        )
        follower.addCards(
            listOf(
                Card(Rank.Four, Suit.Hearts),
                Card(Rank.Six, Suit.Hearts)
            )
        )

        val twoPlayerRoom = Room(id = "two-player", name = "二人房", hostId = leader.id).apply {
            addPlayer(leader)
            addPlayer(follower)
            state = RoomState.Playing
            currentPlayerIndex = 0
        }

        assertTrue(Play.playCards(leader, listOf(Card(Rank.Three, Suit.Spades)), twoPlayerRoom).success)
        assertEquals(follower.id, twoPlayerRoom.currentPlayer?.id)

        assertTrue(Play.pass(follower, twoPlayerRoom).success)
        assertEquals(leader.id, twoPlayerRoom.currentPlayer?.id)
        assertNull(twoPlayerRoom.lastPlayedPattern)
        assertNull(twoPlayerRoom.lastPlayedCards)
    }

    @Test
    fun testCalculateScores() {
        gameFlow.startGame()
        val firstBidder = room.currentBidder!!
        gameFlow.playerBid(firstBidder, 3) // 叫3分立即成为地主

        // 地主只剩1张牌
        val landlord = room.findPlayer(firstBidder)
        val lastCard = landlord?.hand?.first()
        landlord?.clearHand()
        if (lastCard != null) {
            landlord?.addCards(listOf(lastCard))
        }

        gameFlow.playerPlay(firstBidder, listOf(lastCard!!))

        assertTrue(callback.gameEnded)
        assertEquals(firstBidder, callback.lastWinnerId)
    }

    @Test
    fun testGetState() {
        gameFlow.startGame()
        val state = gameFlow.getState()

        assertEquals(room.id, state.roomId)
        assertEquals(RoomState.Bidding, state.state)
        assertEquals(3, state.players.size)
        assertNotNull(state.bottomCards)
    }
}
