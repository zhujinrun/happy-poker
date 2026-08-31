package com.happy.poker.app.progress

import android.content.Context
import android.content.SharedPreferences
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

data class BeanSettlementResult(
    val delta: Int,
    val balance: Int,
    val applied: Boolean
)

class PlayerProgressManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "player_progress"
        private const val KEY_BEAN_BALANCE = "bean_balance"
        private const val KEY_LAST_SETTLED_GAME_KEY = "last_settled_game_key"
        private const val KEY_BEAN_MIGRATION_DONE = "bean_migration_done"
        private const val LEGACY_PLACEHOLDER_BEAN_BALANCE = 11440
        const val INITIAL_BEAN_BALANCE = 1000
        private const val DEFAULT_BEAN_BALANCE = INITIAL_BEAN_BALANCE
        private const val MIN_BEAN_BALANCE = 0
    }

    init {
        migrateLegacyBeanBalanceIfNeeded()
    }

    fun getBeanBalance(): Int {
        migrateLegacyBeanBalanceIfNeeded()
        return prefs.getInt(KEY_BEAN_BALANCE, DEFAULT_BEAN_BALANCE)
    }

    fun settleGameResult(gameKey: String, scoreChange: Int): BeanSettlementResult {
        val currentBalance = getBeanBalance()
        if (gameKey.isBlank()) {
            return BeanSettlementResult(0, currentBalance, false)
        }

        val lastSettledKey = prefs.getString(KEY_LAST_SETTLED_GAME_KEY, null)
        if (lastSettledKey == gameKey) {
            return BeanSettlementResult(0, currentBalance, false)
        }

        val nextBalance = (currentBalance + scoreChange).coerceAtLeast(MIN_BEAN_BALANCE)
        val actualDelta = nextBalance - currentBalance
        prefs.edit()
            .putInt(KEY_BEAN_BALANCE, nextBalance)
            .putString(KEY_LAST_SETTLED_GAME_KEY, gameKey)
            .apply()

        return BeanSettlementResult(
            delta = actualDelta,
            balance = nextBalance,
            applied = true
        )
    }

    fun resetBeanBalance(balance: Int = DEFAULT_BEAN_BALANCE) {
        prefs.edit()
            .putInt(KEY_BEAN_BALANCE, balance.coerceAtLeast(MIN_BEAN_BALANCE))
            .remove(KEY_LAST_SETTLED_GAME_KEY)
            .putBoolean(KEY_BEAN_MIGRATION_DONE, true)
            .apply()
    }

    fun formatBeanBalance(balance: Int = getBeanBalance()): String = formatBeanCount(balance)

    private fun migrateLegacyBeanBalanceIfNeeded() {
        if (prefs.getBoolean(KEY_BEAN_MIGRATION_DONE, false)) return

        val currentBalance = prefs.getInt(KEY_BEAN_BALANCE, DEFAULT_BEAN_BALANCE)
        val hasSettlementHistory = !prefs.getString(KEY_LAST_SETTLED_GAME_KEY, null).isNullOrBlank()

        if (currentBalance == LEGACY_PLACEHOLDER_BEAN_BALANCE && !hasSettlementHistory) {
            prefs.edit()
                .putInt(KEY_BEAN_BALANCE, INITIAL_BEAN_BALANCE)
                .putBoolean(KEY_BEAN_MIGRATION_DONE, true)
                .apply()
            return
        }

        prefs.edit()
            .putBoolean(KEY_BEAN_MIGRATION_DONE, true)
            .apply()
    }
}

fun formatBeanCount(count: Int): String {
    val absCount = abs(count)
    return when {
        absCount >= 10_000 -> {
            val formatter = DecimalFormat("0.###", DecimalFormatSymbols(Locale.CHINA))
            "${formatter.format(count / 10_000.0)}万"
        }
        else -> count.toString()
    }
}
