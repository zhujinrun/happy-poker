package com.happy.poker.core.rules

import com.happy.poker.core.model.Card
import com.happy.poker.core.model.Player

/**
 * 发牌结果
 */
data class DealResult(
    val hands: List<List<Card>>,  // 三个玩家的手牌
    val bottomCards: List<Card>,  // 底牌（3张）
    val deck: List<Card>          // 完整的牌组
)

/**
 * 发牌逻辑
 */
object Deal {
    /**
     * 发牌
     * @param seed 随机种子
     * @return 发牌结果
     */
    fun deal(seed: Long = System.currentTimeMillis()): DealResult {
        // 创建并洗牌
        val deck = Deck.shuffle(Deck.create(), seed)
        
        // 发牌：每个玩家17张，底牌3张
        val hand1 = deck.subList(0, 17).sortedBy { it.rank.value * 10 + it.suit.ordinal }
        val hand2 = deck.subList(17, 34).sortedBy { it.rank.value * 10 + it.suit.ordinal }
        val hand3 = deck.subList(34, 51).sortedBy { it.rank.value * 10 + it.suit.ordinal }
        val bottomCards = deck.subList(51, 54)
        
        return DealResult(
            hands = listOf(hand1, hand2, hand3),
            bottomCards = bottomCards,
            deck = deck
        )
    }

    /**
     * 发牌并分配给玩家
     * @param players 玩家列表
     * @param seed 随机种子
     */
    fun dealToPlayers(players: List<Player>, seed: Long = System.currentTimeMillis()) {
        require(players.size == 3) { "斗地主需要3个玩家" }
        
        val result = deal(seed)
        
        players.forEachIndexed { index, player ->
            player.clearHand()
            player.addCards(result.hands[index])
        }
    }

    /**
     * 获取底牌
     * @param seed 随机种子
     * @return 底牌列表
     */
    fun getBottomCards(seed: Long = System.currentTimeMillis()): List<Card> {
        return Deck.shuffle(Deck.create(), seed).subList(51, 54)
    }

    /**
     * 验证发牌结果
     * @param result 发牌结果
     * @return 是否有效
     */
    fun validate(result: DealResult): Boolean {
        // 检查每个玩家的手牌数量
        if (result.hands.any { it.size != 17 }) return false
        
        // 检查底牌数量
        if (result.bottomCards.size != 3) return false
        
        // 检查所有牌是否唯一
        val allCards = result.hands.flatten() + result.bottomCards
        val cardIds = allCards.map { it.id }
        if (cardIds.size != cardIds.toSet().size) return false
        
        // 检查是否包含所有54张牌
        if (allCards.size != 54) return false
        
        return true
    }
}
