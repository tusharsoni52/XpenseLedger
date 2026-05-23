package com.xpenseledger.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand — Midnight Slate Premium ─────────────────────────────────
//  Calm navy base · soft indigo primary · warm amber accent
//  Designed for eye comfort, long sessions, and premium fintech feel
// ───────────────────────────────────────────────────────────────────

val XpensePrimary         = Color(0xFF6C8EF5)   // Soft Indigo-Blue  — primary actions
val XpensePrimaryDim      = Color(0xFF4F6FD8)   // Deeper Indigo     — pressed / dim
val XpenseSecondary       = Color(0xFFA78BFA)   // Soft Violet       — accent highlights
val XpenseSecondaryDim    = Color(0xFF8B6FE8)   // Dim Violet        — dim accent
val XpenseAccent          = Color(0xFFF59E0B)   // Warm Amber        — currency / warnings

// ── Dark scheme ────────────────────────────────────────────────────
val DarkBackground        = Color(0xFF0F1923)   // Midnight navy     — main bg
val DarkSurface           = Color(0xFF1A2535)   // Dark slate        — panels
val DarkSurfaceVariant    = Color(0xFF243044)   // Elevated slate    — cards & widgets
val DarkSurfaceHigh       = Color(0xFF2E3C56)   // High elevation    — elevated cards
val DarkOnPrimary         = Color(0xFF0D1440)   // On indigo button
val DarkOnSecondary       = Color(0xFF1A0F3D)   // On violet accent
val DarkOnBackground      = Color(0xFFE2E8F0)   // Primary text      — soft white
val DarkOnSurface         = Color(0xFFCBD5E1)   // Secondary text    — muted slate
val DarkOnSurfaceVariant  = Color(0xFF94A3B8)   // Tertiary text
val DarkOutline           = Color(0xFF2D3F5C)   // Subtle blue-tinted border
val DarkError             = Color(0xFFF87171)   // Rose-400          — financial errors
val DarkOnError           = Color(0xFF3D0010)

// ── Light scheme ───────────────────────────────────────────────────
val LightBackground       = Color(0xFFF5F7FF)   // Lavender-tinted light bg
val LightSurface          = Color(0xFFFFFFFF)
val LightSurfaceVariant   = Color(0xFFEEF2FF)   // Indigo tinted surface
val LightOnPrimary        = Color(0xFFFFFFFF)
val LightOnSecondary      = Color(0xFF1A0F3D)
val LightOnBackground     = Color(0xFF1E293B)   // Slate-800
val LightOnSurface        = Color(0xFF334155)   // Slate-700
val LightOnSurfaceVariant = Color(0xFF475569)   // Slate-600
val LightOutline          = Color(0xFFC7D2FE)   // Indigo-200
val LightError            = Color(0xFFDC2626)
val LightOnError          = Color(0xFFFFFFFF)

// ── Semantic (theme-independent — never change these) ───────────────
val ColorSuccess  = Color(0xFF34D399)   // Mint green    — income / success
val ColorWarning  = Color(0xFFF59E0B)   // Amber         — warning / budget alert
val ColorExpense  = Color(0xFFF87171)   // Rose          — expense / loss
val ColorIncome   = Color(0xFF34D399)   // Mint green    — income

// ── Glassmorphism card helpers ──────────────────────────────────────
val GlassCardFill       = Color(0x1A6C8EF5)   // 10% indigo  — card body tint
val GlassCardBorderTop  = Color(0x336C8EF5)   // 20% indigo  — bright edge
val GlassCardBorderBot  = Color(0x0A6C8EF5)   // 4%  indigo  — dim edge
val GlassCardHighlight  = Color(0x14FFFFFF)   // inner top shimmer

// ── Shadow helpers (calibrated for midnight navy backgrounds) ───────
val NeumorphicShadowDark  = Color(0xFF070D14)   // deep navy shadow
val NeumorphicShadowLight = Color(0xFF1E2D42)   // elevated navy highlight
