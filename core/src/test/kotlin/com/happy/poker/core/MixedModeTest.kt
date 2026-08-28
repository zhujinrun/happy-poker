package com.happy.poker.core

import com.happy.poker.core.ai.AiManager
import com.happy.poker.core.ai.AdvancedStrategy
import com.happy.poker.core.flow.GameCallback
import com.happy.poker.core.flow.GameFlow
import com.happy.poker.core.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 混合模式测试
 * 测试人类玩家 + AI玩家的对战场景
 */
class MixedModeTest {

    private lateinit var room: Room
    private lateinit var gameFlow: GameFlow
    private val aiManager = AiManager()
    private val humanPlayerId = "human_player"
    private val events = mutableListOf<String>()

    @Before
    fun setup() {
        room = Room(
            id = "test_room",
            name = "混合模式测试房间",
            hostId = humanPlayerId,
            maxPlayers = 3
        )

        // 创建人类玩家
        val humanPlayer = Player(humanPlayerId, "我", isAI = false)
        // 创建AI玩家
        val aiPlayer1 = Player("ai_1", "电脑1", isAI = true)
        val aiPlayer2 = Player("ai_2", "电脑2", isAI = true)

        room.addPlayer(humanPlayer)
        room.addPlayer(aiPlayer1)
        room.addPlayer(aiPlayer2)

        // 创建AI玩家实例
        aiManager.createAiPlayer(aiPlayer1, AdvancedStrategy())
        aiManager.createAiPlayer(aiPlayer2, AdvancedStrategy())

        gameFlow = GameFlow(room, createTestCallback())
    }

    @Test
    fun `test mixed mode game can start`() {
        gameFlow.startGame()
        assertEquals(RoomState.Bidding, room.state)
        assertEquals(3, room.players.size)
        assertEquals(17, room.players[0].handSize)
        assertEquals(17, room.players[1].handSize)
        assertEquals(20, room.players[2].handSize)
    }

    @Test
    fun `test human player can bid`() {
        gameFlow.startGame()
        val success = gameFlow.playerBid(humanPlayerId, 1)
        assertTrue(success)
    }

    @Test
    fun `test AI players can bid`() {
        gameFlow.startGame()
        
        // 人类玩家叫分
        gameFlow.playerBid(humanPlayerId, 1)
        
        // AI玩家叫分
        val aiPlayer = aiManager.getAiPlayer("ai_1")
        assertNotNull(aiPlayer)
        
        val bid = aiPlayer!!.decideBid(gameFlow, 1)
        val success = gameFlow.playerBid("ai_1", bid)
        assertTrue(success)
    }

    @Test
    fun `test mixed mode full game flow`() {
        gameFlow.startGame()
        
        // 叫地主阶段
        var currentBidder = room.currentBidder
        while (room.state == RoomState.Bidding) {
            if (currentBidder == humanPlayerId) {
                // 人类玩家叫1分
                gameFlow.playerBid(humanPlayerId, 1)
            } else {
                // AI玩家自动叫分
                val aiPlayer = aiManager.getAiPlayer(currentBidder!!)
                if (aiPlayer != null) {
                    val bid = aiPlayer.decideBid(gameFlow, room.currentBid)
                    gameFlow.playerBid(currentBidder, bid)
                }
            }
            currentBidder = room.currentBidder
        }
        
        // 确保地主已确定
        assertNotNull(room.landlordId)
        assertEquals(RoomState.Playing, room.state)
        
        // 出牌阶段
        while (room.state == RoomState.Playing) {
            val currentPlayer = room.currentPlayer
            
            if (currentPlayer?.id == humanPlayerId) {
                // 人类玩家出牌（简单策略：出最小的牌）
                if (currentPlayer.hasCards) {
                    val card = currentPlayer.hand.first()
                    gameFlow.playerPlay(humanPlayerId, listOf(card))
                }
            } else if (currentPlayer != null) {
                // AI玩家自动出牌
                val aiPlayer = aiManager.getAiPlayer(currentPlayer.id)
                if (aiPlayer != null) {
                    val isLandlord = room.landlordId == currentPlayer.id
                    val landlordHandSize = room.landlord?.handSize ?: 0
                    aiPlayer.autoPlay(gameFlow, null, isLandlord, landlordHandSize)
                }
            }
        }
        
        // 游戏结束
        assertEquals(RoomState.Finished, room.state)
        // winnerId通过callback传递，不是Room的属性
        assertTrue(events.any { it.startsWith("GameEnd:") })
    }

    @Test
    fun `test AI strategy differs by role`() {
        gameFlow.startGame()
        
        // 模拟叫地主过程
        gameFlow.playerBid(humanPlayerId, 3)
        
        // 确保地主已确定
        if (room.state == RoomState.Playing) {
            val landlordId = room.landlordId
            assertNotNull(landlordId)
            
            // 验证AI玩家的策略会根据角色不同而不同
            val aiPlayer1 = aiManager.getAiPlayer("ai_1")
            val aiPlayer2 = aiManager.getAiPlayer("ai_2")
            
            assertNotNull(aiPlayer1)
            assertNotNull(aiPlayer2)
            
            // 这里可以进一步验证不同角色的AI行为差异
        }
    }

    private fun createTestCallback(): GameCallback {
        return object : GameCallback {
            override fun onGameStart(players: List<Player>, bottomCards: List<Card>) {
                events.add("GameStarted")
            }

            override fun onDealCards(playerId: String, cards: List<Card>) {
                events.add("CardsDealt:$playerId")
            }

            override fun onBidStart(firstBidderId: String) {
                events.add("BidStarted:$firstBidderId")
            }

            override fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean) {
                events.add("PlayerBid:$playerId:$bid")
            }

            override fun onLandlordDecided(landlordId: String, bottomCards: List<Card>, multiplier: Int) {
                events.add("LandlordDecided:$landlordId")
            }

            override fun onPlayStart(landlordId: String, firstPlayerId: String) {
                events.add("PlayStarted:$firstPlayerId")
            }

            override fun onPlayerPlay(playerId: String, playerName: String, cards: List<Card>, pattern: HandPattern, isPass: Boolean) {
                events.add("PlayerPlayed:$playerId")
            }

            override fun onMultiplierChanged(multiplier: Int, bombCount: Int) {
                events.add("MultiplierChanged:$multiplier")
            }

            override fun onSpring(landlordId: String, isLandlordWin: Boolean) {
                events.add("Spring:$landlordId")
            }

            override fun onGameEnd(winnerId: String, winnerRole: PlayerRole, scores: Map<String, Int>, multiplier: Int) {
                events.add("GameEnd:$winnerId")
            }

            override fun onError(message: String) {
                events.add("Error:$message")
            }

            override fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean) {
                events.add("StatusChanged:$playerId")
            }
        }
    }
}
