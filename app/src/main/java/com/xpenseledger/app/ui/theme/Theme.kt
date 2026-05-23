package com.xpenseledger.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

// ── Color schemes — Dark Emerald Premium ───────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary              = XpensePrimary,       // Emerald-500
    onPrimary            = DarkOnPrimary,
    primaryContainer     = XpensePrimaryDim,    // Emerald-600
    onPrimaryContainer   = DarkOnBackground,
    secondary            = XpenseSecondary,     // Lime neon
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = XpenseSecondaryDim,
    onSecondaryContainer = DarkOnBackground,
    tertiary             = XpenseAccent,        // Gold
    background           = DarkBackground,      // #021E18 deep forest
    onBackground         = DarkOnBackground,    // white
    surface              = DarkSurface,         // #032F24 panel
    onSurface            = DarkOnSurface,       // muted slate
    surfaceVariant       = DarkSurfaceVariant,  // #064E3B card
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

// ── Shapes — spec: buttons 16dp, cards 22dp, inputs 14dp, dialogs 24dp ──

val XpenseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // chips, small badges
    small      = RoundedCornerShape(14.dp),  // inputs
    medium     = RoundedCornerShape(16.dp),  // buttons
    large      = RoundedCornerShape(22.dp),  // cards
    extraLarge = RoundedCornerShape(24.dp)   // dialogs / bottom sheets
)

// ── Theme composable ───────────────────────────────────────────────

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
