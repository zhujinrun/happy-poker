package com.happy.poker.app.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

/**
 * 卡牌动画工具
 */
object CardAnimation {
    /**
     * 出牌动画
     */
    @Composable
    fun DealAnimation(
        modifier: Modifier = Modifier,
        delay: Int = 0,
        content: @Composable () -> Unit
    ) {
        val transition = updateTransition(targetState = true, label = "deal")
        
        val alpha by transition.animateFloat(
            transitionSpec = {
                tween(durationMillis = 300, delayMillis = delay)
            },
            label = "alpha"
        ) { if (it) 1f else 0f }
        
        val offsetY by transition.animateFloat(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            },
            label = "offsetY"
        ) { if (it) 0f else -100f }
        
        Box(
            modifier = modifier.graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
            }
        ) {
            content()
        }
    }

    /**
     * 选中动画
     */
    @Composable
    fun SelectAnimation(
        isSelected: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val transition = updateTransition(targetState = isSelected, label = "select")
        
        val offsetY by transition.animateFloat(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            },
            label = "offsetY"
        ) { if (it) -20f else 0f }
        
        val scale by transition.animateFloat(
            transitionSpec = {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            },
            label = "scale"
        ) { if (it) 1.1f else 1f }
        
        Box(
            modifier = modifier.graphicsLayer {
                this.translationY = offsetY
                this.scaleX = scale
                this.scaleY = scale
            }
        ) {
            content()
        }
    }

    /**
     * 出牌飞出动画
     */
    @Composable
    fun PlayOutAnimation(
        isPlaying: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val transition = updateTransition(targetState = isPlaying, label = "playOut")
        
        val alpha by transition.animateFloat(
            transitionSpec = {
                tween(durationMillis = 200)
            },
            label = "alpha"
        ) { if (it) 0f else 1f }
        
        val offsetY by transition.animateFloat(
            transitionSpec = {
                tween(durationMillis = 300)
            },
            label = "offsetY"
        ) { if (it) -200f else 0f }
        
        val scale by transition.animateFloat(
            transitionSpec = {
                tween(durationMillis = 300)
            },
            label = "scale"
        ) { if (it) 0.5f else 1f }
        
        if (alpha > 0f) {
            Box(
                modifier = modifier.graphicsLayer {
                    this.alpha = alpha
                    this.translationY = offsetY
                    this.scaleX = scale
                    this.scaleY = scale
                }
            ) {
                content()
            }
        }
    }

    /**
     * 炸弹动画
     */
    @Composable
    fun BombAnimation(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "bomb")
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 5f,
            animationSpec = infiniteRepeatable(
                animation = tween(100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotation"
        )
        
        Box(
            modifier = modifier.graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.rotationZ = rotation
            }
        ) {
            content()
        }
    }

    /**
     * 胜利动画
     */
    @Composable
    fun WinAnimation(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "win")
        
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = modifier.graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
            }
        ) {
            content()
        }
    }

    /**
     * 淡入动画
     */
    @Composable
    fun FadeInAnimation(
        modifier: Modifier = Modifier,
        delay: Int = 0,
        content: @Composable () -> Unit
    ) {
        val transition = updateTransition(targetState = true, label = "fadeIn")
        
        val alpha by transition.animateFloat(
            transitionSpec = {
                tween(durationMillis = 500, delayMillis = delay)
            },
            label = "alpha"
        ) { if (it) 1f else 0f }
        
        Box(
            modifier = modifier.graphicsLayer {
                this.alpha = alpha
            }
        ) {
            content()
        }
    }
}
