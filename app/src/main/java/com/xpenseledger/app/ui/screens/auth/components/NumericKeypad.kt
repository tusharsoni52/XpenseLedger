package com.xpenseledger.app.ui.screens.auth.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary

// ── Glass colour tokens ───────────────────────────────────────────────────────

private val CyanAccent        = Color(0xFF22D3EE)       // matches theme XpensePrimary
private val GlassKeyFill      = Color(0x1AFFFFFF)       // 10% white — semi-transparent glass
private val GlassKeyFillPress = Color(0x33FFFFFF)       // 20% white — brightens on press
private val GlassKeyBorder    = Color(0x33FFFFFF)       // 20% white ring
private val ConfirmGrad1      = XpensePrimary           // cyan
private val ConfirmGrad2      = XpenseSecondary         // indigo

/**
 * Glass-style numeric PIN keypad.
 *
 * All digit keys are semi-transparent circles (10% white fill) with a thin
 * white-20% border — matching the glassmorphism card above them.
 *
 * The confirm key retains its cyan→indigo gradient so it reads as the primary
 * action. The backspace key is fully ghost (transparent, border only).
 *
 * Performance: [MutableInteractionSource] is remembered once per button;
 * the only animation is a spring scale — zero allocation per frame.
 */
@Composable
fun NumericKeypad(
    onDigit:        (Int) -> Unit,
    onBackspace:    () -> Unit,
    onConfirm:      () -> Unit,
    confirmEnabled: Boolean,
    enabled:        Boolean,
    modifier:       Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    GlassKeypadButton(
                        enabled = enabled,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigit(digit)
                        }
                    ) {
                        Text(
                            text       = digit.toString(),
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.W400,
                            color      = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }

        // Bottom row: ⌫  0  ✓
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Backspace — fully ghost
            GlassKeypadButton(
                enabled = enabled,
                isGhost = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspace()
                }
            ) {
                Text(
                    text     = "⌫",
                    fontSize = 20.sp,
                    color    = Color.White.copy(alpha = if (enabled) 0.75f else 0.28f)
                )
            }

            // 0
            GlassKeypadButton(
                enabled = enabled,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDigit(0)
                }
            ) {
                Text(
                    text       = "0",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.W400,
                    color      = Color.White.copy(alpha = 0.92f)
                )
            }

            // Confirm — accent gradient (primary action, stays solid)
            GlassKeypadButton(
                enabled     = enabled && confirmEnabled,
                isConfirm   = true,
                onClick     = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm()
                }
            ) {
                Icon(
                    imageVector        = Icons.Default.Done,
                    contentDescription = "Confirm PIN",
                    tint               = if (enabled && confirmEnabled) Color(0xFF050818)
                                         else Color.White.copy(alpha = 0.25f),
                    modifier           = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Glass keypad button ───────────────────────────────────────────────────────

/**
 * Single glass-style circular keypad button.
 *
 * States:
 *  • Normal digit  → 10% white fill + 20% white border
 *  • Pressed       → 20% white fill (brightens, no blur needed)
 *  • Ghost/backsp  → transparent fill, border only
 *  • Confirm       → cyan→indigo solid gradient, stays opaque as primary CTA
 *
 * No neumorphic shadow layers — keeps the glass aesthetic light and crisp.
 */
@Composable
private fun GlassKeypadButton(
    enabled:   Boolean,
    isGhost:   Boolean  = false,
    isConfirm: Boolean  = false,
    onClick:   () -> Unit,
    content:   @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed        by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "glassKeyScale"
    )

    // Fill — glass shifts from 10% → 20% white on press; confirm stays gradient
    val bgBrush: Brush = when {
        isConfirm && enabled -> Brush.linearGradient(
            listOf(ConfirmGrad1, ConfirmGrad2),
            start = Offset(0f, 0f),
            end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
        isGhost -> Brush.radialGradient(          // no fill for backspace
            listOf(Color.Transparent, Color.Transparent)
        )
        isPressed -> Brush.radialGradient(
            listOf(GlassKeyFillPress, GlassKeyFill)
        )
        else -> Brush.radialGradient(
            listOf(GlassKeyFill, GlassKeyFill)
        )
    }

    // Subtle cyan glow behind the confirm button only
    val glowMod = if (isConfirm && enabled) {
        Modifier.drawBehind {
            drawCircle(
                brush  = Brush.radialGradient(
                    listOf(CyanAccent.copy(alpha = 0.28f), Color.Transparent),
                    radius = size.minDimension * 0.95f
                ),
                radius = size.minDimension * 0.95f
            )
        }
    } else Modifier

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.32f)
            .then(glowMod)
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = when {
                    isConfirm && enabled -> CyanAccent.copy(alpha = 0.45f)
                    isGhost              -> Color.White.copy(alpha = 0.18f)
                    else                 -> GlassKeyBorder
                },
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(
                    bounded = true,
                    radius  = 34.dp,
                    color   = if (isConfirm) CyanAccent else Color.White
                ),
                enabled           = enabled,
                role              = Role.Button,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
