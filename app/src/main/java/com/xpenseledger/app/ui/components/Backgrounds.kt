package com.xpenseledger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
//  Colour constants — Midnight Slate Premium palette
// ─────────────────────────────────────────────────────────────────────────────

// Dashboard (Home)
private val DashTop    = Color(0xFF0F1923)   // midnight navy
private val DashMid    = Color(0xFF1A2535)   // dark slate
private val DashBottom = Color(0xFF0F1923)   // back to midnight navy

// Soft form background (AddExpense)
private val SoftTop    = Color(0xFF0F1923)
private val SoftBottom = Color(0xFF1A2535)

// Analytics tab
private val AnalyticsTop    = Color(0xFF0F1923)
private val AnalyticsMid    = Color(0xFF172030)
private val AnalyticsBottom = Color(0xFF0F1923)

// Indigo/violet glow colors
private val EmeraldGlow  = Color(0xFF6C8EF5)   // soft indigo — primary
private val LimeGlow     = Color(0xFFA78BFA)   // soft violet — accent
private val GoldGlow     = Color(0xFFF59E0B)   // warm amber  — currency

// ─────────────────────────────────────────────────────────────────────────────
//  DASHBOARD BACKGROUND — Home / Expenses screen
//  Midnight navy gradient + soft indigo/violet radial glows + dot-grid texture + scrim
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen background for the Home / Expenses tab.
 * Midnight navy base with soft indigo-glow orbs and a barely-visible
 * dot-grid texture — premium FinTech analytics feel.
 */
@Composable
fun DashboardBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to DashTop,
                        0.45f to DashMid,
                        1.0f  to DashBottom
                    )
                )
            )
    ) {
        // ── Emerald glow orbs ─────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-right emerald orb
            val topR = size.width * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGlow.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                    radius = topR * 1.4f
                ),
                radius = topR,
                center = Offset(size.width * 0.85f, size.height * 0.08f)
            )
            // Mid-left lime accent orb
            val midR = size.width * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LimeGlow.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.48f),
                    radius = midR * 1.4f
                ),
                radius = midR,
                center = Offset(size.width * 0.12f, size.height * 0.48f)
            )
            // Bottom emerald orb
            val botR = size.width * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGlow.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width * 0.68f, size.height * 0.90f),
                    radius = botR * 1.4f
                ),
                radius = botR,
                center = Offset(size.width * 0.68f, size.height * 0.90f)
            )
        }

        // ── Dot-grid texture (Canvas — no drawable needed) ────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.18f)
        ) {
            val spacing = 48.dp.toPx()
            val dotR    = 1.5.dp.toPx()
            val cols = (size.width  / spacing).toInt() + 2
            val rows = (size.height / spacing).toInt() + 2
            for (row in 0..rows) {
                for (col in 0..cols) {
                    drawCircle(
                        color  = Color.White,
                        radius = dotR,
                        center = Offset(col * spacing, row * spacing)
                    )
                }
            }
        }

        // ── Subtle overlay scrim ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x1A0F1923))
        )

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SOFT GRADIENT BACKGROUND — Add Expense form
//  Calm two-stop navy gradient, single centred glow — form fields prominent
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SoftGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to SoftTop,
                        1.0f to SoftBottom
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top-centre emerald glow — form focus area
            val glowRadius = size.width * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGlow.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.04f),
                    radius = glowRadius * 1.4f
                ),
                radius = glowRadius,
                center = Offset(size.width * 0.50f, size.height * 0.04f)
            )
            // Bottom-right lime accent — subtle depth
            val accentRadius = size.width * 0.26f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LimeGlow.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.78f),
                    radius = accentRadius * 1.4f
                ),
                radius = accentRadius,
                center = Offset(size.width * 0.75f, size.height * 0.78f)
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ANALYTICS BACKGROUND — Compare / month-comparison tab
//  Spreadsheet-inspired: dark navy base + chart grid lines + glow + scrim
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnalyticsBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to AnalyticsTop,
                        0.55f to AnalyticsMid,
                        1.0f  to AnalyticsBottom
                    )
                )
            )
    ) {
        // ── Spreadsheet-style chart grid lines ────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = EmeraldGlow.copy(alpha = 0.06f)
            val lineCount = 8
            val spacing = size.height / (lineCount + 1)
            repeat(lineCount) { i ->
                val y = spacing * (i + 1)
                drawLine(
                    color       = lineColor,
                    start       = Offset(0f, y),
                    end         = Offset(size.width, y),
                    strokeWidth = 1.2f
                )
            }
            // Vertical axis baseline
            drawLine(
                color       = LimeGlow.copy(alpha = 0.05f),
                start       = Offset(size.width * 0.08f, 0f),
                end         = Offset(size.width * 0.08f, size.height),
                strokeWidth = 1f
            )
        }

        // ── Bar-chart decorative (Canvas — no drawable needed) ───────────────
        Canvas(
            modifier = Modifier
                .size(width = 200.dp, height = 125.dp)
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-8).dp)
                .alpha(0.22f)
        ) {
            val barHeights = listOf(0.45f, 0.70f, 0.55f, 0.85f, 0.60f, 0.75f, 0.50f)
            val barW  = size.width / (barHeights.size * 2f)
            val gap   = barW
            barHeights.forEachIndexed { i, frac ->
                val barH = size.height * frac
                val x    = i * (barW + gap) + gap / 2f
                drawRoundRect(
                    color       = LimeGlow.copy(alpha = 0.75f),
                    topLeft     = Offset(x, size.height - barH),
                    size        = androidx.compose.ui.geometry.Size(barW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }

        // ── Emerald glow behind chart area ────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldGlow.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.15f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.78f, size.height * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LimeGlow.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.85f),
                    radius = size.width * 0.42f
                ),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.22f, size.height * 0.85f)
            )
        }

        // ── Scrim ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x180F1923))
        )

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FINTECH BACKGROUND — Premium midnight navy → dark slate gradient
//  Static abstract shapes — NO particles, NO floating circles
// ─────────────────────────────────────────────────────────────────────────────

private val FintechDeep1 = Color(0xFF080F17)
private val FintechDeep2 = Color(0xFF0F1923)
private val FintechDeep3 = Color(0xFF1A2535)

/**
 * Premium dark fintech background:
 * - Deep navy diagonal gradient base
 * - Soft abstract arc shapes in indigo and violet
 * - Subtle diagonal line grid for fintech data texture
 */
@Composable
fun FintechBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to FintechDeep1,
                        0.40f to FintechDeep2,
                        0.70f to FintechDeep3,
                        1.00f to Color(0xFF0F1923)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-right arc — emerald glow
            drawArc(
                brush      = Brush.radialGradient(
                    colors = listOf(EmeraldGlow.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * 0.90f, h * 0.08f),
                    radius = w * 0.75f
                ),
                startAngle = 110f,
                sweepAngle = 140f,
                useCenter  = false,
                topLeft    = Offset(w * 0.30f, -h * 0.15f),
                size       = androidx.compose.ui.geometry.Size(w * 1.2f, h * 0.55f),
                style      = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.38f)
            )

            // Bottom-left ellipse — lime glow
            drawOval(
                brush   = Brush.radialGradient(
                    colors = listOf(LimeGlow.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(w * 0.10f, h * 0.88f),
                    radius = w * 0.65f
                ),
                topLeft = Offset(-w * 0.30f, h * 0.62f),
                size    = androidx.compose.ui.geometry.Size(w * 0.85f, h * 0.45f)
            )

            // Centre-right gold depth circle
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(GoldGlow.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(w * 0.78f, h * 0.58f),
                    radius = w * 0.50f
                ),
                center = Offset(w * 0.78f, h * 0.58f),
                radius = w * 0.50f
            )

            // Diagonal fine-line grid — fintech data texture
            val lineAlpha = 0.030f
            val step = w * 0.065f
            var x = -h.toFloat()
            while (x < w + h) {
                drawLine(
                    color       = Color.White.copy(alpha = lineAlpha),
                    start       = Offset(x, 0f),
                    end         = Offset(x + h, h),
                    strokeWidth = 0.8f
                )
                x += step
            }
        }

        content()
    }
}
