package com.xpenseledger.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// ── Color schemes ──────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary              = XpensePrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = XpensePrimaryDim,
    onPrimaryContainer   = DarkOnBackground,
    secondary            = XpenseSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = XpenseSecondaryDim,
    onSecondaryContainer = DarkOnBackground,
    tertiary             = XpenseAccent,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    outline              = DarkOutline,
    error                = DarkError,
    onError              = DarkOnError,
)

private val LightColorScheme = lightColorScheme(
    primary              = XpensePrimaryDim,
    onPrimary            = LightOnPrimary,
    primaryContainer     = XpensePrimary,
    onPrimaryContainer   = LightOnBackground,
    secondary            = XpenseSecondaryDim,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = XpenseSecondary,
    onSecondaryContainer = LightOnBackground,
    tertiary             = XpenseAccent,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    outline              = LightOutline,
    error                = LightError,
    onError              = LightOnError,
)

// ── Shapes  — neumorphic uses large rounded corners everywhere ──────

val XpenseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── Theme composable ───────────────────────────────────────────────

/**
 * Wrap your entire Compose UI with this theme.
 * [darkTheme] defaults to `true` — the app ships in dark mode.
 * Pass `isSystemInDarkTheme()` at the call site to follow the system setting.
 */
@Composable
fun XpenseLedgerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = XpenseTypography,
        shapes      = XpenseShapes,
        content     = content
    )
}
