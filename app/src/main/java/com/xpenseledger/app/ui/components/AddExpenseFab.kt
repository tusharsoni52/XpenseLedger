package com.xpenseledger.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary

/**
 * Neumorphic FAB for "Add Expense".
 *
 * Design:
 *  • 60dp circle — cyan → indigo gradient fill
 *  • Dual soft neumorphic shadow (dark bottom-right / light top-left)
 *  • Cyan glow ring drawn via [drawBehind]
 *  • 1dp cyan border for crispness
 *  • Spring 0.88× press scale + LongPress haptic
 */
@Composable
fun AddExpenseFab(
    onClick:  () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic            = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .size(60.dp)
            .scale(scale)
            // Dual neumorphic shadow
            .neumorphicShadow(elevation = 10.dp, cornerRadius = 30.dp)
            // Outer cyan glow
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to XpensePrimary.copy(alpha = if (isPressed) 0.50f else 0.28f),
                            0.6f to XpensePrimary.copy(alpha = 0.08f),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.88f
                    ),
                    radius = size.minDimension * 0.88f
                )
            }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(XpensePrimary, XpenseSecondary),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(1.dp, XpensePrimary.copy(alpha = 0.40f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(bounded = true, radius = 30.dp, color = Color.White),
                role              = Role.Button,
                onClick           = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Default.Add,
            contentDescription = "Add Expense",
            tint               = Color.White,
            modifier           = Modifier.size(28.dp)
        )
    }
}
