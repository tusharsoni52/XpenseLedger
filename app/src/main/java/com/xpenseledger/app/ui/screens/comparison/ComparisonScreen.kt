package com.xpenseledger.app.ui.screens.comparison

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpenseledger.app.ui.components.AnalyticsBackground
import com.xpenseledger.app.ui.components.BarChart
import com.xpenseledger.app.ui.components.DoughnutChart
import com.xpenseledger.app.ui.components.StatisticCard
import com.xpenseledger.app.ui.components.TrendIndicator
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import com.xpenseledger.app.ui.viewmodel.MonthData

// ─────────────────────────────────────────────────────────────────────────────
//  Public entry point
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full analytics & comparison screen for expenses.
 *
 * Features:
 *  • Tab-based layout: Monthly Comparison, Category Breakdown, Summary Table
 *  • Smooth animations and transitions between tabs and chart updates
 *  • Monthly bar chart with values and trend indicators
 *  • Category distribution doughnut chart with legend
 *  • Summary statistics with YoY or MoM comparison
 *  • Detailed month-by-month breakdown with subcategory details
 *
 * @param vm ExpenseViewModel providing monthComparison, categorySummary, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(vm: ExpenseViewModel = hiltViewModel()) {
    val monthData by vm.monthComparison.collectAsState()
    val categorySummary by vm.categorySummary.collectAsState()

    if (monthData.isEmpty()) {
        AnalyticsBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(64.dp)
                            .height(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No Data Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add expenses to see analytics and comparisons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val selectedTabIndex = remember { mutableIntStateOf(0) }

    AnalyticsBackground {
        Column(Modifier.fillMaxSize()) {
            // ── Tab bar ────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTabIndex.intValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1520)),
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                color = XpensePrimary.copy(alpha = 0.3f)
                            )
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex.intValue == 0,
                    onClick = { selectedTabIndex.intValue = 0 },
                    text = { Text("Monthly") }
                )
                Tab(
                    selected = selectedTabIndex.intValue == 1,
                    onClick = { selectedTabIndex.intValue = 1 },
                    text = { Text("Categories") }
                )
                Tab(
                    selected = selectedTabIndex.intValue == 2,
                    onClick = { selectedTabIndex.intValue = 2 },
                    text = { Text("Summary") }
                )
            }

            // ── Tab content (animated) ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                when (selectedTabIndex.intValue) {
                    0 -> MonthlyComparisonTab(monthData)
                    1 -> CategoriesTab(monthData, categorySummary)
                    2 -> SummaryTableTab(monthData)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab: Monthly Comparison  (bar chart + month cards)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyComparisonTab(monthData: List<MonthData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Monthly Totals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // ── Bar chart ──────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = monthData.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val chartData = monthData.map { it.month to it.total }
                BarChart(
                    data = chartData,
                    height = 320,
                    animationDuration = 900
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Key metrics ────────────────────────────────────────────────────
            val latestMonth = monthData.lastOrNull()
            val previousMonth = monthData.dropLast(1).lastOrNull()
            val avgMonthly = monthData.map { it.total }.average()
            val peakMonth = monthData.maxByOrNull { it.total }

            if (latestMonth != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatisticCard(
                        label = "Latest Month",
                        value = "₹${String.format("%.0f", latestMonth.total)}",
                        subLabel = latestMonth.month,
                        modifier = Modifier.weight(1f)
                    )
                    StatisticCard(
                        label = "Monthly Avg",
                        value = "₹${String.format("%.0f", avgMonthly)}",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (peakMonth != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatisticCard(
                            label = "Highest Month",
                            value = "₹${String.format("%.0f", peakMonth.total)}",
                            subLabel = peakMonth.month,
                            modifier = Modifier.weight(1f)
                        )
                        if (previousMonth != null) {
                            Box(modifier = Modifier.weight(1f)) {
                                StatisticCard(
                                    label = "MoM Change",
                                    value = "—",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                TrendIndicator(
                                    current = latestMonth.total,
                                    previous = previousMonth.total,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(bottom = 16.dp, end = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // ── Detailed month cards ───────────────────────────────────────────────
        items(monthData.size) { idx ->
            val data = monthData[idx]
            val prevData = if (idx > 0) monthData[idx - 1] else null
            MonthDetailCard(data, prevData)
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab: Category Breakdown  (doughnut chart)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoriesTab(monthData: List<MonthData>, categorySummary: Map<String, Double>) {
    val allTimeByCategory = monthData
        .flatMap { it.byCategory.entries }
        .groupingBy { it.key }
        .fold(0.0) { acc, entry -> acc + entry.value }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Spending by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // Show current month or all-time depending on filter
            val displayData = if (categorySummary.isNotEmpty()) {
                categorySummary
            } else {
                allTimeByCategory
            }

            DoughnutChart(
                data = displayData,
                height = 340,
                animationDuration = 950,
                centerLabel = if (categorySummary.isNotEmpty()) "This Month" else "All Time"
            )

            Spacer(Modifier.height(16.dp))

            // ── Top category highlight ─────────────────────────────────────────
            val topCategory = displayData.maxByOrNull { it.value }
            if (topCategory != null) {
                val percentage = (topCategory.value / displayData.values.sum()) * 100
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Top Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(topCategory.key, fontWeight = FontWeight.Bold)
                            Text(
                                "₹${String.format("%.0f", topCategory.value)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${String.format("%.1f", percentage)}% of total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab: Summary Table
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryTableTab(monthData: List<MonthData>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Summary Table",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // All categories across all months
            val allCategories = monthData
                .flatMap { it.byCategory.keys }
                .distinct()
                .sorted()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    // ── Header row ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Category",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1.5f)
                        )
                        monthData.forEach { md ->
                            Text(
                                md.month.substring(5),  // "2025-03" → "03"
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    // ── Data rows ──────────────────────────────────────────────
                    allCategories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                cat,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1.5f)
                            )
                            monthData.forEach { md ->
                                val amt = md.byCategory[cat] ?: 0.0
                                Text(
                                    if (amt > 0) "₹${String.format("%.0f", amt)}" else "—",
                                    fontSize = 10.sp,
                                    color = if (amt > 0) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(0.9f)
                                )
                            }
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    // ── Total row ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Total",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1.5f)
                        )
                        monthData.forEach { md ->
                            Text(
                                "₹${String.format("%.0f", md.total)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Month detail card  (for monthly comparison tab)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthDetailCard(data: MonthData, prevData: MonthData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    data.month,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${String.format("%.2f", data.total)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (prevData != null) {
                        TrendIndicator(
                            current = data.total,
                            previous = prevData.total
                        )
                    }
                }
            }

            androidx.compose.material3.LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Top 3 categories
            data.byCategory
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .forEach { (category, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            category,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "₹${String.format("%.2f", amount)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

            if (data.byCategory.size > 3) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "+ ${data.byCategory.size - 3} more categories",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
