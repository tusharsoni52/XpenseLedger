package com.xpenseledger.app.ui.screens.comparison

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpenseledger.app.ui.components.AnalyticsBackground
import com.xpenseledger.app.ui.components.categoryBadgeColor
import com.xpenseledger.app.ui.theme.ColorExpense
import com.xpenseledger.app.ui.theme.ColorIncome
import com.xpenseledger.app.ui.theme.XpenseAccent
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary
import com.xpenseledger.app.ui.viewmodel.AllTimeStats
import com.xpenseledger.app.ui.viewmodel.DayOfWeekStats
import com.xpenseledger.app.ui.viewmodel.ExpenseViewModel
import com.xpenseledger.app.ui.viewmodel.MonthData
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

private val FMT_DISPLAY = SimpleDateFormat("MMM yy", Locale.getDefault())
private val FMT_KEY     = SimpleDateFormat("yyyy-MM", Locale.US)
private fun monthLabel(key: String) = try { FMT_DISPLAY.format(FMT_KEY.parse(key)!!) } catch (e: Exception) { key }

// ─────────────────────────────────────────────────────────────────────────────
//  Entry point
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(vm: ExpenseViewModel = hiltViewModel()) {
    val monthData       by vm.monthComparison.collectAsState()
    val categorySummary by vm.categorySummary.collectAsState()
    val allTimeStats    by vm.allTimeStats.collectAsState()
    val dowStats        by vm.dayOfWeekStats.collectAsState()
    val selectedMonth   by vm.selectedMonth.collectAsState()

    if (monthData.isEmpty()) {
        AnalyticsBackground {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(32.dp)
                ) {
                    Icon(Icons.Default.BarChart, null,
                        tint = XpensePrimary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No Data Yet", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Text("Add transactions to unlock analytics and insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            }
        }
        return
    }

    val selectedTabIndex = remember { mutableIntStateOf(0) }
    val tabs = listOf("Monthly", "Categories", "Summary", "Insights")

    AnalyticsBackground {
        Column(Modifier.fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex.intValue,
                modifier         = Modifier.fillMaxWidth().background(Color(0xFF0F1923)),
                containerColor   = Color(0xFF0F1923),
                contentColor     = XpensePrimary,
                edgePadding      = 0.dp,
                indicator        = { tabPositions ->
                    if (selectedTabIndex.intValue < tabPositions.size) {
                        val tab = tabPositions[selectedTabIndex.intValue]
                        Box(
                            Modifier
                                .fillMaxSize()
                                .wrapContentSize(Alignment.BottomStart)
                                .offset(x = tab.left)
                                .width(tab.width)
                                .height(2.dp)
                                .background(Brush.horizontalGradient(listOf(XpensePrimary, XpenseSecondary)))
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTabIndex.intValue == idx,
                        onClick  = { selectedTabIndex.intValue = idx },
                        text = {
                            Text(label,
                                fontWeight = if (selectedTabIndex.intValue == idx) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp)
                        }
                    )
                }
            }

            when (selectedTabIndex.intValue) {
                0 -> MonthlyTab(monthData)
                1 -> CategoriesTab(monthData, categorySummary, selectedMonth, vm)
                2 -> SummaryTab(monthData)
                3 -> InsightsTab(allTimeStats, dowStats, monthData)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab 0 — Monthly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonthlyTab(monthData: List<MonthData>) {
    val maxVal = monthData.maxOf { maxOf(it.total, it.totalIncome) }.coerceAtLeast(1.0)

    LazyColumn(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionLabel("Income vs Expenses by Month")
            Spacer(Modifier.height(10.dp))
            GroupedBarChart(monthData)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(ColorIncome, "Income")
                LegendDot(ColorExpense, "Expenses")
                LegendDot(XpensePrimary, "Savings dot")
            }
            Spacer(Modifier.height(16.dp))

            val latest     = monthData.lastOrNull()
            val previous   = monthData.dropLast(1).lastOrNull()
            val avgExpense = monthData.map { it.total }.average()
            val avgIncome  = monthData.map { it.totalIncome }.average()
            val avgSavings = monthData.map { it.netSavings }.average()
            val bestSavings = monthData.maxByOrNull { it.netSavings }

            if (latest != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("Avg Expense/mo", "₹${avgExpense.toInt()}", ColorExpense,  Modifier.weight(1f))
                    MetricTile("Avg Income/mo",  "₹${avgIncome.toInt()}",  ColorIncome,   Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("Avg Net Savings", "₹${avgSavings.toInt()}",
                        if (avgSavings >= 0) ColorIncome else ColorExpense, Modifier.weight(1f))
                    if (previous != null) {
                        val momPct = if (previous.total > 0) ((latest.total - previous.total) / previous.total * 100) else 0.0
                        MetricTile("MoM Expense",
                            "${if (momPct >= 0) "+" else ""}${String.format("%.1f", momPct)}%",
                            if (momPct <= 0) ColorIncome else ColorExpense, Modifier.weight(1f))
                    } else Spacer(Modifier.weight(1f))
                }
                if (bestSavings != null && bestSavings.netSavings > 0) {
                    Spacer(Modifier.height(8.dp))
                    InsightBanner("🏆 Best savings: ${monthLabel(bestSavings.month)} — ₹${bestSavings.netSavings.toInt()}")
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel("Month Detail")
        }

        items(monthData.size) { idx ->
            val data = monthData[monthData.size - 1 - idx]
            val prev = if (idx < monthData.size - 1) monthData[monthData.size - 2 - idx] else null
            MonthDetailCard(data, prev, maxVal)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun GroupedBarChart(monthData: List<MonthData>) {
    if (monthData.isEmpty()) return
    val maxVal  = monthData.maxOf { maxOf(it.total, it.totalIncome) }.coerceAtLeast(1.0)
    val months  = monthData.takeLast(6)
    val barH    = 160.dp

    Row(
        modifier              = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment     = Alignment.Bottom
    ) {
        months.forEach { md ->
            val expAnim by animateFloatAsState((md.total / maxVal).toFloat().coerceIn(0f,1f),
                tween(700, easing = FastOutSlowInEasing), label = "exp${md.month}")
            val incAnim by animateFloatAsState((md.totalIncome / maxVal).toFloat().coerceIn(0f,1f),
                tween(700, easing = FastOutSlowInEasing), label = "inc${md.month}")

            Column(
                modifier            = Modifier.width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (md.netSavings > 0) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(XpensePrimary))
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    modifier              = Modifier.height(barH),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment     = Alignment.Bottom
                ) {
                    Box(Modifier.width(16.dp).fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.width(16.dp).height(barH * incAnim)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(Brush.verticalGradient(listOf(ColorIncome, ColorIncome.copy(0.55f)))))
                    }
                    Box(Modifier.width(16.dp).fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.width(16.dp).height(barH * expAnim)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(Brush.verticalGradient(listOf(ColorExpense, ColorExpense.copy(0.55f)))))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(monthLabel(md.month), style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MonthDetailCard(data: MonthData, prevData: MonthData?, maxVal: Double) {
    val spendFrac = (data.total / maxVal).toFloat().coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(spendFrac, tween(600, easing = FastOutSlowInEasing), label = "mdc${data.month}")

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(monthLabel(data.month), fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹${String.format("%.0f", data.total)}", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium, color = ColorExpense)
                    if (data.totalIncome > 0)
                        Text("Income: ₹${String.format("%.0f", data.totalIncome)}",
                            style = MaterialTheme.typography.labelSmall, color = ColorIncome)
                    if (prevData != null && prevData.total > 0) {
                        val pct = (data.total - prevData.total) / prevData.total * 100
                        Text("${if (pct >= 0) "▲" else "▼"} ${String.format("%.1f", abs(pct))}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pct <= 0) ColorIncome else ColorExpense)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outline.copy(0.15f))) {
                Box(Modifier.fillMaxWidth(animFrac).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(ColorExpense, ColorExpense.copy(0.6f)))))
            }

            Spacer(Modifier.height(10.dp))
            data.byCategory.entries.sortedByDescending { it.value }.take(3).forEach { (cat, amt) ->
                val catFrac = if (data.total > 0) (amt / data.total).toFloat() else 0f
                val catAnim by animateFloatAsState(catFrac, tween(500), label = "cat$cat")
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(categoryBadgeColor(cat)))
                    Text(cat, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.width(56.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(0.15f))) {
                        Box(Modifier.fillMaxWidth(catAnim).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(categoryBadgeColor(cat).copy(0.7f)))
                    }
                    Text("₹${String.format("%.0f", amt)}", style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (data.byCategory.size > 3) {
                Spacer(Modifier.height(4.dp))
                Text("+ ${data.byCategory.size - 3} more", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (data.totalIncome > 0) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f), thickness = 0.5.dp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Savings", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${if (data.netSavings >= 0) "+" else ""}₹${String.format("%.0f", data.netSavings)}",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        color = if (data.netSavings >= 0) ColorIncome else ColorExpense)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab 1 — Categories
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoriesTab(
    monthData: List<MonthData>, categorySummary: Map<String, Double>,
    selectedMonth: String?, vm: ExpenseViewModel
) {
    val allTimeByCategory = monthData.flatMap { it.byCategory.entries }
        .groupingBy { it.key }.fold(0.0) { acc, e -> acc + e.value }
    val isAllTime   = categorySummary.isEmpty()
    val displayData = if (!isAllTime) categorySummary else allTimeByCategory
    val totalDisplay = displayData.values.sum().coerceAtLeast(0.01)
    val sortedCats  = displayData.entries.sortedByDescending { it.value }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionLabel("Spending by Category")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = isAllTime, onClick = { vm.selectMonth(null) },
                    label = { Text("All Time", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XpensePrimary.copy(0.18f), selectedLabelColor = XpensePrimary))
                monthData.reversed().forEach { md ->
                    FilterChip(selected = selectedMonth == md.month, onClick = { vm.selectMonth(md.month) },
                        label = { Text(monthLabel(md.month), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = XpensePrimary.copy(0.18f), selectedLabelColor = XpensePrimary))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Total: ₹${String.format("%.0f", totalDisplay)}  •  ${if (isAllTime) "All Time" else monthLabel(selectedMonth ?: "")}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }

        items(sortedCats.size) { idx ->
            val (cat, amt) = sortedCats[idx]
            val frac    = (amt / totalDisplay).toFloat().coerceIn(0f,1f)
            val animFrac by animateFloatAsState(frac, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "c$idx")
            val badgeColor = categoryBadgeColor(cat)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(badgeColor.copy(0.18f)),
                                contentAlignment = Alignment.Center) {
                                Text("${idx+1}", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                            Text(cat, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%.0f", amt)}", style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${String.format("%.1f", frac*100)}%", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(0.15f))) {
                        Box(Modifier.fillMaxWidth(animFrac).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(Brush.horizontalGradient(listOf(badgeColor, badgeColor.copy(0.55f)))))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab 2 — Summary Table
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryTab(monthData: List<MonthData>) {
    val allCategories = monthData.flatMap { it.byCategory.keys }.distinct().sorted()
    val months = monthData.reversed().take(6).reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionLabel("Cross-Month Summary")
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Box(Modifier.horizontalScroll(rememberScrollState())) {
                    Column(Modifier.padding(12.dp)) {
                        // Header
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            TableCell("Category", 120.dp, header = true)
                            months.forEach { TableCell(monthLabel(it.month), 72.dp, header = true) }
                            TableCell("Total", 76.dp, header = true, highlight = true)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.3f))

                        // Data rows
                        allCategories.forEach { cat ->
                            val rowTotal = months.sumOf { it.byCategory[cat] ?: 0.0 }
                            Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                TableCell(cat, 120.dp)
                                months.forEach { md ->
                                    val amt = md.byCategory[cat] ?: 0.0
                                    TableCell(if (amt > 0) "₹${String.format("%.0f", amt)}" else "—", 72.dp, dimmed = amt == 0.0)
                                }
                                TableCell("₹${String.format("%.0f", rowTotal)}", 76.dp, highlight = true)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 6.dp), 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.3f))

                        // Expenses row
                        Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            TableCell("Expenses", 120.dp, header = true)
                            months.forEach { TableCell("₹${String.format("%.0f", it.total)}", 72.dp, header = true, highlight = true) }
                            TableCell("₹${String.format("%.0f", months.sumOf { it.total })}", 76.dp, header = true, highlight = true)
                        }
                        // Income row
                        Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            TableCell("Income", 120.dp)
                            months.forEach {
                                TableCell(if (it.totalIncome > 0) "₹${String.format("%.0f", it.totalIncome)}" else "—",
                                    72.dp, dimmed = it.totalIncome == 0.0, incomeCol = true)
                            }
                            TableCell("₹${String.format("%.0f", months.sumOf { it.totalIncome })}", 76.dp, incomeCol = true)
                        }
                        // Savings row
                        Row(Modifier.padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            TableCell("Savings", 120.dp)
                            months.forEach {
                                TableCell(
                                    "${if (it.netSavings >= 0) "+" else ""}₹${String.format("%.0f", it.netSavings)}",
                                    72.dp, savingsCol = true, negative = it.netSavings < 0)
                            }
                            val gs = months.sumOf { it.netSavings }
                            TableCell("${if (gs >= 0) "+" else ""}₹${String.format("%.0f", gs)}", 76.dp,
                                savingsCol = true, negative = gs < 0)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TableCell(
    text: String, width: androidx.compose.ui.unit.Dp,
    header: Boolean = false, highlight: Boolean = false,
    dimmed: Boolean = false, incomeCol: Boolean = false,
    savingsCol: Boolean = false, negative: Boolean = false
) {
    val color = when {
        incomeCol  -> ColorIncome
        savingsCol -> if (negative) ColorExpense else ColorIncome
        highlight  -> MaterialTheme.colorScheme.primary
        dimmed     -> MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
        else       -> MaterialTheme.colorScheme.onSurface
    }
    Text(text, modifier = Modifier.width(width).padding(horizontal = 4.dp, vertical = 2.dp),
        fontSize = 11.sp, fontWeight = if (header || highlight) FontWeight.Bold else FontWeight.Normal,
        color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
}

// ─────────────────────────────────────────────────────────────────────────────
//  Tab 3 — Insights
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InsightsTab(stats: AllTimeStats, dowStats: DayOfWeekStats, monthData: List<MonthData>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionLabel("All-Time Overview")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Total Spent",  "₹${stats.totalSpent.toInt()}",  ColorExpense, Modifier.weight(1f))
                MetricTile("Total Income", "₹${stats.totalIncome.toInt()}", ColorIncome,  Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("Net Savings", "₹${stats.netSavings.toInt()}",
                    if (stats.netSavings >= 0) ColorIncome else ColorExpense, Modifier.weight(1f))
                MetricTile("Transactions", "${stats.transactionCount}", XpensePrimary, Modifier.weight(1f))
            }
        }

        item {
            SectionLabel("Savings Rate")
            Spacer(Modifier.height(8.dp))
            SavingsRateGauge(stats.savingsRate)
        }

        item {
            SectionLabel("Expense Facts")
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FactRow("Avg transaction size", "₹${String.format("%.0f", stats.avgTransaction)}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f), thickness = 0.5.dp)
                    FactRow("Largest expense", "₹${String.format("%.0f", stats.biggestExpense)}", stats.biggestExpenseTitle)
                    if (monthData.isNotEmpty()) {
                        val avgMo = monthData.map { it.total }.average()
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f), thickness = 0.5.dp)
                        FactRow("Monthly avg spend", "₹${String.format("%.0f", avgMo)}")
                        val lowest = monthData.minByOrNull { it.total }
                        if (lowest != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.15f), thickness = 0.5.dp)
                            FactRow("Lowest spend month", "₹${String.format("%.0f", lowest.total)}", monthLabel(lowest.month))
                        }
                    }
                }
            }
        }

        item {
            SectionLabel("Spending by Day of Week")
            Spacer(Modifier.height(8.dp))
            DayOfWeekHeatmap(dowStats)
        }

        if (monthData.size >= 2) {
            item {
                SectionLabel("Spending Trend")
                Spacer(Modifier.height(8.dp))
                SpendingTrendCard(monthData)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Savings rate gauge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavingsRateGauge(rate: Float) {
    val animRate by animateFloatAsState(rate.coerceIn(0f,1f), tween(900, easing = FastOutSlowInEasing), label = "sr")
    val rateColor = when {
        rate >= 0.30f -> ColorIncome
        rate >= 0.10f -> XpenseAccent
        else          -> ColorExpense
    }
    val rateLabel = when {
        rate >= 0.30f -> "Excellent 🎉"
        rate >= 0.20f -> "Good"
        rate >= 0.10f -> "Fair"
        rate >  0.00f -> "Low"
        else          -> "Negative"
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("${String.format("%.1f", animRate * 100)}%",
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = rateColor)
                    Text(rateLabel, style = MaterialTheme.typography.labelMedium, color = rateColor.copy(0.75f))
                }
                Icon(if (rate >= 0.10f) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    null, tint = rateColor, modifier = Modifier.size(36.dp))
            }
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.outline.copy(0.15f))) {
                Box(Modifier.fillMaxWidth(animRate).height(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(rateColor.copy(0.7f), rateColor))))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0%", "10%", "20%", "30%+").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                }
            }
            Text("Savings Rate = (Income − Expenses − Transfers) ÷ Income",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f), fontSize = 10.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Day-of-week heatmap
// ─────────────────────────────────────────────────────────────────────────────

private val kDayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
private fun DayOfWeekHeatmap(stats: DayOfWeekStats) {
    val maxT = stats.maxTotal.coerceAtLeast(1.0)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                stats.totals.forEachIndexed { idx, total ->
                    val frac = (total / maxT).toFloat()
                    val animFrac by animateFloatAsState(frac, tween(600 + idx * 60), label = "dow$idx")
                    val col by animateColorAsState(XpensePrimary.copy(0.15f + animFrac * 0.65f), tween(600), label = "dowC$idx")
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(kDayLabels[idx], style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(col),
                            contentAlignment = Alignment.Center) {
                            if (frac > 0)
                                Text("${String.format("%.0f", frac*100)}%", fontSize = 9.sp,
                                    color = if (frac > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium)
                        }
                        if (total > 0)
                            Text("₹${total.toInt()}", fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("Relative spending per day (darker = more)", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f), fontSize = 10.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Spending trend card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpendingTrendCard(monthData: List<MonthData>) {
    val recent    = monthData.takeLast(3)
    val older     = monthData.dropLast(3)
    val recentAvg = if (recent.isNotEmpty()) recent.map { it.total }.average() else 0.0
    val olderAvg  = if (older.isNotEmpty())  older.map { it.total }.average()  else recentAvg
    val trend     = if (olderAvg > 0) ((recentAvg - olderAvg) / olderAvg * 100) else 0.0
    val isDown    = trend <= 0

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = (if (isDown) ColorIncome else ColorExpense).copy(0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (isDown) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                null, tint = if (isDown) ColorIncome else ColorExpense, modifier = Modifier.size(32.dp))
            Column {
                Text(if (isDown) "Spending down ${String.format("%.1f", abs(trend))}% vs earlier months 🎉"
                     else "Spending up ${String.format("%.1f", abs(trend))}% vs earlier months",
                    style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Recent 3-mo avg: ₹${recentAvg.toInt()}  •  Prior avg: ₹${olderAvg.toInt()}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared micro-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun MetricTile(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(accentColor.copy(0.10f))
        .padding(horizontal = 12.dp, vertical = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FactRow(label: String, value: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (subtitle != null)
                Text(subtitle, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(value, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightBanner(text: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(XpensePrimary.copy(0.10f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
