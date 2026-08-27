package com.happy.poker.core.rules

import com.happy.poker.core.model.Player
import com.happy.poker.core.model.PlayerRole
import com.happy.poker.core.model.Room
import com.happy.poker.core.model.RoomState

/**
 * 叫地主结果
 */
data class BidResult(
    val success: Boolean,
    val message: String = "",
    val bid: Int = 0,
    val isLandlord: Boolean = false
)

/**
 * 叫地主逻辑
 */
object Bid {
    /**
     * 叫地主
     * @param player 玩家
     * @param room 房间
     * @param bid 叫分（0=不叫，1=1分，2=2分，3=3分）
     */
    fun bid(player: Player, room: Room, bid: Int): BidResult {
        // 检查游戏状态
        if (!room.isBidding) {
            return BidResult(false, "当前不是叫地主阶段")
        }
        
        // 检查是否轮到该玩家
        if (room.currentPlayer?.id != player.id) {
            return BidResult(false, "未轮到你叫地主")
        }
        
        // 检查叫分是否有效
        if (bid < 0 || bid > 3) {
            return BidResult(false, "叫分必须在0-3之间")
        }
        
        // 检查叫分是否超过当前最高分
        if (bid != 0 && bid <= room.currentBid) {
            return BidResult(false, "叫分必须高于当前最高分")
        }
        
        // 更新房间状态
        if (bid > 0) {
            room.currentBid = bid
            room.currentBidder = player.id
        }
        
        // 如果叫3分，直接成为地主
        if (bid == 3) {
            return setLandlord(player, room)
        }
        
        // 切换到下一个玩家
        room.currentPlayerIndex = room.nextPlayerIndex()
        
        // 检查是否所有人都叫过了
        if (isAllBid(room)) {
            // 如果有人叫过分，叫分最高的人成为地主
            if (room.currentBidder != null) {
                val landlord = room.findPlayer(room.currentBidder!!)
                if (landlord != null) {
                    return setLandlord(landlord, room)
                }
            }
            
            // 如果没人叫分，重新发牌
            return BidResult(false, "没人叫地主，重新发牌")
        }
        
        return BidResult(
            true,
            if (bid == 0) "${player.name} 不叫" else "${player.name} 叫 ${bid} 分",
            bid
        )
    }

    /**
     * 设置地主
     */
    private fun setLandlord(player: Player, room: Room): BidResult {
        // 设置玩家角色
        room.players.forEach { p ->
            p.role = if (p.id == player.id) PlayerRole.Landlord else PlayerRole.Farmer
        }
        
        // 设置地主ID
        room.landlordId = player.id
        
        // 给地主发底牌
        player.addCards(room.bottomCards)
        
        // 更新房间状态
        room.state = RoomState.Playing
        room.currentPlayerIndex = room.getPlayerIndex(player.id)
        
        return BidResult(
            true,
            "${player.name} 成为地主！",
            room.currentBid,
            true
        )
    }

    /**
     * 检查是否所有人都叫过了
     */
    private fun isAllBid(room: Room): Boolean {
        // 简化处理：检查是否已经轮了一圈
        // 实际需要更复杂的逻辑来跟踪谁叫过了
        return room.turnHistory.size >= 3
    }

    /**
     * 开始叫地主阶段
     */
    fun startBidding(room: Room) {
        room.state = RoomState.Bidding
        room.currentBid = 0
        room.currentBidder = null
        room.currentPlayerIndex = 0  // 从第一个玩家开始叫
    }
}
