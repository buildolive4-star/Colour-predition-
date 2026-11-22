package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AmberGoldTop
import com.example.ui.theme.AmberOrangeDeep
import com.example.ui.theme.AmberOrangeMid
import com.example.ui.theme.CoinGoldSoft
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldBright
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.RubyRedBright
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class WinPopupData(
    val title: String,
    val amountWon: Long,
    val subtitle: String,
    val badgeText: String = "CONGRATULATIONS!"
)

@Composable
fun WinCelebrationDialog(
    data: WinPopupData,
    onDismiss: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "win_popup_anim")
    val rayAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "win_rays"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(750, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "win_pulse"
    )
    val confettiPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "win_confetti"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ObsidianCard,
            border = BorderStroke(2.dp, AmberGoldTop),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .testTag("win_celebration_dialog")
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Animated sunburst & confetti particles behind the card content
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp)
                ) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.34f
                    val maxR = size.maxDimension * 0.68f

                    // Soft radial gold aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmberGoldTop.copy(alpha = 0.34f),
                                AmberOrangeMid.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = maxR * 0.72f
                        ),
                        radius = maxR * 0.72f,
                        center = Offset(cx, cy)
                    )

                    // Rotating sunburst beams
                    val rayCount = 12
                    val step = 360f / rayCount
                    for (i in 0 until rayCount) {
                        val a1 = Math.toRadians((rayAngle + i * step).toDouble())
                        val a2 = Math.toRadians((rayAngle + i * step + step * 0.38f).toDouble())
                        val path = Path().apply {
                            moveTo(cx, cy)
                            lineTo(cx + (cos(a1) * maxR).toFloat(), cy + (sin(a1) * maxR).toFloat())
                            lineTo(cx + (cos(a2) * maxR).toFloat(), cy + (sin(a2) * maxR).toFloat())
                            close()
                        }
                        drawPath(path, color = AmberGoldTop.copy(alpha = 0.08f))
                    }

                    // Animated orbiting confetti sparkles
                    val confettiColors = listOf(
                        AmberGoldTop, NeonEmeraldBright, CyanPulse, ElectricVioletBright, RubyRedBright, CoinGoldSoft
                    )
                    for (i in 0 until 18) {
                        val baseAngle = (i * (2.0 * PI / 18.0)) + confettiPhase * 2.0 * PI * 0.25
                        val orbit = 58.dp.toPx() + ((i % 4) * 26.dp.toPx()) + (sin(confettiPhase * 2 * PI + i) * 8.dp.toPx()).toFloat()
                        val px = cx + (cos(baseAngle) * orbit).toFloat()
                        val py = cy + (sin(baseAngle) * orbit * 0.78f).toFloat()
                        drawCircle(
                            color = confettiColors[i % confettiColors.size].copy(alpha = 0.85f),
                            radius = if (i % 3 == 0) 4.dp.toPx() else 2.8.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AmberGoldTop.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, AmberGoldTop.copy(alpha = 0.75f))
                    ) {
                        Text(
                            text = data.badgeText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = AmberGoldTop,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }

                    // 3D Pulsing Trophy Emblem
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(pulse)
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(CoinGoldSoft, AmberGoldTop, AmberOrangeDeep)
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = "Winner Trophy",
                            tint = ObsidianCanvas,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianCanvas.copy(alpha = 0.9f),
                        border = BorderStroke(1.5.dp, NeonEmerald.copy(alpha = 0.75f))
                    ) {
                        Text(
                            text = "+${formatCoins(data.amountWon)} VC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            color = NeonEmeraldBright,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }

                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberOrangeDeep,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("win_popup_collect_button")
                    ) {
                        Text(
                            text = "AWESOME • CONTINUE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
