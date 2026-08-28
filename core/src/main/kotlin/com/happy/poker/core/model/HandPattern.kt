package com.happy.poker.core.model

/**
 * 牌型
 */
enum class PatternType(val displayName: String, val priority: Int) {
    Invalid("无效", 0),
    Single("单张", 1),
    Pair("对子", 2),
    Triple("三条", 3),
    TripleWithOne("三带一", 4),
    TripleWithPair("三带二", 5),
    Straight("顺子", 6),
    ConsecutivePairs("连对", 7),
    Plane("飞机", 8),
    PlaneWithWings("飞机带翅膀", 9),
    Bomb("炸弹", 10),
    Rocket("火箭", 11),
    FourWithTwo("四带二", 12),
    FourWithPairs("四带两对", 13)
}

/**
 * 牌型
 */
data class HandPattern(
    val type: PatternType,
    val mainRank: Rank,
    val cardCount: Int,
    val groupCount: Int = 0,
    val kickerCount: Int = 0
) {
    /**
     * 是否有效
     */
    val isValid: Boolean get() = type != PatternType.Single || cardCount == 1

    /**
     * 是否是炸弹
     */
    val isBomb: Boolean get() = type == PatternType.Bomb

    /**
     * 是否是火箭
     */
    val isRocket: Boolean get() = type == PatternType.Rocket

    /**
     * 是否是炸弹或火箭
     */
    val isBombOrRocket: Boolean get() = isBomb || isRocket

    /**
     * 获取牌型描述
     */
    val description: String
        get() = buildString {
            append(type.displayName)
            if (type == PatternType.Bomb || type == PatternType.Rocket) {
                append(" ${mainRank.displayName}")
            } else if (type == PatternType.Straight || type == PatternType.ConsecutivePairs) {
                val endValue = mainRank.value - cardCount + groupCount
                val endRank = Rank.entries.firstOrNull { it.value == endValue }
                if (endRank != null) {
                    append(" ${mainRank.displayName}到${endRank.displayName}")
                } else {
                    append(" ${mainRank.displayName}")
                }
            } else {
                append(" ${mainRank.displayName}")
            }
        }

    companion object {
        /**
         * 无效牌型
         */
        val Invalid = HandPattern(PatternType.Single, Rank.Three, 0)

        /**
         * 创建单张
         */
        fun single(rank: Rank) = HandPattern(PatternType.Single, rank, 1)

        /**
         * 创建对子
         */
        fun pair(rank: Rank) = HandPattern(PatternType.Pair, rank, 2)

        /**
         * 创建三条
         */
        fun triple(rank: Rank) = HandPattern(PatternType.Triple, rank, 3)

        /**
         * 创建三带一
         */
        fun tripleWithOne(rank: Rank) = HandPattern(PatternType.TripleWithOne, rank, 4)

        /**
         * 创建三带二
         */
        fun tripleWithPair(rank: Rank) = HandPattern(PatternType.TripleWithPair, rank, 5)

        /**
         * 创建顺子
         */
        fun straight(startRank: Rank, count: Int) = HandPattern(PatternType.Straight, startRank, count)

        /**
         * 创建连对
         */
        fun consecutivePairs(startRank: Rank, pairCount: Int) = HandPattern(PatternType.ConsecutivePairs, startRank, pairCount * 2, pairCount)

        /**
         * 创建飞机
         */
        fun plane(startRank: Rank, groupCount: Int) = HandPattern(PatternType.Plane, startRank, groupCount * 3, groupCount)

        /**
         * 创建飞机带翅膀
         */
        fun planeWithWings(startRank: Rank, groupCount: Int, kickerCount: Int) = HandPattern(PatternType.PlaneWithWings, startRank, groupCount * 3 + kickerCount, groupCount, kickerCount)

        /**
         * 创建炸弹
         */
        fun bomb(rank: Rank) = HandPattern(PatternType.Bomb, rank, 4)

        /**
         * 创建火箭
         */
        fun rocket() = HandPattern(PatternType.Rocket, Rank.BigJoker, 2)

        /**
         * 创建四带二
         */
        fun fourWithTwo(rank: Rank) = HandPattern(PatternType.FourWithTwo, rank, 6)

        /**
         * 创建四带两对
         */
        fun fourWithPairs(rank: Rank) = HandPattern(PatternType.FourWithPairs, rank, 8)
    }
}

/**
 * 牌型比较结果
 */
enum class CompareResult {
    Win,    // 胜利
    Lose,   // 失败
    Draw    // 平局（同牌型同点数）
}

/**
 * 扩展函数：比较两个牌型
 */
fun HandPattern.compare(other: HandPattern): CompareResult {
    // 火箭最大
    if (isRocket) return CompareResult.Win
    if (other.isRocket) return CompareResult.Lose

    // 炸弹可以压非炸弹
    if (isBomb && !other.isBomb) return CompareResult.Win
    if (!isBomb && other.isBomb) return CompareResult.Lose

    // 同类型比较
    if (type != other.type) return CompareResult.Lose
    if (cardCount != other.cardCount) return CompareResult.Lose

    return when {
        mainRank.value > other.mainRank.value -> CompareResult.Win
        mainRank.value < other.mainRank.value -> CompareResult.Lose
        else -> CompareResult.Draw
    }
}

/**
 * 扩展函数：判断是否能压过另一个牌型
 */
fun HandPattern.canBeat(other: HandPattern): Boolean {
    return compare(other) == CompareResult.Win
}
