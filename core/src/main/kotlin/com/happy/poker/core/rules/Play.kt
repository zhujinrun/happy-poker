package com.happy.poker.core.rules

import com.happy.poker.core.model.Card
import com.happy.poker.core.model.HandPattern
import com.happy.poker.core.model.Player
import com.happy.poker.core.model.Room

/**
 * 出牌结果
 */
data class PlayResult(
    val success: Boolean,
    val message: String = "",
    val cards: List<Card> = emptyList(),
    val pattern: HandPattern = HandPattern.Invalid
)

/**
 * 游戏逻辑
 */
object Play {
    /**
     * 出牌
     */
    fun playCards(
        player: Player,
        cards: List<Card>,
        room: Room
    ): PlayResult {
        // 检查游戏状态
        if (!room.isPlaying) {
            return PlayResult(false, "游戏未开始")
        }
        
        // 检查是否轮到该玩家
        if (room.currentPlayer?.id != player.id) {
            return PlayResult(false, "未轮到你出牌")
        }
        
        // 检查玩家是否有这些牌
        if (!player.hasCards(cards)) {
            return PlayResult(false, "你没有这些牌")
        }
        
        // 识别牌型
        val validationResult = Validator.validatePlay(cards, room.lastPlayedPattern)
        if (!validationResult.isValid) {
            return PlayResult(false, validationResult.reason)
        }
        
        // 从手牌中移除牌
        player.removeCards(cards)
        
        // 记录出牌
        val record = com.happy.poker.core.model.TurnRecord(
            playerId = player.id,
            playerName = player.name,
            cards = cards,
            pattern = validationResult.pattern
        )
        room.addTurnRecord(record)
        
        // 更新房间状态
        room.lastPlayedPattern = validationResult.pattern
        room.lastPlayedCards = cards
        room.lastPlayedPlayerId = player.id
        room.passCount = 0
        
        // 检查是否是炸弹
        if (validationResult.pattern.isBomb) {
            room.addBombMultiplier()
        }
        
        // 检查是否出完牌
        if (player.hand.isEmpty()) {
            // 检查春天
            checkSpring(room, player)
            return PlayResult(
                true,
                "${player.name} 获胜！",
                cards,
                validationResult.pattern
            )
        }
        
        // 切换到下一个玩家
        room.currentPlayerIndex = room.nextPlayerIndex()
        
        return PlayResult(
            true,
            "${player.name} 出了 ${validationResult.pattern.description}",
            cards,
            validationResult.pattern
        )
    }

    /**
     * 不出（过牌）
     */
    fun pass(player: Player, room: Room): PlayResult {
        // 检查游戏状态
        if (!room.isPlaying) {
            return PlayResult(false, "游戏未开始")
        }
        
        // 检查是否轮到该玩家
        if (room.currentPlayer?.id != player.id) {
            return PlayResult(false, "未轮到你出牌")
        }
        
        // 检查是否可以不出（如果是第一个出牌，必须出）
        if (room.lastPlayedPattern == null || room.lastPlayedPlayerId == player.id) {
            return PlayResult(false, "你必须出牌")
        }
        
        // 增加过牌计数
        room.passCount++
        
        // 除最后出牌者外，其他玩家都过牌后，轮到最后出牌者重新领出
        val passesNeededToReset = (room.playerCount - 1).coerceAtLeast(1)
        if (room.passCount >= passesNeededToReset) {
            val lastPlayer = room.findPlayer(room.lastPlayedPlayerId!!)
            if (lastPlayer != null) {
                room.currentPlayerIndex = room.getPlayerIndex(lastPlayer.id)
                room.lastPlayedPattern = null
                room.lastPlayedCards = null
                room.passCount = 0
            }
        } else {
            // 切换到下一个玩家
            room.currentPlayerIndex = room.nextPlayerIndex()
        }
        
        return PlayResult(
            true,
            "${player.name} 不出"
        )
    }

    /**
     * 检查春天
     */
    private fun checkSpring(room: Room, winner: Player) {
        // 检查是否有人一张牌都没出
        val hasSpring = room.players.any { 
            it.id != winner.id && 
            it.hand.size == 17 && 
            room.turnHistory.none { record -> record.playerId == it.id }
        }
        
        if (hasSpring) {
            room.setSpringMultiplier()
        }
    }

    /**
     * 检查玩家是否有可出的牌
     */
    fun hasPlayableCards(player: Player, room: Room): Boolean {
        if (!room.isPlaying) return false
        if (room.currentPlayer?.id != player.id) return false
        
        // 如果没有上家出牌，任何牌都可以出
        if (room.lastPlayedPattern == null || room.lastPlayedPlayerId == player.id) {
            return player.hand.isNotEmpty()
        }
        
        // 检查是否有能压过上家的牌
        // 这里简化处理，实际需要更复杂的逻辑
        return true
    }

    /**
     * 获取玩家可以出的所有牌型
     */
    fun getPlayablePatterns(player: Player, room: Room): List<HandPattern> {
        if (!room.isPlaying) return emptyList()
        if (room.currentPlayer?.id != player.id) return emptyList()
        
        // 这里简化处理，实际需要生成所有可能的牌型
        return emptyList()
    }
}
