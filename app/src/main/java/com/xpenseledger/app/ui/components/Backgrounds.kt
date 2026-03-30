package com.xpenseledger.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.xpenseledger.app.R

// ─────────────────────────────────────────────────────────────────────────────
//  Colour constants (only used inside this file — avoids polluting theme)
// ─────────────────────────────────────────────────────────────────────────────


private val DashTop      = Color(0xFF0F172A)   // neumorphic base
private val DashMid      = Color(0xFF111D35)   // navy shift
private val DashBottom   = Color(0xFF0D1F2A)   // subtle teal tint
private val DashGlow1    = Color(0xFF22D3EE)   // cyan
private val DashGlow2    = Color(0xFF6366F1)   // indigo

private val SoftTop      = Color(0xFF0F172A)
private val SoftBottom   = Color(0xFF0D1E1F)
private val SoftGlow     = Color(0xFF22D3EE)

private val AnalyticsTop     = Color(0xFF0C0F1A)
private val AnalyticsMid     = Color(0xFF0F1A2A)
private val AnalyticsBottom  = Color(0xFF0A1520)
private val AnalyticsGlow    = Color(0xFF6366F1)


// ─────────────────────────────────────────────────────────────────────────────
//  2. DASHBOARD BACKGROUND  ── Home / Expenses screen
//       Multi-stop dark gradient  +  two large colour-glow orbs (teal & indigo)
//       +  subtle dot-grid overlay  +  mild scrim
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen background for the Home / Expenses tab.
 *
 * Creates a deep financial-dashboard feel with ambient brand-colour glows
 * and a barely-visible dot-grid texture.
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
                        0.0f to DashTop,
                        0.45f to DashMid,
                        1.0f to DashBottom
                    )
                )
            )
    ) {
        // ── Large brand-colour orbs ────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Top teal orb
            val topR = size.width * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1800D4A0), Color.Transparent),
                    center = Offset(size.width * 0.80f, size.height * 0.08f),
                    radius = topR * 1.4f
                ),
                radius = topR,
                center = Offset(size.width * 0.80f, size.height * 0.08f)
            )
            // Mid indigo orb
            val midR = size.width * 0.60f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x127B68EE), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.50f),
                    radius = midR * 1.4f
                ),
                radius = midR,
                center = Offset(size.width * 0.15f, size.height * 0.50f)
            )
            // Bottom teal orb
            val botR = size.width * 0.50f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1500D4A0), Color.Transparent),
                    center = Offset(size.width * 0.70f, size.height * 0.92f),
                    radius = botR * 1.4f
                ),
                radius = botR,
                center = Offset(size.width * 0.70f, size.height * 0.92f)
            )
        }

        // ── Dot-grid texture ───────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().alpha(0.50f)) {
            val t = 48.dp
            // Fill a 8-col × 20-row grid
            for (row in 0..19) {
                for (col in 0..7) {
                    Image(
                        painter = painterResource(R.drawable.bg_dot_grid),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = (col * 48).dp, y = (row * 48).dp)
                            .size(t),
                        contentScale = ContentScale.FillBounds
                    )
                }
            }
        }

        // ── Subtle overlay scrim ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x280F1117))
        )

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  3. SOFT GRADIENT BACKGROUND  ── Add Expense dialog / sheet
//       Minimal two-stop gradient with a single centred teal glow, no texture.
//       Designed to feel calm and focused so the form fields stay prominent.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Gentle background intended for form/dialog surfaces (Add Expense).
 * Uses a diagonal gradient + a single soft glow so the screen is distinct
 * from the Home screen without being distracting.
 *
 * Attach to the dialog's inner [Column] or a wrapping [Box] rather than
 * a full-screen surface.
 */
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
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Single subtle teal glow at top-centre
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowRadius = size.width * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1400D4A0), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.05f),
                    radius = glowRadius * 1.4f        // gradient fades before edge
                ),
                radius = glowRadius,
                center = Offset(size.width * 0.50f, size.height * 0.05f)
            )
            // Bottom-right indigo accent — centre well inside, radius kept small
            val accentRadius = size.width * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x0C7B68EE), Color.Transparent),
                    center = Offset(size.width * 0.72f, size.height * 0.78f),
                    radius = accentRadius * 1.4f      // gradient fades before edge
                ),
                radius = accentRadius,
                center = Offset(size.width * 0.72f, size.height * 0.78f)
            )
        }

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  4. ANALYTICS BACKGROUND  ── Compare / month-comparison tab
//       Dark indigo-tinted gradient  +  bar-chart vector decoration in the
//       upper portion  +  radial glow  +  horizontal rule lines mimicking
//       chart grid lines
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Analytics-inspired background for the Compare tab.
 *
 * Layers (bottom → top):
 * 1. Deep indigo-tinted gradient (dark navy → very dark teal)
 * 2. Horizontal faint "chart grid" lines drawn via [Canvas]
 * 3. Bar-chart vector image decorating the top-right corner
 * 4. Radial glow from the bar-chart area
 * 5. Scrim to keep content readable
 */
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
                        0.0f to AnalyticsTop,
                        0.55f to AnalyticsMid,
                        1.0f to AnalyticsBottom
                    )
                )
            )
    ) {
        // ── Faint horizontal chart-grid lines ──────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color(0x0F00D4A0)
            val lineCount = 8
            val spacing = size.height / (lineCount + 1)
            repeat(lineCount) { i ->
                val y = spacing * (i + 1)
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.2f
                )
            }
            // Vertical baseline faint line
            drawLine(
                color = Color(0x0A7B68EE),
                start = Offset(size.width * 0.08f, 0f),
                end = Offset(size.width * 0.08f, size.height),
                strokeWidth = 1f
            )
        }

        // ── Bar-chart decorative vector (top-right corner) ─────────────────
        Image(
            painter = painterResource(R.drawable.bg_bar_chart),
            contentDescription = null,
            modifier = Modifier
                .size(width = 200.dp, height = 125.dp)
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-8).dp)
                .alpha(0.45f),
            contentScale = ContentScale.FillBounds
        )

        // ── Indigo glow behind the chart area ──────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x157B68EE), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.15f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.75f, size.height * 0.15f)
            )
            // Bottom teal glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1100D4A0), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.88f),
                    radius = size.width * 0.45f
                ),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.25f, size.height * 0.88f)
            )
        }

        // ── Scrim ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x200C0F1A))
        )

        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FINTECH VAULT BACKGROUND  ── Premium deep blue → purple gradient
//  Static abstract shapes — NO particles, NO floating circles
// ─────────────────────────────────────────────────────────────────────────────

private val FintechDeep1   = Color(0xFF050D1A)
private val FintechDeep2   = Color(0xFF0A0E2A)
private val FintechDeep3   = Color(0xFF100820)
private val FintechGlowA   = Color(0xFF6366F1)   // indigo
private val FintechGlowB   = Color(0xFF22D3EE)   // cyan
private val FintechGlowC   = Color(0xFF4547C4)   // deep indigo

/**
 * Premium fintech background:
 * - Deep blue-to-purple diagonal gradient base
 * - Two large soft abstract arc shapes (Canvas, no images)
 * - Subtle noise-like diagonal line grid for depth
 * - NO particles, NO floating circles, NO distracting animations
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
                        0.35f to FintechDeep2,
                        0.65f to FintechDeep3,
                        1.00f to Color(0xFF080520)
                    ),
                    start = Offset(0f, 0f),
                    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // ── Abstract arc shapes on Canvas ─────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Shape 1: Large arc / ring segment top-right — indigo glow
            drawArc(
                brush      = Brush.radialGradient(
                    colors  = listOf(FintechGlowA.copy(alpha = 0.18f), Color.Transparent),
                    center  = Offset(w * 0.90f, h * 0.08f),
                    radius  = w * 0.75f
                ),
                startAngle = 110f,
                sweepAngle = 140f,
                useCenter  = false,
                topLeft    = Offset(w * 0.30f, -h * 0.15f),
                size       = androidx.compose.ui.geometry.Size(w * 1.2f, h * 0.55f),
                style      = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = w * 0.38f
                )
            )

            // Shape 2: Large soft ellipse blob bottom-left — teal
            drawOval(
                brush   = Brush.radialGradient(
                    colors  = listOf(FintechGlowB.copy(alpha = 0.10f), Color.Transparent),
                    center  = Offset(w * 0.10f, h * 0.88f),
                    radius  = w * 0.65f
                ),
                topLeft = Offset(-w * 0.30f, h * 0.62f),
                size    = androidx.compose.ui.geometry.Size(w * 0.85f, h * 0.45f)
            )

            // Shape 3: Subtle deep-purple fill centre-right for depth
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(FintechGlowC.copy(alpha = 0.13f), Color.Transparent),
                    center = Offset(w * 0.78f, h * 0.58f),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.78f, h * 0.58f),
                radius = w * 0.55f
            )

            // Diagonal fine-line grid (very subtle — adds fintech "data" texture)
            val lineAlpha = 0.035f
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
