package com.xpenseledger.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  Bar Chart  (Monthly expense comparison)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Animated bar chart for comparing monthly expenses.
 */
@Composable
fun BarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    height: Int = 280,
    animationDuration: Int = 800,
    axisColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxValue = data.maxOf { it.second }.takeIf { it > 0.0 } ?: 1.0
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "barChartAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val canvasHeight = size.height
            val canvasWidth  = size.width
            val chartPadding = 48f
            val chartWidth   = canvasWidth - (chartPadding * 2)
            val chartHeight  = canvasHeight - 60f

            val barCount = data.size
            val barWidth = (chartWidth / barCount) * 0.7f
            val barGap   = chartWidth / barCount

            // Grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val fraction = i.toFloat() / gridLines
                val yPos = canvasHeight - 50f - (chartHeight * fraction)
                drawLine(
                    color       = axisColor,
                    start       = Offset(chartPadding - 4f, yPos),
                    end         = Offset(canvasWidth - chartPadding, yPos),
                    strokeWidth = 1f
                )
            }

            // Bars
            data.forEachIndexed { index, (_, value) ->
                val normalizedValue = ((value / maxValue) * animProgress).toFloat()
                val barHeight = (chartHeight * normalizedValue)
                val xStart    = (chartPadding + (index * barGap) + ((barGap - barWidth) / 2))
                val yStart    = (canvasHeight - 50f - barHeight)

                drawRect(
                    color   = Color(0xFF00D4A0).copy(alpha = 0.8f),
                    topLeft = Offset(xStart, yStart),
                    size    = Size(barWidth, barHeight)
                )
            }

            // Axes
            drawLine(
                color       = axisColor,
                start       = Offset(chartPadding, canvasHeight - 50f),
                end         = Offset(canvasWidth - chartPadding, canvasHeight - 50f),
                strokeWidth = 2f
            )
            drawLine(
                color       = axisColor,
                start       = Offset(chartPadding, 0f),
                end         = Offset(chartPadding, canvasHeight - 50f),
                strokeWidth = 2f
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Doughnut Chart  (Category breakdown)
// ─────────────────────────────────────────────────────────────────────────────

// 10-colour palette matching categoryBadgeColor in DashboardComponents
private val kDoughnutColors = listOf(
    Color(0xFF00D4A0), Color(0xFF7B68EE), Color(0xFFFFB74D),
    Color(0xFFFF5370), Color(0xFF4CAF50), Color(0xFF2196F3),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFFFF9800),
    Color(0xFF00BCD4)
)

private fun doughnutColor(index: Int): Color =
    kDoughnutColors[index % kDoughnutColors.size]

/**
 * Animated doughnut chart for category breakdown.
 *
 * Draws real arc sectors on a [Canvas] with:
 *  • 2° gap between each sector for visual separation
 *  • Animated sweep from 0° → full on first composition
 *  • Centre label showing total amount
 *  • Colour-coded legend below the chart
 */
@Composable
fun DoughnutChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    height: Int = 260,
    animationDuration: Int = 900,
    centerLabel: String = "Total"
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(height.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val total   = data.values.sum()
    val items   = data.entries.sortedByDescending { it.value }.toList()

    // Animate sweep 0 → 360
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(data) {
        sweep.snapTo(0f)
        sweep.animateTo(
            targetValue   = 360f,
            animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
        )
    }

    val strokeWidth = 56f          // ring thickness in px (density-independent via dp below)
    val gapDeg      = 2f           // degrees of gap between slices

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Doughnut canvas ──────────────────────────────────────────────────
        Box(
            modifier          = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(24.dp),
            contentAlignment  = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val diameter    = minOf(size.width, size.height)
                val strokePx    = diameter * 0.13f          // ~13 % of diameter
                val inset       = strokePx / 2f
                val arcSize     = Size(diameter - strokePx, diameter - strokePx)
                val topLeft     = Offset(
                    (size.width  - arcSize.width)  / 2f,
                    (size.height - arcSize.height) / 2f
                )

                var startAngle  = -90f          // 12-o'clock
                val totalSweep  = sweep.value   // animated 0 → 360

                items.forEachIndexed { idx, (_, value) ->
                    val fraction   = (value / total).toFloat()
                    val rawSweep   = fraction * 360f
                    // Scale each sector proportionally by the animation progress
                    val scaledSweep = rawSweep * (totalSweep / 360f)
                    val actualSweep = (scaledSweep - gapDeg).coerceAtLeast(0f)

                    if (actualSweep > 0f) {
                        drawArc(
                            color      = doughnutColor(idx),
                            startAngle = startAngle + gapDeg / 2f,
                            sweepAngle = actualSweep,
                            useCenter  = false,
                            topLeft    = topLeft,
                            size       = arcSize,
                            style      = Stroke(width = strokePx)
                        )
                    }
                    startAngle += scaledSweep
                }

                // Background ring (always full 360°, behind the coloured arcs)
                drawArc(
                    color      = Color(0xFF2A2D42),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokePx * 0.35f)
                )
            }

            // Centre label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = centerLabel,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text       = "₹${String.format("%.0f", total)}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ── Legend ───────────────────────────────────────────────────────────
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement   = Arrangement.spacedBy(6.dp)
        ) {
            items.forEachIndexed { idx, (cat, amount) ->
                val pct = (amount / total) * 100
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Colour swatch
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(doughnutColor(idx))
                    )
                    // Category name — expands to fill available space
                    Text(
                        text     = cat,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    // Amount
                    Text(
                        text      = "₹${String.format("%.0f", amount)}",
                        style     = MaterialTheme.typography.bodySmall,
                        fontWeight= FontWeight.SemiBold,
                        color     = MaterialTheme.colorScheme.onSurface
                    )
                    // Percentage
                    Text(
                        text      = "${String.format("%.1f", pct)}%",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier  = Modifier.width(44.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Statistic Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatisticCard(
    label: String,
    value: String,
    subLabel: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            if (subLabel != null) {
                Spacer(Modifier.height(4.dp))
                Text(subLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Trend Indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TrendIndicator(
    current: Double,
    previous: Double?,
    modifier: Modifier = Modifier
) {
    if (previous == null || previous <= 0) return

    val percentChange = ((current - previous) / previous) * 100
    val isIncrease    = percentChange >= 0
    val trendColor    = if (isIncrease) Color(0xFFFF6B6B) else Color(0xFF6BCB77)
    val arrow         = if (isIncrease) "▲" else "▼"

    Text(
        "$arrow ${String.format("%.1f", kotlin.math.abs(percentChange))}% vs prev",
        style    = MaterialTheme.typography.labelSmall,
        color    = trendColor,
        modifier = modifier
    )
}


