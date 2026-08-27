package com.happy.poker.core.model

/**
 * 牌花色
 */
enum class Suit(val symbol: String, val displayName: String) {
    Spades("♠", "黑桃"),
    Hearts("♥", "红心"),
    Diamonds("♦", "方块"),
    Clubs("♣", "梅花"),
    Joker("🃏", "王牌")
}

/**
 * 牌点数
 */
enum class Rank(val value: Int, val label: String, val displayName: String) {
    Three(3, "3", "三"),
    Four(4, "4", "四"),
    Five(5, "5", "五"),
    Six(6, "6", "六"),
    Seven(7, "7", "七"),
    Eight(8, "8", "八"),
    Nine(9, "9", "九"),
    Ten(10, "10", "十"),
    Jack(11, "J", "J"),
    Queen(12, "Q", "Q"),
    King(13, "K", "K"),
    Ace(14, "A", "A"),
    Two(15, "2", "二"),
    SmallJoker(16, "小王", "小王"),
    BigJoker(17, "大王", "大王")
}

/**
 * 牌
 */
data class Card(
    val rank: Rank,
    val suit: Suit
) {
    /**
     * 唯一标识，如 "3♠", "大王"
     */
    val id: String = when {
        rank == Rank.SmallJoker -> "小王"
        rank == Rank.BigJoker -> "大王"
        else -> "${rank.label}${suit.symbol}"
    }

    /**
     * 显示名称，如 "黑桃3", "大王"
     */
    val displayName: String = when {
        rank == Rank.SmallJoker -> "小王"
        rank == Rank.BigJoker -> "大王"
        else -> "${suit.displayName}${rank.displayName}"
    }

    /**
     * 是否是王牌
     */
    val isJoker: Boolean get() = rank == Rank.SmallJoker || rank == Rank.BigJoker

    companion object {
        /**
         * 根据ID创建牌
         */
        fun fromId(id: String): Card? {
            return when (id) {
                "小王" -> Card(Rank.SmallJoker, Suit.Joker)
                "大王" -> Card(Rank.BigJoker, Suit.Joker)
                else -> {
                    if (id.length < 2) return null
                    val rankLabel = id.substring(0, id.length - 1)
                    val suitSymbol = id.last().toString()
                    
                    val rank = Rank.entries.find { it.label == rankLabel } ?: return null
                    val suit = Suit.entries.find { it.symbol == suitSymbol } ?: return null
                    
                    Card(rank, suit)
                }
            }
        }
    }
}

/**
 * 牌列表类型别名
 */
typealias Cards = List<Card>

/**
 * 扩展函数：按游戏顺序排序（点数从小到大，同点数按花色排序）
 */
fun Cards.sortedByGameOrder(): List<Card> = sortedWith(
    compareBy<Card> { it.rank.value }
        .thenBy { it.suit.ordinal }
)

/**
 * 扩展函数：转为牌文本（如 "3♠ 4♥ 5♦"）
 */
fun Cards.toCardText(): String = joinToString(" ") { it.id }

/**
 * 扩展函数：按点数分组计数
 */
fun Cards.countByRank(): Map<Rank, Int> = groupingBy { it.rank }.eachCount()

/**
 * 扩展函数：获取所有不同的点数
 */
fun Cards.distinctRanks(): List<Rank> = map { it.rank }.distinct().sortedBy { it.value }
