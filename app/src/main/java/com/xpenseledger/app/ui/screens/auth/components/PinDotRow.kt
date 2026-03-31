package com.xpenseledger.app.ui.screens.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xpenseledger.app.ui.theme.ColorSuccess
import com.xpenseledger.app.ui.theme.DarkError
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary

// ── State ─────────────────────────────────────────────────────────────────────

enum class PinDotState { NORMAL, SUCCESS, FAILURE }

// ── Colour constants ──────────────────────────────────────────────────────────

private val CyanAccent   = Color(0xFF00E5FF)   // vivid cyan glow
private val PinGrad1     = Color(0xFF00D4A0)   // teal
private val PinGrad2     = CyanAccent          // cyan highlight

// ── Component ─────────────────────────────────────────────────────────────────

/**
 * Premium animated PIN dot row.
 *
 * Visual design:
 *  • Empty  → faint white ring, no fill
 *  • Filled → teal→cyan gradient fill + outer glow drawn via [drawBehind]
 *  • Success → scale 1.4× spring + green gradient
 *  • Failure → red shake + DarkError fill
 *
 * Performance: each dot only recomposes when its own filled/state changes
 * because [animateFloatAsState] / [animateColorAsState] are keyed individually.
 */
@Composable
fun PinDotRow(
    filledCount: Int,
    maxCount:    Int        = AuthViewModel_MAX_PIN_LENGTH,
    state:       PinDotState = PinDotState.NORMAL,
    modifier:    Modifier   = Modifier
) {
    val shakeX = remember { Animatable(0f) }

    LaunchedEffect(state) {
        if (state == PinDotState.FAILURE) {
            for (off in floatArrayOf(18f, -18f, 13f, -13f, 7f, -7f, 0f))
                shakeX.animateTo(off, tween(48))
        } else {
            shakeX.animateTo(0f, tween(80))
        }
    }

    Row(
        modifier              = modifier.offset { IntOffset(shakeX.value.toInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(maxCount) { index ->
            PinDot(
                filled = index < filledCount,
                state  = state,
                index  = index
            )
        }
    }
}

// ── Single dot ────────────────────────────────────────────────────────────────

@Composable
private fun PinDot(filled: Boolean, state: PinDotState, index: Int) {

    // Scale: empty stays 1f, filled pops in via spring (0.6 → 1.0), success overshoots
    val scale by animateFloatAsState(
        targetValue = when {
            filled && state == PinDotState.SUCCESS -> 1.40f
            filled  -> 1.10f   // slight pop-in so every dot — including the 6th — is clearly seen
            else    -> 1.00f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "dotScale$index"
    )

    // Glow radius: grows when filled
    val glowRadius by animateFloatAsState(
        targetValue   = if (filled) 14f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label         = "glowRadius$index"
    )

    // Glow colour
    val glowColor by animateColorAsState(
        targetValue = when (state) {
            PinDotState.SUCCESS -> ColorSuccess.copy(alpha = 0.55f)
            PinDotState.FAILURE -> DarkError.copy(alpha = 0.55f)
            PinDotState.NORMAL  -> if (filled) CyanAccent.copy(alpha = 0.45f)
                                   else Color.Transparent
        },
        animationSpec = tween(220),
        label = "glowColor$index"
    )

    // Fill gradient
    val fillBrush: Brush = when {
        !filled -> Brush.radialGradient(listOf(Color.White.copy(0.06f), Color.White.copy(0.06f)))
        state == PinDotState.SUCCESS ->
            Brush.linearGradient(listOf(ColorSuccess, ColorSuccess.copy(0.75f)))
        state == PinDotState.FAILURE ->
            Brush.radialGradient(listOf(DarkError, DarkError.copy(0.75f)))
        else ->
            Brush.linearGradient(listOf(PinGrad1, PinGrad2))
    }

    // Border colour
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            PinDotState.SUCCESS -> ColorSuccess
            PinDotState.FAILURE -> DarkError
            PinDotState.NORMAL  -> if (filled) CyanAccent else Color.White.copy(alpha = 0.22f)
        },
        animationSpec = tween(200),
        label = "borderCol$index"
    )

    Box(
        modifier = Modifier
            .size(20.dp)
            .scale(scale)
            // Soft outer glow drawn on a canvas layer (no overdraw on siblings)
            .drawBehind {
                if (glowRadius > 0f) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            radius = glowRadius * 2f
                        ),
                        radius = glowRadius * 2f
                    )
                }
            }
            // Ring border
            .drawWithCache {
                val borderPx = 1.5.dp.toPx()
                onDrawWithContent {
                    drawContent()
                    drawCircle(
                        color  = borderColor,
                        radius = size.minDimension / 2f - borderPx / 2f,
                        style  = androidx.compose.ui.graphics.drawscope.Stroke(borderPx)
                    )
                }
            }
            // Fill
            .drawBehind {
                drawCircle(brush = fillBrush)
            },
        contentAlignment = Alignment.Center
    ) {}
}

private const val AuthViewModel_MAX_PIN_LENGTH = 6
