package com.happy.poker.core.flow

import com.happy.poker.core.model.*
import com.happy.poker.core.rules.Deal
import com.happy.poker.core.rules.DealResult
import com.happy.poker.core.rules.Play
import com.happy.poker.core.rules.Validator

/**
 * 游戏状态机
 * 管理整个游戏流程：发牌 → 叫地主 → 出牌 → 胜负判定
 */
class GameFlow(
    private val room: Room,
    private val callback: GameCallback
) {
    private var bidIndex = 0
    private val bidHistory = mutableListOf<Pair<String, Int>>()
    private var highestBid = 0
    private var highestBidder: String? = null
    private var passCount = 0
    private var currentDeal: DealResult? = null

    /**
     * 开始游戏
     */
    fun startGame() {
        if (room.playerCount < 2) {
            callback.onError("需要至少2名玩家才能开始游戏")
            return
        }

        // 发牌
        val dealResult = Deal.deal()
        currentDeal = dealResult

        // 分配手牌给玩家
        room.players.forEachIndexed { index, player ->
            player.clearHand()
            player.addCards(dealResult.hands[index])
            player.role = PlayerRole.Unknown
            player.isReady = false
            callback.onDealCards(player.id, dealResult.hands[index])
        }

        // 设置游戏状态
        room.turnHistory.clear()
        room.state = RoomState.Bidding
        room.multiplier = 1
        room.bombCount = 0
        room.landlordId = null
        room.currentBid = 0
        room.currentBidder = null
        highestBid = 0
        highestBidder = null
        passCount = 0
        bidHistory.clear()

        callback.onGameStart(room.players, dealResult.bottomCards)

        // 随机选择第一个叫地主的玩家
        bidIndex = (0 until room.playerCount).random()
        room.currentBidder = room.players[bidIndex].id
        callback.onBidStart(room.players[bidIndex].id)
    }

    /**
     * 玩家叫分
     * @param playerId 玩家ID
     * @param bid 叫分 (0=不叫, 1/2/3)
     */
    fun playerBid(playerId: String, bid: Int): Boolean {
        if (room.state != RoomState.Bidding) {
            callback.onError("当前不是叫地主阶段")
            return false
        }

        if (room.currentBidder != playerId) {
            callback.onError("还没轮到你叫地主")
            return false
        }

        if (bid < 0 || bid > 3) {
            callback.onError("叫分必须在0-3之间")
            return false
        }

        if (bid != 0 && bid <= highestBid) {
            callback.onError("叫分必须高于当前最高分")
            return false
        }

        val player = room.findPlayer(playerId) ?: return false

        bidHistory.add(Pair(playerId, bid))
        callback.onPlayerBid(playerId, player.name, bid, bid == 0)

        if (bid > 0) {
            highestBid = bid
            highestBidder = playerId
            room.currentBid = bid
        } else {
            passCount++
        }

        // 叫3分直接成为地主
        if (bid == 3) {
            decideLandlord(playerId)
            return true
        }

        // 所有人都叫过了
        if (passCount >= room.playerCount) {
            if (highestBidder != null) {
                decideLandlord(highestBidder!!)
            } else {
                callback.onError("所有人都没叫地主，重新发牌")
                startGame()
            }
            return true
        }

        // 下一个玩家叫地主
        bidIndex = (bidIndex + 1) % room.playerCount
        room.currentBidder = room.players[bidIndex].id
        callback.onBidStart(room.players[bidIndex].id)

        return true
    }

    /**
     * 确定地主
     */
    private fun decideLandlord(landlordId: String) {
        val landlord = room.findPlayer(landlordId) ?: return
        val bottomCards = currentDeal?.bottomCards ?: return

        landlord.role = PlayerRole.Landlord
        landlord.addCards(bottomCards)
        room.landlordId = landlordId
        room.players.filter { it.id != landlordId }.forEach {
            it.role = PlayerRole.Farmer
        }

        room.state = RoomState.Playing
        room.currentPlayerIndex = room.getPlayerIndex(landlordId)
        room.lastPlayedPattern = null
        room.lastPlayedCards = null
        room.lastPlayedPlayerId = null
        room.passCount = 0

        callback.onLandlordDecided(landlordId, bottomCards, room.multiplier)
        callback.onPlayStart(landlordId, room.players[room.currentPlayerIndex].id)
    }

    /**
     * 玩家出牌
     */
    fun playerPlay(playerId: String, cards: List<Card>): Boolean {
        if (room.state != RoomState.Playing) {
            callback.onError("当前不是出牌阶段")
            return false
        }

        if (room.currentPlayer?.id != playerId) {
            callback.onError("还没轮到你出牌")
            return false
        }

        val player = room.findPlayer(playerId) ?: return false
        val result = Validator.validatePlay(cards, room.lastPlayedPattern)

        if (!result.isValid) {
            callback.onError(result.reason)
            return false
        }

        val playResult = Play.playCards(player, cards, room)
        if (!playResult.success) {
            callback.onError(playResult.message)
            return false
        }

        callback.onPlayerPlay(playerId, player.name, cards, result.pattern, false)
        checkBomb(result.pattern)
        checkGameEnd(playerId)
        return true
    }

    /**
     * 玩家过牌
     */
    fun playerPass(playerId: String): Boolean {
        if (room.state != RoomState.Playing) {
            callback.onError("当前不是出牌阶段")
            return false
        }

        if (room.currentPlayer?.id != playerId) {
            callback.onError("还没轮到你出牌")
            return false
        }

        // 首次出牌不能过
        if (room.lastPlayedPattern == null) {
            callback.onError("你必须出牌")
            return false
        }

        val player = room.findPlayer(playerId) ?: return false
        val playResult = Play.pass(player, room)
        if (!playResult.success) {
            callback.onError(playResult.message)
            return false
        }

        callback.onPlayerPlay(playerId, player.name, emptyList(), HandPattern.Invalid, true)
        return true
    }

    /**
     * 检查炸弹/火箭
     */
    private fun checkBomb(pattern: HandPattern) {
        when (pattern.type) {
            PatternType.Bomb, PatternType.Rocket -> {
                room.addBombMultiplier()
                callback.onMultiplierChanged(room.multiplier, room.bombCount)
            }
            else -> {}
        }
    }

    /**
     * 检查游戏是否结束
     */
    private fun checkGameEnd(lastPlayerId: String) {
        val lastPlayer = room.findPlayer(lastPlayerId) ?: return
        if (!lastPlayer.hasCards) {
            // 游戏结束
            val winnerRole = lastPlayer.role
            val isLandlordWin = winnerRole == PlayerRole.Landlord

            // 检查春天
            if (isLandlordWin) {
                val farmersPlayed = room.turnHistory
                    .filter { room.findPlayer(it.playerId)?.isFarmer == true }
                    .any { it.cards.isNotEmpty() }
                if (!farmersPlayed) {
                    room.setSpringMultiplier()
                    callback.onSpring(lastPlayerId, true)
                }
            } else {
                val landlordPlayed = room.turnHistory
                    .filter { room.findPlayer(it.playerId)?.isLandlord == true }
                    .any { it.cards.isNotEmpty() }
                if (!landlordPlayed) {
                    room.setSpringMultiplier()
                    callback.onSpring(lastPlayerId, false)
                }
            }

            // 计算分数
            val baseScore = room.currentBid
            val scores = calculateScores(winnerRole, baseScore, room.multiplier)
            room.state = RoomState.Finished

            callback.onGameEnd(lastPlayerId, winnerRole, scores, room.multiplier)
        }
    }

    /**
     * 计算分数
     */
    private fun calculateScores(winnerRole: PlayerRole, baseScore: Int, multiplier: Int): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        val totalScore = baseScore * multiplier

        room.players.forEach { player ->
            scores[player.id] = when {
                player.isLandlord && winnerRole == PlayerRole.Landlord -> totalScore * 2
                player.isFarmer && winnerRole == PlayerRole.Farmer -> totalScore
                else -> -totalScore
            }
        }

        return scores
    }

    /**
     * 获取当前游戏状态
     */
    fun getState(): GameState {
        return GameState(
            roomId = room.id,
            state = room.state,
            players = room.players.map { it.toPlayerState() },
            currentPlayerId = room.currentPlayer?.id,
            landlordId = room.landlordId,
            multiplier = room.multiplier,
            lastPlayedCards = room.lastPlayedCards,
            lastPlayedPlayerId = room.lastPlayedPlayerId,
            bottomCards = currentDeal?.bottomCards ?: emptyList()
        )
    }
}

/**
 * 游戏状态快照
 */
data class GameState(
    val roomId: String,
    val state: RoomState,
    val players: List<PlayerState>,
    val currentPlayerId: String?,
    val landlordId: String?,
    val multiplier: Int,
    val lastPlayedCards: List<Card>?,
    val lastPlayedPlayerId: String?,
    val bottomCards: List<Card>
)

/**
 * 玩家状态
 */
data class PlayerState(
    val id: String,
    val name: String,
    val role: PlayerRole,
    val handSize: Int,
    val isOnline: Boolean,
    val isReady: Boolean
)

/**
 * Player 扩展函数
 */
fun Player.toPlayerState(): PlayerState {
    return PlayerState(
        id = id,
        name = name,
        role = role,
        handSize = handSize,
        isOnline = isOnline,
        isReady = isReady
    )
}
