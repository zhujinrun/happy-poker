package com.happy.poker.core.flow

import com.happy.poker.core.model.*

/**
 * 游戏事件回调接口
 * 用于通知UI或网络层游戏状态变化
 */
interface GameCallback {
    /** 游戏开始 */
    fun onGameStart(players: List<Player>, bottomCards: List<Card>)

    /** 发牌动画 */
    fun onDealCards(playerId: String, cards: List<Card>)

    /** 叫地主阶段开始 */
    fun onBidStart(firstBidderId: String)

    /** 玩家叫分 */
    fun onPlayerBid(playerId: String, playerName: String, bid: Int, isPass: Boolean)

    /** 叫地主结束，确定地主 */
    fun onLandlordDecided(landlordId: String, bottomCards: List<Card>, multiplier: Int)

    /** 出牌阶段开始 */
    fun onPlayStart(landlordId: String, firstPlayerId: String)

    /** 玩家出牌 */
    fun onPlayerPlay(
        playerId: String,
        playerName: String,
        cards: List<Card>,
        pattern: HandPattern,
        isPass: Boolean
    )

    /** 炸弹/火箭出现，倍数增加 */
    fun onMultiplierChanged(multiplier: Int, bombCount: Int)

    /** 春天出现，倍数翻倍 */
    fun onSpring(landlordId: String, isLandlordWin: Boolean)

    /** 游戏结束 */
    fun onGameEnd(
        winnerId: String,
        winnerRole: PlayerRole,
        scores: Map<String, Int>,
        multiplier: Int
    )

    /** 错误信息 */
    fun onError(message: String)

    /** 玩家状态更新 */
    fun onPlayerStatusChanged(playerId: String, isOnline: Boolean, isReady: Boolean)
}
