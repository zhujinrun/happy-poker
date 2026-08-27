package com.happy.poker.core.model

enum class RoomState(val displayName: String) {
    Waiting("等待玩家"),
    Bidding("叫地主中"),
    Playing("游戏中"),
    Finished("已结束")
}

data class TurnRecord(
    val playerId: String,
    val playerName: String,
    val cards: List<Card>,
    val pattern: HandPattern,
    val timestamp: Long = System.currentTimeMillis()
)

class Room(
    val id: String,
    val name: String,
    val hostId: String,
    val maxPlayers: Int = 3,
    val bottomCards: List<Card> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    var state: RoomState = RoomState.Waiting
    val players: MutableList<Player> = mutableListOf()
    var currentBid: Int = 0
    var currentBidder: String? = null
    var landlordId: String? = null
    var multiplier: Int = 1
    var bombCount: Int = 0
    val turnHistory: MutableList<TurnRecord> = mutableListOf()
    var currentPlayerIndex: Int = 0
    var lastPlayedPattern: HandPattern? = null
    var lastPlayedCards: List<Card>? = null
    var lastPlayedPlayerId: String? = null
    var passCount: Int = 0

    val playerCount: Int get() = players.size
    val isFull: Boolean get() = playerCount >= maxPlayers
    val isWaiting: Boolean get() = state == RoomState.Waiting
    val isBidding: Boolean get() = state == RoomState.Bidding
    val isPlaying: Boolean get() = state == RoomState.Playing
    val isFinished: Boolean get() = state == RoomState.Finished

    val currentPlayer: Player?
        get() = if (currentPlayerIndex in players.indices) players[currentPlayerIndex] else null
    val landlord: Player?
        get() = players.find { it.isLandlord }
    val farmers: List<Player>
        get() = players.filter { it.isFarmer }
    val onlinePlayers: List<Player>
        get() = players.filter { it.isOnline }
    val readyPlayers: List<Player>
        get() = players.filter { it.isReady }
    val allReady: Boolean
        get() = players.size == maxPlayers && players.all { it.isReady }

    fun addPlayer(player: Player): Boolean {
        if (isFull) return false
        if (players.any { it.id == player.id }) return false
        players.add(player)
        return true
    }

    fun removePlayer(playerId: String): Boolean {
        return players.removeAll { it.id == playerId }
    }

    fun findPlayer(playerId: String): Player? = players.find { it.id == playerId }

    fun getPlayerIndex(playerId: String): Int = players.indexOfFirst { it.id == playerId }

    fun nextPlayerIndex(): Int = (currentPlayerIndex + 1) % playerCount

    fun addTurnRecord(record: TurnRecord) {
        turnHistory.add(record)
    }

    fun addBombMultiplier() {
        multiplier *= 2
        bombCount++
    }

    fun setSpringMultiplier() {
        multiplier *= 2
    }
}
