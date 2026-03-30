package com.xpenseledger.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand ──────────────────────────────────────────────────────────
val XpensePrimary         = Color(0xFF22D3EE)   // Cyan accent
val XpensePrimaryDim      = Color(0xFF0E9BB0)   // Darker cyan
val XpenseSecondary       = Color(0xFF6366F1)   // Indigo
val XpenseSecondaryDim    = Color(0xFF4547C4)
val XpenseAccent          = Color(0xFFF59E0B)   // Amber (warnings / highlights)

// ── Dark scheme ────────────────────────────────────────────────────
val DarkBackground        = Color(0xFF0F172A)   // Neumorphic base
val DarkSurface           = Color(0xFF1E293B)   // Card surface
val DarkSurfaceVariant    = Color(0xFF263348)   // Slightly lighter surface
val DarkSurfaceHigh       = Color(0xFF2D3F55)   // Top-layer surfaces
val DarkOnPrimary         = Color(0xFF0D1B24)
val DarkOnSecondary       = Color(0xFF12133A)
val DarkOnBackground      = Color(0xFFE2E8F0)   // Slate-200
val DarkOnSurface         = Color(0xFFCBD5E1)   // Slate-300
val DarkOnSurfaceVariant  = Color(0xFF94A3B8)   // Slate-400
val DarkOutline           = Color(0xFF334155)   // Slate-700
val DarkError             = Color(0xFFF87171)   // Red-400
val DarkOnError           = Color(0xFF3D0010)

// ── Light scheme ───────────────────────────────────────────────────
val LightBackground       = Color(0xFFF1F5F9)
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceVariant   = Color(0xFFE2E8F0)
val LightOnPrimary        = Color(0xFFFFFFFF)
val LightOnSecondary      = Color(0xFFFFFFFF)
val LightOnBackground     = Color(0xFF0F172A)
val LightOnSurface        = Color(0xFF1E293B)
val LightOnSurfaceVariant = Color(0xFF475569)
val LightOutline          = Color(0xFFCBD5E1)
val LightError            = Color(0xFFDC2626)
val LightOnError          = Color(0xFFFFFFFF)

// ── Semantic (theme-independent helpers) ───────────────────────────
val ColorSuccess  = Color(0xFF34D399)   // Emerald-400
val ColorWarning  = Color(0xFFF59E0B)   // Amber-400
val ColorExpense  = Color(0xFFF87171)   // Red-400
val ColorIncome   = Color(0xFF34D399)   // Emerald-400

// ── Neumorphic shadow helpers ───────────────────────────────────────
/** Dark shadow for neumorphic depth (cast downward). */
val NeumorphicShadowDark  = Color(0xFF080F1E)
/** Light shadow for neumorphic highlight (cast upward). */
val NeumorphicShadowLight = Color(0xFF1E3147)
