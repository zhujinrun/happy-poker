package com.happy.poker.app.effects

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 特效类型
 */
enum class SpecialEffectType {
    Bomb,           // 炸弹爆炸效果
    Spring,         // 春天效果
    Rocket,         // 火箭效果
    DoubleBomb,     // 双炸弹效果
    BombCount       // 多炸弹效果
}

/**
 * 特效状态
 */
data class SpecialEffectState(
    val type: SpecialEffectType,
    val multiplier: Int = 1,
    val bombCount: Int = 0,
    val isActive: Boolean = false
)

/**
 * 特效管理器
 * 管理炸弹和春天等特殊效果
 */
class SpecialEffectsManager {
    private val _effectState = MutableStateFlow(SpecialEffectState(SpecialEffectType.Bomb))
    val effectState: StateFlow<SpecialEffectState> = _effectState.asStateFlow()

    private var dismissJob: kotlinx.coroutines.Job? = null

    /**
     * 触发炸弹效果
     */
    fun triggerBombEffect(multiplier: Int, bombCount: Int) {
        _effectState.value = SpecialEffectState(
            type = SpecialEffectType.Bomb,
            multiplier = multiplier,
            bombCount = bombCount,
            isActive = true
        )
    }

    /**
     * 触发火箭效果
     */
    fun triggerRocketEffect(multiplier: Int) {
        _effectState.value = SpecialEffectState(
            type = SpecialEffectType.Rocket,
            multiplier = multiplier,
            isActive = true
        )
    }

    /**
     * 触发春天效果
     */
    fun triggerSpringEffect(multiplier: Int) {
        _effectState.value = SpecialEffectState(
            type = SpecialEffectType.Spring,
            multiplier = multiplier,
            isActive = true
        )
    }

    /**
     * 触发双炸弹效果
     */
    fun triggerDoubleBombEffect(multiplier: Int) {
        _effectState.value = SpecialEffectState(
            type = SpecialEffectType.DoubleBomb,
            multiplier = multiplier,
            isActive = true
        )
    }

    /**
     * 触发多炸弹效果
     */
    fun triggerMultiBombEffect(multiplier: Int, bombCount: Int) {
        _effectState.value = SpecialEffectState(
            type = SpecialEffectType.BombCount,
            multiplier = multiplier,
            bombCount = bombCount,
            isActive = true
        )
    }

    /**
     * 停止当前效果
     */
    fun stopEffect() {
        _effectState.value = _effectState.value.copy(isActive = false)
    }
}

/**
 * 特效屏幕覆盖层
 */
@Composable
fun SpecialEffectOverlay(
    effectState: SpecialEffectState,
    onEffectComplete: () -> Unit
) {
    if (!effectState.isActive) return

    AnimatedVisibility(
        visible = effectState.isActive,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        when (effectState.type) {
            SpecialEffectType.Bomb -> BombEffectScreen(
                multiplier = effectState.multiplier,
                bombCount = effectState.bombCount,
                onComplete = onEffectComplete
            )
            SpecialEffectType.Spring -> SpringEffectScreen(
                multiplier = effectState.multiplier,
                onComplete = onEffectComplete
            )
            SpecialEffectType.Rocket -> RocketEffectScreen(
                multiplier = effectState.multiplier,
                onComplete = onEffectComplete
            )
            SpecialEffectType.DoubleBomb -> DoubleBombEffectScreen(
                multiplier = effectState.multiplier,
                onComplete = onEffectComplete
            )
            SpecialEffectType.BombCount -> MultiBombEffectScreen(
                multiplier = effectState.multiplier,
                bombCount = effectState.bombCount,
                onComplete = onEffectComplete
            )
        }
    }
}

/**
 * 炸弹爆炸效果
 */
@Composable
private fun BombEffectScreen(
    multiplier: Int,
    bombCount: Int,
    onComplete: () -> Unit
) {
    val particles = remember { List(30) { BombParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "bomb_screen")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onComplete()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            drawCircle(
                color = particle.color,
                radius = particle.size,
                center = Offset(
                    x = size.width * particle.x + (particle.vx * particle.life),
                    y = size.height * particle.y + (particle.vy * particle.life)
                ),
                alpha = alpha
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = 1f + (alpha * 0.2f)
                scaleY = 1f + (alpha * 0.2f)
            }
        ) {
            Text(
                text = "💥",
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "炸弹!",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
            if (bombCount > 1) {
                Text(
                    text = "${bombCount}连炸!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = "x${multiplier}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 春天效果
 */
@Composable
private fun SpringEffectScreen(
    multiplier: Int,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spring_screen")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
        ) {
            Text(
                text = "🌸",
                fontSize = 100.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "春天!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF69B4),
                textAlign = TextAlign.Center
            )
            Text(
                text = "x${multiplier}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 火箭效果
 */
@Composable
private fun RocketEffectScreen(
    multiplier: Int,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rocket_screen")

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 200f,
        targetValue = -200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationY = offsetY
                this.alpha = alpha
            }
        ) {
            Text(
                text = "🚀",
                fontSize = 100.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "火箭!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4169E1),
                textAlign = TextAlign.Center
            )
            Text(
                text = "x${multiplier}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 双炸弹效果
 */
@Composable
private fun DoubleBombEffectScreen(
    multiplier: Int,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "double_bomb_screen")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Text(
                text = "💥💥",
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "双炸弹!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF4500),
                textAlign = TextAlign.Center
            )
            Text(
                text = "x${multiplier}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 多炸弹效果
 */
@Composable
private fun MultiBombEffectScreen(
    multiplier: Int,
    bombCount: Int,
    onComplete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "multi_bomb_screen")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        ) {
            Text(
                text = "💥".repeat(bombCount.coerceAtMost(5)),
                fontSize = 60.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${bombCount}连炸!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF0000),
                textAlign = TextAlign.Center
            )
            Text(
                text = "x${multiplier}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 炸弹粒子
 */
data class BombParticle(
    val x: Float = Random.nextFloat(),
    val y: Float = Random.nextFloat(),
    val vx: Float = (Random.nextFloat() - 0.5f) * 100f,
    val vy: Float = (Random.nextFloat() - 0.5f) * 100f,
    val size: Float = Random.nextFloat() * 20f + 5f,
    val color: Color = listOf(
        Color.Red,
        Color(0xFFFF6B00),
        Color(0xFFFFD700),
        Color(0xFFFF4500)
    ).random(),
    val life: Float = 0f
)
