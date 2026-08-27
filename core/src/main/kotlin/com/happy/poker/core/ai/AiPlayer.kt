package com.happy.poker.core.ai

import com.happy.poker.core.flow.GameFlow
import com.happy.poker.core.model.*

/**
 * AI玩家
 * 自动进行叫地主和出牌决策
 */
class AiPlayer(
    val player: Player,
    private val strategy: Strategy = SimpleStrategy()
) {
    /**
     * 决定是否叫地主
     */
    fun decideBid(gameFlow: GameFlow, currentBid: Int): Int {
        val state = gameFlow.getState()
        return strategy.decideBid(player.hand, currentBid, state.players.size)
    }

    /**
     * 决定出什么牌
     */
    fun decidePlay(
        gameFlow: GameFlow,
        lastPattern: HandPattern?,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): List<Card>? {
        return strategy.decidePlay(player.hand, lastPattern, isLandlord, landlordHandSize)
    }

    /**
     * 自动叫地主
     */
    fun autoBid(gameFlow: GameFlow, currentBid: Int): Boolean {
        val bid = decideBid(gameFlow, currentBid)
        return gameFlow.playerBid(player.id, bid)
    }

    /**
     * 自动出牌
     */
    fun autoPlay(
        gameFlow: GameFlow,
        lastPattern: HandPattern?,
        isLandlord: Boolean,
        landlordHandSize: Int
    ): Boolean {
        val cards = decidePlay(gameFlow, lastPattern, isLandlord, landlordHandSize)

        return if (cards != null && cards.isNotEmpty()) {
            gameFlow.playerPlay(player.id, cards)
        } else {
            gameFlow.playerPass(player.id)
        }
    }
}

/**
 * AI管理器
 * 管理多个AI玩家
 */
class AiManager {
    private val aiPlayers = mutableMapOf<String, AiPlayer>()

    /**
     * 创建AI玩家
     */
    fun createAiPlayer(player: Player, strategy: Strategy = SimpleStrategy()): AiPlayer {
        val aiPlayer = AiPlayer(player, strategy)
        aiPlayers[player.id] = aiPlayer
        return aiPlayer
    }

    /**
     * 获取AI玩家
     */
    fun getAiPlayer(playerId: String): AiPlayer? = aiPlayers[playerId]

    /**
     * 移除AI玩家
     */
    fun removeAiPlayer(playerId: String) {
        aiPlayers.remove(playerId)
    }

    /**
     * 清空所有AI玩家
     */
    fun clear() {
        aiPlayers.clear()
    }

    /**
     * 获取所有AI玩家
     */
    fun getAllAiPlayers(): List<AiPlayer> = aiPlayers.values.toList()
}
