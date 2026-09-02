package com.happy.poker.app.effects

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.happy.poker.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.abs
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

@Composable
private fun BombEffectScreen(
    multiplier: Int,
    bombCount: Int,
    onComplete: () -> Unit
) {
    BombComboEffectScreen(
        title = if (bombCount > 1) "${bombCount}连炸" else "炸弹",
        subtitle = "炸弹翻倍",
        multiplier = multiplier,
        comboCount = bombCount.coerceAtLeast(1),
        onComplete = onComplete
    )
}

@Composable
private fun BombComboEffectScreen(
    title: String,
    subtitle: String,
    multiplier: Int,
    comboCount: Int,
    onComplete: () -> Unit
) {
    val particles = remember(comboCount) { List(42 + comboCount * 8) { BombParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "bomb_combo")
    val burst by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "burst"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800)
        onComplete()
    }

    EffectScrim {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Color(0xFFFFD84A),
                radius = 92f + burst * 220f,
                center = center,
                alpha = (1f - burst).coerceIn(0f, 1f),
                style = Stroke(width = 8f)
            )
            repeat(18) { index ->
                val angle = (index / 18f) * (PI * 2f).toFloat()
                val startDistance = 34f + burst * 70f
                val endDistance = 130f + burst * 260f
                drawLine(
                    color = if (index % 2 == 0) Color(0xFFFFE66D) else Color(0xFFFF4D22),
                    start = Offset(
                        center.x + cos(angle) * startDistance,
                        center.y + sin(angle) * startDistance
                    ),
                    end = Offset(
                        center.x + cos(angle) * endDistance,
                        center.y + sin(angle) * endDistance
                    ),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round,
                    alpha = (1f - burst * 0.75f).coerceIn(0f, 1f)
                )
            }
            particles.forEach { particle ->
                val distance = particle.distance * burst
                drawCircle(
                    color = particle.color,
                    radius = particle.size * (1f - burst * 0.35f),
                    center = Offset(
                        x = center.x + cos(particle.angle) * distance,
                        y = center.y + sin(particle.angle) * distance
                    ),
                    alpha = (1f - burst).coerceIn(0f, 1f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
        ) {
            BombCardFan(cardCount = 4 + (comboCount - 1).coerceIn(0, 2))
            Spacer(modifier = Modifier.height(10.dp))
            EffectTitleCard(
                title = title,
                subtitle = subtitle,
                multiplier = multiplier,
                accentColor = Color(0xFFFFD84A)
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
    val bloom by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "bloom"
    )
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "drift"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }

    EffectScrim {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(24) { index ->
                val angle = (index / 24f) * (PI * 2f).toFloat() + drift * 0.8f
                val radius = 86f + (index % 4) * 28f + drift * 52f
                drawCircle(
                    color = if (index % 2 == 0) Color(0xFFFF8CC8) else Color(0xFF8BE36A),
                    radius = 10f + (index % 3) * 3f,
                    center = Offset(
                        center.x + cos(angle) * radius,
                        center.y + sin(angle) * radius * 0.62f
                    ),
                    alpha = 0.62f
                )
            }
        }

        EffectTitleCard(
            title = "春天",
            subtitle = "全场翻倍",
            multiplier = multiplier,
            accentColor = Color(0xFFFF8CC8),
            modifier = Modifier.graphicsLayer {
                scaleX = bloom
                scaleY = bloom
            },
        )
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
    val lift by infiniteTransition.animateFloat(
        initialValue = 90f,
        targetValue = -110f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "lift"
    )
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800)
        onComplete()
    }

    EffectScrim {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            drawLine(
                color = Color(0xFF4FC3F7),
                start = Offset(centerX, size.height * 0.18f),
                end = Offset(centerX, size.height * 0.82f),
                strokeWidth = 18f,
                cap = StrokeCap.Round,
                alpha = glow * 0.40f
            )
            drawCircle(
                color = Color(0xFF77E5FF),
                radius = 130f + glow * 90f,
                center = Offset(centerX, size.height / 2f),
                alpha = 0.22f * glow,
                style = Stroke(width = 12f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationY = lift
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-28).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.joker_small),
                    contentDescription = "小王",
                    modifier = Modifier
                        .width(74.dp)
                        .height(96.dp)
                        .graphicsLayer { rotationZ = -8f },
                    contentScale = ContentScale.Fit
                )
                Image(
                    painter = painterResource(id = R.drawable.joker_big),
                    contentDescription = "大王",
                    modifier = Modifier
                        .width(78.dp)
                        .height(100.dp)
                        .graphicsLayer { rotationZ = 8f },
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            EffectTitleCard(
                title = "王炸",
                subtitle = "火箭翻倍",
                multiplier = multiplier,
                accentColor = Color(0xFF77E5FF)
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
    BombComboEffectScreen(
        title = "双炸弹",
        subtitle = "连续压制",
        multiplier = multiplier,
        comboCount = 2,
        onComplete = onComplete
    )
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
    BombComboEffectScreen(
        title = "${bombCount}连炸",
        subtitle = "倍数飙升",
        multiplier = multiplier,
        comboCount = bombCount.coerceAtLeast(3),
        onComplete = onComplete
    )
}

@Composable
private fun EffectScrim(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun EffectTitleCard(
    title: String,
    subtitle: String,
    multiplier: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xF51D2330),
                        Color(0xF5421B12),
                        Color(0xF51D2330)
                    )
                ),
                shape = shape
            )
            .border(2.dp, accentColor.copy(alpha = 0.86f), shape)
            .padding(horizontal = 32.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = accentColor,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.86f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "x$multiplier",
            color = Color(0xFFFFD84A),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BombCardFan(cardCount: Int) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        repeat(cardCount.coerceIn(4, 6)) { index ->
            val middle = (cardCount - 1) / 2f
            val distanceFromMiddle = abs(index - middle)
            Image(
                painter = painterResource(id = R.drawable.card_back),
                contentDescription = "炸弹牌",
                modifier = Modifier
                    .width(58.dp)
                    .height(80.dp)
                    .graphicsLayer {
                        rotationZ = (index - middle) * 12f
                        translationX = (index - middle) * 34f
                        translationY = distanceFromMiddle * 7f
                    },
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

/**
 * 炸弹粒子
 */
data class BombParticle(
    val angle: Float = Random.nextFloat() * (PI * 2f).toFloat(),
    val distance: Float = Random.nextFloat() * 260f + 90f,
    val size: Float = Random.nextFloat() * 20f + 5f,
    val color: Color = listOf(
        Color(0xFFFF2A1F),
        Color(0xFFFF8A00),
        Color(0xFFFFD700),
        Color(0xFFFFFFFF)
    ).random()
)
