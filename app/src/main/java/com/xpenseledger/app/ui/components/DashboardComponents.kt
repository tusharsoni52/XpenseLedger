package com.xpenseledger.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xpenseledger.app.domain.model.Expense
import com.xpenseledger.app.ui.security.maskedAmount
import com.xpenseledger.app.ui.security.rememberDebouncedClick
import com.xpenseledger.app.ui.theme.XpensePrimary
import com.xpenseledger.app.ui.theme.XpenseSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

// ── Category badge colour palette (deterministic, 10 colours) ─────────────────
private val kCategoryColors = listOf(
    Color(0xFF22D3EE), // cyan
    Color(0xFF6366F1), // indigo
    Color(0xFF34D399), // emerald
    Color(0xFFF59E0B), // amber
    Color(0xFFF87171), // red
    Color(0xFF818CF8), // violet
    Color(0xFF38BDF8), // sky
    Color(0xFFA78BFA), // purple
    Color(0xFF4ADE80), // green
    Color(0xFFFB923C)  // orange
)

/** Returns a stable colour for [name] via hash, same name → same colour forever. */
fun categoryBadgeColor(name: String): Color =
    kCategoryColors[(name.hashCode() and 0x7FFF_FFFF) % kCategoryColors.size]

// ─────────────────────────────────────────────────────────────────────────────
//  1. TOTAL EXPENSE CARD  ── gradient hero card at the top of the dashboard
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-width gradient card showing [total] with an animated counting-up
 * number and a stat pill showing how many [count] expenses are included.
 *
 * The amount counter runs from 0 → [total] over 900 ms whenever [total] changes.
 * Two semi-transparent decorative rings are drawn over the gradient to give
 * visual depth without requiring any image assets.
 */
@Composable
fun TotalExpenseCard(
    total:    Double,
    count:    Int,
    period:   String,
    masked:   Boolean = false,
    modifier: Modifier = Modifier
) {
    val animatedTotal = remember { Animatable(0f) }
    LaunchedEffect(total) {
        animatedTotal.animateTo(
            targetValue   = total.toFloat(),
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicShadow(elevation = 10.dp, cornerRadius = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(XpensePrimary.copy(alpha = 0.85f), XpenseSecondary),
                        start  = Offset(0f, 0f),
                        end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            // ── Decorative background rings ─────────────────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
            )
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )

            // ── Text content ────────────────────────────────────────────────
            Column {
                Text(
                    text          = period.uppercase(Locale.getDefault()),
                    style         = MaterialTheme.typography.labelSmall,
                    color         = Color.White.copy(alpha = 0.75f),
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = "Total Expenses",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text       = if (masked) maskedAmount(total, true)
                                 else "₹${"%.2f".format(animatedTotal.value)}",
                    style      = MaterialTheme.typography.displaySmall,
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(10.dp))
                // Stat pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text  = "$count transaction${if (count != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  2. FINANCIAL SUMMARY CARD  ── Income / Expenses / Transfers / Balance
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Four-metric summary card showing Income (green), Expenses (red),
 * Transfers (orange) and the computed Balance (highlighted).
 *
 * Balance = Income − Expenses − Transfers
 * Balance is green when positive, red when negative, grey when zero.
 */
@Composable
fun FinancialSummaryCard(
    totalIncome:    Double,
    totalExpenses:  Double,
    totalTransfers: Double,
    balance:        Double,
    period:         String,
    masked:         Boolean  = false,
    modifier:       Modifier = Modifier
) {
    val incomeAnim    = remember { Animatable(0f) }
    val expenseAnim   = remember { Animatable(0f) }
    val transferAnim  = remember { Animatable(0f) }
    val balanceAnim   = remember { Animatable(0f) }

    LaunchedEffect(totalIncome)    { incomeAnim.animateTo(totalIncome.toFloat(),    tween(800, easing = FastOutSlowInEasing)) }
    LaunchedEffect(totalExpenses)  { expenseAnim.animateTo(totalExpenses.toFloat(), tween(800, easing = FastOutSlowInEasing)) }
    LaunchedEffect(totalTransfers) { transferAnim.animateTo(totalTransfers.toFloat(), tween(800, easing = FastOutSlowInEasing)) }
    LaunchedEffect(balance)        { balanceAnim.animateTo(balance.toFloat(),       tween(900, easing = FastOutSlowInEasing)) }

    val balanceColor = when {
        balance > 0 -> Color(0xFF34D399)   // emerald green
        balance < 0 -> Color(0xFFF87171)   // soft red
        else        -> Color(0xFF94A3B8)   // slate
    }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicShadow(elevation = 10.dp, cornerRadius = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                        start  = Offset(0f, 0f),
                        end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // ── Header ─────────────────────────────────────────────────────
                Text(
                    text          = period.uppercase(Locale.getDefault()),
                    style         = MaterialTheme.typography.labelSmall,
                    color         = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.2.sp
                )

                // ── Balance (hero figure) ──────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text  = "Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text       = if (masked) maskedAmount(balance, true)
                                         else "${if (balance >= 0) "+" else ""}₹${"%.2f".format(balanceAnim.value)}",
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color      = balanceColor
                        )
                    }
                    // Balance formula hint
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text  = "Income − Expenses − Transfers",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // ── Three metric tiles ─────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryTile(
                        label    = "Income",
                        value    = if (masked) maskedAmount(totalIncome, true) else "₹${"%.0f".format(incomeAnim.value)}",
                        color    = Color(0xFF34D399),
                        prefix   = "+",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTile(
                        label    = "Expenses",
                        value    = if (masked) maskedAmount(totalExpenses, true) else "₹${"%.0f".format(expenseAnim.value)}",
                        color    = Color(0xFFF87171),
                        prefix   = "-",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTile(
                        label    = "Transfers",
                        value    = if (masked) maskedAmount(totalTransfers, true) else "₹${"%.0f".format(transferAnim.value)}",
                        color    = Color(0xFFFB923C),
                        prefix   = "→",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(
    label:    String,
    value:    String,
    color:    Color,
    prefix:   String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(prefix, style = MaterialTheme.typography.labelMedium, color = color,
                    fontWeight = FontWeight.Bold)
                Text(label,  style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f))
            }
            Text(
                text       = value,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color      = color,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  3. TRANSACTION ROW with type indicator  ── Income/Expense/Transfer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Single transaction row that adapts its color and prefix symbol
 * to the transaction type:
 *  • INCOME   →  "+" green
 *  • EXPENSE  →  "−" red
 *  • TRANSFER →  "→" orange
 */
@Composable
fun TransactionRow(
    expense:  Expense,
    onEdit:   (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    masked:   Boolean  = false,
    modifier: Modifier = Modifier
) {
    val (typeColor, typePrefix) = when (expense.type) {
        com.xpenseledger.app.domain.model.TransactionType.INCOME   -> Color(0xFF34D399) to "+"
        com.xpenseledger.app.domain.model.TransactionType.TRANSFER -> Color(0xFFFB923C) to "→"
        else                                                        -> Color(0xFFF87171) to "−"
    }
    val badgeColor = categoryBadgeColor(expense.category)
    val dateFmt    = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val safeEdit   = rememberDebouncedClick { onEdit(expense) }
    val safeDelete = rememberDebouncedClick { onDelete(expense) }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicShadow(elevation = 4.dp, cornerRadius = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type prefix badge
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = typePrefix,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = typeColor
                    )
                }

                // Title + meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = expense.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    val sub     = expense.subCategory
                    val dateStr = dateFmt.format(Date(expense.timestamp))
                    Text(
                        text  = buildString {
                            append(expense.category)
                            if (!sub.isNullOrBlank()) append(" › $sub")
                            append("  •  $dateStr")
                        },
                        style   = MaterialTheme.typography.labelSmall,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Amount + actions
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text       = "${typePrefix}₹${"%.2f".format(expense.amount)}",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = typeColor
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        IconButton(onClick = safeEdit,   modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit,   contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = safeDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CATEGORY BREAKDOWN CARD  ── one row per category with animated bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Expandable category row:
 *  • Collapsed (default): shows badge, name, total amount, % bar, and a chevron
 *  • Expanded: reveals every expense in that category with title, sub-category, date & amount
 *  • Tap anywhere on the header row to toggle
 *  • Chevron animates 0° → 180° on expand
 *  • Expansion animates with expandVertically + fadeIn
 *
 * @param expenses    All expenses that belong to this category for the selected month.
 * @param isExpanded  Expansion state owned by the parent (LazyColumn).
 * @param onToggle    Called when the user taps the header to flip the state.
 * @param onEdit      Propagated to each child expense row.
 * @param onDelete    Propagated to each child expense row.
 */
@Composable
fun CategoryBreakdownCard(
    category:   String,
    amount:     Double,
    fraction:   Float,
    index:      Int,
    expenses:   List<Expense>     = emptyList(),
    isExpanded: Boolean           = false,
    onToggle:   () -> Unit        = {},
    onEdit:     (Expense) -> Unit = {},
    onDelete:   (Expense) -> Unit = {},
    modifier:   Modifier          = Modifier
) {
    // ── Staggered bar animation ────────────────────────────────────────────────
    var triggered by remember(category) { mutableStateOf(false) }
    LaunchedEffect(category) {
        delay(index * 75L)
        triggered = true
    }
    val animatedFraction by animateFloatAsState(
        targetValue   = if (triggered) fraction else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "cat_bar_$category"
    )

    // ── Chevron rotation ───────────────────────────────────────────────────────
    val chevronAngle by animateFloatAsState(
        targetValue   = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label         = "chevron_$category"
    )

    val badgeColor = categoryBadgeColor(category)
    val dateFmt    = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicShadow(elevation = 6.dp, cornerRadius = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
        Column {
            // ── Header row (always visible, tappable) ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        role    = Role.Button,
                        onClick = onToggle
                    )
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: badge + name
                Row(
                    modifier              = Modifier.weight(1f),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier         = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = category.take(1).uppercase(Locale.getDefault()),
                            style      = MaterialTheme.typography.labelLarge,
                            color      = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = category,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        if (expenses.isNotEmpty()) {
                            Text(
                                text  = "${expenses.size} expense${if (expenses.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Right: amount + percentage + chevron
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text       = "₹${"%.2f".format(amount)}",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "${"%.1f".format(animatedFraction * 100)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier           = Modifier
                            .size(22.dp)
                            .rotate(chevronAngle),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Animated progress bar (always visible) ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(badgeColor, badgeColor.copy(alpha = 0.55f))
                            )
                        )
                )
            }

            // ── Expandable expense list ────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded && expenses.isNotEmpty(),
                enter   = expandVertically(spring(stiffness = Spring.StiffnessMediumLow))
                          + fadeIn(tween(200)),
                exit    = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                    expenses.forEachIndexed { i, expense ->
                        if (i > 0) {
                            HorizontalDivider(
                                modifier  = Modifier.padding(horizontal = 14.dp),
                                thickness = 0.5.dp,
                                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        }
                        ExpandedExpenseRow(
                            expense  = expense,
                            dateFmt  = dateFmt,
                            onEdit   = onEdit,
                            onDelete = onDelete
                        )
                    }
                }
            }

            // Small bottom padding so the bar isn't flush with the card edge
            Spacer(Modifier.height(10.dp))
        }
        } // Box
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Expanded expense row  (shown inside CategoryBreakdownCard when expanded)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpandedExpenseRow(
    expense:  Expense,
    dateFmt:  SimpleDateFormat,
    onEdit:   (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    val safeEdit   = rememberDebouncedClick { onEdit(expense) }
    val safeDelete = rememberDebouncedClick { onDelete(expense) }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sub-category pill or plain dot
        Box(
            modifier         = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(categoryBadgeColor(expense.category).copy(alpha = 0.6f))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = expense.title,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            val sub = expense.subCategory
            val dateStr = dateFmt.format(Date(expense.timestamp))
            Text(
                text  = if (!sub.isNullOrBlank()) "$sub  •  $dateStr" else dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text       = "₹${"%.2f".format(expense.amount)}",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            IconButton(onClick = safeEdit,   modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit,   contentDescription = "Edit",
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = safeDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  3. TRANSACTION CARD  ── single expense row with press-elevation animation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Polished transaction row showing:
 *  • A rounded-square category initial badge (colour from [categoryBadgeColor])
 *  • Expense title + "Category [› Sub]  •  Date"
 *  • Amount + compact edit / delete icon buttons
 *
 * M3 [Card] handles the press-elevation tween automatically via
 * [CardDefaults.cardElevation], keeping this composable stateless.
 */
@Composable
fun TransactionCard(
    expense:  Expense,
    onEdit:   () -> Unit,
    onDelete: () -> Unit,
    masked:   Boolean = false,
    modifier: Modifier = Modifier
) {
    val badgeColor    = categoryBadgeColor(expense.category)
    val dateFmt       = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    // Debounce both destructive actions to prevent accidental double-taps
    val safeEdit      = rememberDebouncedClick { onEdit() }
    val safeDelete    = rememberDebouncedClick { onDelete() }

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphicShadow(elevation = 5.dp, cornerRadius = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category initial badge
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = expense.category.take(1).uppercase(Locale.getDefault()),
                    style      = MaterialTheme.typography.titleMedium,
                    color      = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Title + category/date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = expense.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    if (!expense.subCategory.isNullOrBlank())
                        append("${expense.category} › ${expense.subCategory}")
                    else
                        append(expense.category)
                    append("  •  ")
                    append(dateFmt.format(Date(expense.timestamp)))
                }
                Text(
                    text     = meta,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Amount + actions
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = maskedAmount(expense.amount, masked),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    IconButton(onClick = safeEdit,   modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = safeDelete, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector        = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        } // Box
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  4. EMPTY EXPENSE STATE  ── shown when no expenses match the filters
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EmptyExpenseState(modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "💸", style = MaterialTheme.typography.displayMedium)
            Text(
                text       = "No expenses yet",
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = "Tap  +  to log your first expense",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}
