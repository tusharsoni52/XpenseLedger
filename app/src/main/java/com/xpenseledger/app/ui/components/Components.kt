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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xpenseledger.app.ui.theme.NeumorphicShadowDark
import com.xpenseledger.app.ui.theme.NeumorphicShadowLight
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary

// ── BrandGradient ──────────────────────────────────────────────────
/** Cyan → indigo gradient used for primary actions throughout the app. */
val BrandGradient: Brush
    get() = Brush.linearGradient(
        listOf(XpensePrimary, XpenseSecondary),
        start = Offset(0f, 0f),
        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

// ── Neumorphic shadow Modifier ─────────────────────────────────────

/**
 * Draws two soft-shadow layers behind the composable to create a neumorphic
 * "floating slab" effect on dark backgrounds.
 *
 * - [darkShadow]  is cast toward the bottom-right (depth).
 * - [lightShadow] is cast toward the top-left   (highlight).
 *
 * Both are drawn with a large [blurRadius] and very low alpha so they blend
 * softly rather than cutting hard edges.
 *
 * ⚠️ Use only on dark (#0F172A) backgrounds — neumorphic shadows only read
 * correctly when the background and surface are close in lightness.
 */
fun Modifier.neumorphicShadow(
    elevation:   Dp    = 8.dp,
    cornerRadius: Dp   = 20.dp,
    darkShadow:  Color = NeumorphicShadowDark,
    lightShadow: Color = NeumorphicShadowLight
): Modifier = this.drawBehind {
    val radiusPx    = cornerRadius.toPx()
    val elevationPx = elevation.toPx()

    drawIntoCanvas { canvas ->
        // ── Dark shadow  (bottom-right) ─────────────────────────────────────
        val darkPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color       = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    elevationPx * 1.5f,
                    elevationPx * 0.6f,
                    elevationPx * 0.6f,
                    darkShadow.copy(alpha = 0.85f).toArgb()
                )
            }
        }
        canvas.drawRoundRect(
            left   = elevationPx,
            top    = elevationPx,
            right  = size.width  - elevationPx,
            bottom = size.height - elevationPx,
            radiusX = radiusPx,
            radiusY = radiusPx,
            paint   = darkPaint
        )

        // ── Light shadow (top-left) ─────────────────────────────────────────
        val lightPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color       = android.graphics.Color.TRANSPARENT
                setShadowLayer(
                    elevationPx * 1.5f,
                    -elevationPx * 0.6f,
                    -elevationPx * 0.6f,
                    lightShadow.copy(alpha = 0.55f).toArgb()
                )
            }
        }
        canvas.drawRoundRect(
            left   = elevationPx,
            top    = elevationPx,
            right  = size.width  - elevationPx,
            bottom = size.height - elevationPx,
            radiusX = radiusPx,
            radiusY = radiusPx,
            paint   = lightPaint
        )
    }
}

// ── NeumorphicCard ─────────────────────────────────────────────────

/**
 * A soft-UI card with dual neumorphic shadows and a subtle top-edge
 * highlight border that simulates light hitting the surface from above.
 *
 * Drop-in replacement for Material3 [Card] in dark contexts.
 *
 * @param cornerRadius  Shape radius. Defaults to 20dp (neumorphic standard).
 * @param elevation     Shadow size/distance. 6–12dp recommended.
 * @param backgroundColor The card fill — should be close to but slightly
 *                        lighter than the page background.
 */
@Composable
fun NeumorphicCard(
    modifier:         Modifier = Modifier,
    cornerRadius:     Dp       = 20.dp,
    elevation:        Dp       = 8.dp,
    backgroundColor:  Color    = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neumorphicShadow(
                elevation    = elevation,
                cornerRadius = cornerRadius
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            // Subtle top-left highlight border — neumorphic "light edge"
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = 0.10f),
                        0.50f to Color.White.copy(alpha = 0.03f),
                        1.00f to Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

// ── GradientButton ─────────────────────────────────────────────────

/**
 * Pill-shaped primary action button with a cyan→indigo gradient fill.
 *
 * On press:
 *  - Spring-based 0.93× scale-down for tactile feedback
 *  - White ripple effect
 *
 * @param fullWidth  When true, stretches to [fillMaxWidth]. Default true.
 */
@Composable
fun GradientButton(
    text:      String,
    onClick:   () -> Unit,
    modifier:  Modifier = Modifier,
    enabled:   Boolean  = true,
    fullWidth: Boolean  = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed        by interactionSource.collectIsPressedAsState()
    val scale            by animateFloatAsState(
        targetValue   = if (isPressed) 0.93f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "btnScale"
    )

    val baseModifier = if (fullWidth) modifier.fillMaxWidth() else modifier

    Box(
        modifier = baseModifier
            .scale(scale)
            .neumorphicShadow(elevation = 6.dp, cornerRadius = 50.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) BrandGradient
                else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.outline,
                        MaterialTheme.colorScheme.outline
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = ripple(bounded = true, color = Color.White),
                enabled           = enabled,
                role              = Role.Button,
                onClick           = onClick
            )
            .padding(PaddingValues(horizontal = 24.dp, vertical = 15.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = if (enabled) Color.White
                         else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── InputField ─────────────────────────────────────────────────────

/**
 * Neumorphic-styled [OutlinedTextField].
 *
 * Focus state shows a soft cyan glow border.
 * Disabled state is visually dimmed.
 */
@Composable
fun InputField(
    value:         String,
    onValueChange: (String) -> Unit,
    label:         String,
    modifier:      Modifier    = Modifier,
    isPassword:    Boolean     = false,
    singleLine:    Boolean     = true,
    enabled:       Boolean     = true,
    readOnly:      Boolean     = false,
    keyboardType:  KeyboardType = KeyboardType.Text,
    trailingIcon:  @Composable (() -> Unit)? = null,
    leadingIcon:   @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = { Text(label) },
        modifier             = modifier.fillMaxWidth(),
        singleLine           = singleLine,
        enabled              = enabled,
        readOnly             = readOnly,
        shape                = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation()
                               else VisualTransformation.None,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon         = trailingIcon,
        leadingIcon          = leadingIcon,
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor        = XpensePrimary,
            unfocusedBorderColor      = MaterialTheme.colorScheme.outline,
            focusedLabelColor         = XpensePrimary,
            unfocusedLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor               = XpensePrimary,
            focusedContainerColor     = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor   = MaterialTheme.colorScheme.surface,
            focusedTextColor          = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor        = MaterialTheme.colorScheme.onSurface,
            disabledTextColor         = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor       = MaterialTheme.colorScheme.outline.copy(0.4f),
            disabledLabelColor        = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    )
}

// ── SectionHeader ──────────────────────────────────────────────────

/** Bold section label with a cyan→indigo gradient accent underline. */
@Composable
fun SectionHeader(
    title:    String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleMedium,
            color      = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandGradient)
        )
    }
}
