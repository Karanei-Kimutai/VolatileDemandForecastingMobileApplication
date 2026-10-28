package com.example.volatiledemandforecasting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volatiledemandforecasting.ui.theme.HighlightDarkest
import com.example.volatiledemandforecasting.ui.theme.NeutralDarkest
import com.example.volatiledemandforecasting.ui.theme.NeutralLight
import com.example.volatiledemandforecasting.ui.theme.SurfaceWhite
import com.example.volatiledemandforecasting.ui.theme.VolatileDemandForecastingTheme

private val AppBackground = Color(0xFFF5F7FB)
private val BorderColor = Color(0xFFE7EBF2)
private val MutedBlue = Color(0xFFEAF2FF)
private val SuccessGreen = Color(0xFF16845B)
private val SuccessBackground = Color(0xFFE8F6F0)
private val WarningOrange = Color(0xFFB65D0B)
private val WarningBackground = Color(0xFFFFF3E5)

private enum class AppTab(val title: String) {
    Forecast("Forecast"),
    Inventory("Inventory"),
    Events("Events"),
    Profile("Profile")
}

private data class StockItem(
    val name: String,
    val sku: String,
    val quantity: Int,
    val coverDays: Int,
    val recommendedOrder: Int
)

private data class MarketEvent(
    val name: String,
    val date: String,
    val affectedProducts: String,
    val lift: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VolatileDemandForecastingTheme(darkTheme = false, dynamicColor = false) {
                DemandForecastApp()
            }
        }
    }
}

@Composable
private fun DemandForecastApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Forecast) }
    var period by remember { mutableStateOf("7 days") }
    var inventoryFilter by remember { mutableStateOf("All") }
    var showAddEvent by remember { mutableStateOf(false) }
    val events = remember {
        mutableStateListOf(
            MarketEvent("Jamhuri Day", "12 Dec 2026", "Cooking oil, sugar", "+25%"),
            MarketEvent("Christmas season", "25 Dec 2026", "Maize flour, rice", "+45%"),
            MarketEvent("School opening", "05 Jan 2027", "Wheat flour, sugar", "+30%")
        )
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            AppBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            BusinessHeader()
            when (selectedTab) {
                AppTab.Forecast -> ForecastScreen(
                    period = period,
                    onPeriodChange = { period = it },
                    onInventoryClick = { selectedTab = AppTab.Inventory }
                )

                AppTab.Inventory -> InventoryScreen(
                    selectedFilter = inventoryFilter,
                    onFilterSelected = { inventoryFilter = it }
                )

                AppTab.Events -> EventsScreen(
                    events = events,
                    onAddEvent = { showAddEvent = true }
                )

                AppTab.Profile -> ProfileScreen()
            }
        }
    }

    if (showAddEvent) {
        AddEventDialog(
            onDismiss = { showAddEvent = false },
            onAdd = { name ->
                events.add(MarketEvent(name, "Date to confirm", "Not set", "—"))
                showAddEvent = false
            }
        )
    }
}

@Composable
private fun BusinessHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mazuri Wholesale Depot",
                color = NeutralDarkest,
                fontSize = 17.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "NAIROBI  ·  DEMO WORKSPACE",
                color = NeutralLight,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MutedBlue),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = HighlightDarkest, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ForecastScreen(
    period: String,
    onPeriodChange: (String) -> Unit,
    onInventoryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Demand forecast", color = NeutralDarkest, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("A clear view of what to stock next.", color = NeutralLight, fontSize = 13.sp)
            }
            PeriodSelector(
                period = period,
                onClick = { onPeriodChange(if (period == "7 days") "30 days" else "7 days") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "FORECAST UNITS",
                value = "1,248",
                detail = "+8.6% vs last week",
                positive = true
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                label = "MODEL ACCURACY",
                value = "91%",
                detail = "Last 30 days",
                positive = null
            )
        }

        DemandChartCard(period = period)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Needs attention",
                color = NeutralDarkest,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text("2 signals", color = NeutralLight, fontSize = 12.sp)
        }

        AttentionCard(
            title = "Sugar demand is rising",
            body = "Demand is tracking 24% above your usual weekly pattern.",
            footnote = "58 units on hand  ·  8 days of cover",
            action = "Review stock",
            onAction = onInventoryClick
        )
        AttentionCard(
            title = "Cooking oil may run low",
            body = "Current stock could fall below your buffer in about 5 days.",
            footnote = "34 units on hand  ·  5 days of cover",
            action = "Review stock",
            onAction = onInventoryClick,
            warning = true
        )

        Text(
            text = "Illustrative demo data · Updated today at 08:30",
            color = NeutralLight,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PeriodSelector(period: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(period, color = NeutralDarkest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("⌄", color = NeutralLight, fontSize = 15.sp)
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    label: String,
    value: String,
    detail: String,
    positive: Boolean?
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(label, color = NeutralLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Text(value, color = NeutralDarkest, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                detail,
                color = when (positive) {
                    true -> SuccessGreen
                    false -> WarningOrange
                    null -> NeutralLight
                },
                fontSize = 10.sp,
                lineHeight = 14.sp,
                fontWeight = if (positive == null) FontWeight.Normal else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DemandChartCard(period: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Expected demand", color = NeutralDarkest, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(if (period == "7 days") "Units across all products" else "Daily average · 30 day view", color = NeutralLight, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("↑ 8.6%", color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("vs previous period", color = NeutralLight, fontSize = 9.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(HighlightDarkest, "Forecast")
                Spacer(Modifier.width(18.dp))
                LegendDot(Color(0xFFB9C5D7), "Previous week")
            }

            Spacer(Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                val left = 4.dp.toPx()
                val right = size.width - 4.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - 22.dp.toPx()
                val chartHeight = bottom - top
                val chartWidth = right - left
                val gridColor = Color(0xFFF0F2F6)

                for (line in 0..3) {
                    val y = top + chartHeight * line / 3f
                    drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
                }

                val previous = listOf(0.46f, 0.58f, 0.51f, 0.67f, 0.62f, 0.74f, 0.69f)
                val forecast = listOf(0.50f, 0.60f, 0.56f, 0.72f, 0.70f, 0.82f, 0.91f)
                fun points(values: List<Float>) = values.mapIndexed { index, value ->
                    Offset(left + chartWidth * index / (values.lastIndex), bottom - chartHeight * value)
                }

                val previousPath = Path().apply {
                    points(previous).forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    previousPath,
                    color = Color(0xFFB9C5D7),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())))
                )

                val forecastPoints = points(forecast)
                val areaPath = Path().apply {
                    moveTo(forecastPoints.first().x, bottom)
                    forecastPoints.forEach { lineTo(it.x, it.y) }
                    lineTo(forecastPoints.last().x, bottom)
                    close()
                }
                drawPath(
                    areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x332A78FF), Color(0x002A78FF)),
                        startY = top,
                        endY = bottom
                    )
                )

                val forecastPath = Path().apply {
                    forecastPoints.forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                }
                drawPath(forecastPath, HighlightDarkest, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                forecastPoints.forEach { point ->
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = point)
                    drawCircle(HighlightDarkest, radius = 3.dp.toPx(), center = point)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = if (period == "7 days") {
                    listOf("Today", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                } else {
                    listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4", "Wk 5", "Wk 6", "Wk 7")
                }
                labels.forEach {
                    Text(it, color = NeutralLight, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, color = NeutralLight, fontSize = 10.sp)
    }
}

@Composable
private fun AttentionCard(
    title: String,
    body: String,
    footnote: String,
    action: String,
    onAction: () -> Unit,
    warning: Boolean = false
) {
    val accent = if (warning) WarningOrange else HighlightDarkest
    val tint = if (warning) WarningBackground else MutedBlue
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(30.dp)
                        .background(tint, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (warning) "!" else "↗", color = accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = NeutralDarkest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(body, color = NeutralLight, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(footnote, color = NeutralLight, fontSize = 9.sp, modifier = Modifier.weight(1f))
                Text(
                    action,
                    color = HighlightDarkest,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun InventoryScreen(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<StockItem?>(null) }
    var showAddProduct by remember { mutableStateOf(false) }
    val products = remember {
        mutableStateListOf(
        StockItem("Cooking oil · 1L", "OIL-001", 34, 5, 40),
        StockItem("White sugar · 2kg", "SUG-014", 58, 8, 24),
        StockItem("Maize flour · 2kg", "MAI-022", 126, 16, 0),
        StockItem("Long-grain rice · 2kg", "RIC-008", 92, 12, 0)
        )
    }
    val filteredProducts = when (selectedFilter) {
        "Low stock" -> products.filter { it.coverDays <= 7 }
        "Healthy" -> products.filter { it.coverDays > 7 }
        else -> products
    }
    val visibleProducts = filteredProducts.filter { it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Inventory health", color = NeutralDarkest, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Compare on-hand stock with expected demand.", color = NeutralLight, fontSize = 13.sp)
            }
            Button(onClick = { showAddProduct = true }, shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp), colors = ButtonDefaults.buttonColors(containerColor = HighlightDarkest)) {
                Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search products or SKU") }, shape = RoundedCornerShape(13.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Low stock", "Healthy").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }
        InfoBanner(
            title = "2 products need a closer look",
            body = "Low stock is based on forecast demand and your current on-hand quantities."
        )
        if (visibleProducts.isEmpty()) {
            Text("No products match your search.", color = NeutralLight, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
        }
        visibleProducts.forEach { product -> StockCard(product, onClick = { selectedProduct = product }) }
        Text(
            "Sample inventory · Connect your stock records for live recommendations",
            color = NeutralLight,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
    selectedProduct?.let { product -> ProductDetailDialog(product, onDismiss = { selectedProduct = null }) }
    if (showAddProduct) {
        AddProductDialog(
            onDismiss = { showAddProduct = false },
            onAdd = { name, sku, quantity -> products.add(0, StockItem(name, sku, quantity, 14, 0)); showAddProduct = false }
        )
    }
}

@Composable
private fun StockCard(item: StockItem, onClick: () -> Unit) {
    val low = item.coverDays <= 7
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick
    ) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (low) WarningBackground else MutedBlue, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.name.take(1), color = if (low) WarningOrange else HighlightDarkest, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, color = NeutralDarkest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("SKU ${item.sku}", color = NeutralLight, fontSize = 10.sp)
                }
                StatusPill(if (low) "Low cover" else "Healthy", low)
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StockMetric("ON HAND", "${item.quantity} units", Modifier.weight(1f))
                StockMetric("COVER", "${item.coverDays} days", Modifier.weight(1f))
                StockMetric("REORDER", if (item.recommendedOrder > 0) "+${item.recommendedOrder}" else "—", Modifier.weight(1f))
            }
            if (item.recommendedOrder > 0) {
                Spacer(Modifier.height(12.dp))
                Text("Suggested replenishment is a planning estimate.", color = NeutralLight, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ProductDetailDialog(item: StockItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, color = NeutralDarkest, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SKU ${item.sku}", color = NeutralLight, fontSize = 12.sp)
                androidx.compose.material3.HorizontalDivider(color = BorderColor)
                StockMetric("ON HAND", "${item.quantity} units")
                StockMetric("FORECAST COVER", "${item.coverDays} days")
                StockMetric("SUGGESTED REORDER", if (item.recommendedOrder > 0) "${item.recommendedOrder} units" else "No reorder suggested")
                Text(if (item.coverDays <= 7) "Review this item soon. Reorder quantities are estimates based on sample demand." else "Stock cover looks healthy based on sample demand." , color = NeutralLight, fontSize = 11.sp, lineHeight = 16.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = HighlightDarkest) } },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun AddProductDialog(onDismiss: () -> Unit, onAdd: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && sku.isNotBlank() && (quantity.toIntOrNull() ?: -1) >= 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add inventory item", color = NeutralDarkest, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add a product to this demo inventory.", color = NeutralLight, fontSize = 12.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Product name") }, singleLine = true)
                OutlinedTextField(sku, { sku = it }, label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("Units on hand") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onAdd(name.trim(), sku.trim().uppercase(), quantity.toInt()) }) { Text("Add item", color = HighlightDarkest) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NeutralLight) } },
        containerColor = SurfaceWhite
    )
}

@Composable
private fun StockMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, color = NeutralLight, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = NeutralDarkest, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusPill(text: String, warning: Boolean) {
    Text(
        text = text,
        color = if (warning) WarningOrange else SuccessGreen,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(if (warning) WarningBackground else SuccessBackground, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
private fun EventsScreen(events: List<MarketEvent>, onAddEvent: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Local events", color = NeutralDarkest, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Add context that can shift demand.", color = NeutralLight, fontSize = 13.sp)
            }
            Button(
                onClick = onAddEvent,
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HighlightDarkest)
            ) {
                Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        InfoBanner(
            title = "Events are planning signals",
            body = "Demand lifts shown here are estimates. Add local holidays, school dates, or promotions your team expects."
        )
        events.forEach { event -> EventCard(event) }
        Text("Demo calendar · Confirm dates and expected impact with your team.", color = NeutralLight, fontSize = 11.sp)
    }
}

@Composable
private fun EventCard(event: MarketEvent) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MutedBlue, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("•", color = HighlightDarkest, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(event.name, color = NeutralDarkest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(event.date, color = NeutralLight, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                Text(event.affectedProducts, color = NeutralLight, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(event.lift, color = HighlightDarkest, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("estimated lift", color = NeutralLight, fontSize = 8.sp)
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    var businessName by remember { mutableStateOf("Mazuri Wholesale Depot") }
    var location by remember { mutableStateOf("Nairobi, Kenya") }
    var horizon by remember { mutableStateOf("7 days") }
    var stockAlerts by remember { mutableStateOf(true) }
    var weeklySummary by remember { mutableStateOf(true) }
    var dialog by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(top = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Profile & settings", color = NeutralDarkest, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Manage your business workspace and preferences.", color = NeutralLight, fontSize = 13.sp)
        }
        SettingsCard(title = "BUSINESS PROFILE") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(MutedBlue, CircleShape), contentAlignment = Alignment.Center) { Text(businessName.take(1).uppercase(), color = HighlightDarkest, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(businessName, color = NeutralDarkest, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(location, color = NeutralLight, fontSize = 12.sp)
                }
                Text("Edit", color = HighlightDarkest, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { dialog = "business" }.padding(7.dp))
            }
            SettingRow("Account owner", "M. Kamau · Manager", "›") { dialog = "account" }
        }
        SettingsCard(title = "FORECAST PREFERENCES") {
            Text("Planning horizon", color = NeutralDarkest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Set the time range used for forecast planning.", color = NeutralLight, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf("7 days", "30 days").forEach { FilterChip(selected = horizon == it, onClick = { horizon = it }, label = { Text(it, fontSize = 11.sp) }) }
            }
            SettingRow("Business type", "Retail · Wholesale", "›") { dialog = "business type" }
            SettingRow("Week starts on", "Monday", "›") { dialog = "week start" }
        }
        SettingsCard(title = "NOTIFICATIONS") {
            SettingSwitch("Stock alerts", "Get notified when cover runs low", stockAlerts) { stockAlerts = it }
            androidx.compose.material3.HorizontalDivider(color = BorderColor)
            SettingSwitch("Weekly summary", "A planning recap every Monday", weeklySummary) { weeklySummary = it }
        }
        SettingsCard(title = "DATA & WORKSPACE") {
            SettingRow("Data source", "Demo sample data", "›") { dialog = "data source" }
            SettingRow("Currency", "Kenyan shilling (KES)", "›") { dialog = "currency" }
            SettingRow("Team access", "1 member", "›") { dialog = "team access" }
        }
        InfoBanner(title = "Demo workspace", body = "Forecast, inventory, and event values are illustrative. Connect your business data before using recommendations for purchasing.")
        Text("Volatile Demand Forecasting · Version 1.0.0", color = NeutralLight, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    dialog?.let { setting ->
        if (setting == "business") {
            BusinessEditDialog(businessName, location, onDismiss = { dialog = null }, onSave = { name, place -> businessName = name; location = place; dialog = null })
        } else {
            val detail = when (setting) {
                "account" -> "Signed in as M. Kamau. Account management will be available when sign-in is connected."
                "data source" -> "This workspace uses illustrative sample data. Connect a business data source to enable live forecasts."
                "team access" -> "You are the only member in this demo workspace."
                "currency" -> "Currency display is set to Kenyan shillings (KES)."
                "business type" -> "Retail and wholesale demand model. This demo setting is informational."
                else -> "Your reporting week starts on Monday."
            }
            AlertDialog(onDismissRequest = { dialog = null }, title = { Text(setting.replaceFirstChar { it.uppercase() }, color = NeutralDarkest, fontWeight = FontWeight.Bold) }, text = { Text(detail, color = NeutralLight, fontSize = 13.sp, lineHeight = 19.sp) }, confirmButton = { TextButton(onClick = { dialog = null }) { Text("Done", color = HighlightDarkest) } }, containerColor = SurfaceWhite)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SurfaceWhite), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp)) {
            Text(title, color = NeutralLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingRow(title: String, detail: String, trailing: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NeutralDarkest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = NeutralLight, fontSize = 10.sp)
        }
        Text(trailing, color = NeutralLight, fontSize = 20.sp)
    }
}

@Composable
private fun SettingSwitch(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = NeutralDarkest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = NeutralLight, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BusinessEditDialog(name: String, location: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var editedName by remember { mutableStateOf(name) }
    var editedLocation by remember { mutableStateOf(location) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit business profile", color = NeutralDarkest, fontWeight = FontWeight.Bold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(editedName, { editedName = it }, label = { Text("Business name") }, singleLine = true)
            OutlinedTextField(editedLocation, { editedLocation = it }, label = { Text("Location") }, singleLine = true)
        }
    }, confirmButton = { TextButton(enabled = editedName.isNotBlank() && editedLocation.isNotBlank(), onClick = { onSave(editedName.trim(), editedLocation.trim()) }) { Text("Save", color = HighlightDarkest) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NeutralLight) } }, containerColor = SurfaceWhite)
}

@Composable
private fun InfoBanner(title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MutedBlue, RoundedCornerShape(14.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("i", color = HighlightDarkest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = NeutralDarkest, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(body, color = Color(0xFF4F6075), fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun AppBottomNavigation(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = SurfaceWhite,
        tonalElevation = 0.dp
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { NavigationGlyph(tab, selectedTab == tab) },
                label = { Text(tab.title, fontSize = 9.sp, maxLines = 1) },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun NavigationGlyph(tab: AppTab, selected: Boolean) {
    val color = if (selected) HighlightDarkest else NeutralLight
    Canvas(Modifier.size(21.dp)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (tab) {
            AppTab.Forecast -> {
                drawLine(color, Offset(3 * sx, 20 * sy), Offset(21 * sx, 20 * sy), strokeWidth = 1.5.dp.toPx())
                drawLine(color, Offset(4 * sx, 18 * sy), Offset(4 * sx, 5 * sy), strokeWidth = 1.5.dp.toPx())
                val path = Path().apply {
                    moveTo(5 * sx, 16 * sy)
                    lineTo(10 * sx, 11 * sy)
                    lineTo(14 * sx, 14 * sy)
                    lineTo(20 * sx, 6 * sy)
                }
                drawPath(path, color, style = stroke)
            }

            AppTab.Inventory -> {
                drawRoundRect(color, Offset(3 * sx, 4 * sy), Size(18 * sx, 16 * sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2 * sx, 2 * sy), style = stroke)
                drawLine(color, Offset(3 * sx, 9 * sy), Offset(21 * sx, 9 * sy), strokeWidth = 1.5.dp.toPx())
                drawLine(color, Offset(9 * sx, 9 * sy), Offset(9 * sx, 20 * sy), strokeWidth = 1.5.dp.toPx())
            }

            AppTab.Events -> {
                drawRoundRect(color, Offset(3 * sx, 5 * sy), Size(18 * sx, 16 * sy), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2 * sx, 2 * sy), style = stroke)
                drawLine(color, Offset(3 * sx, 9 * sy), Offset(21 * sx, 9 * sy), strokeWidth = 1.5.dp.toPx())
                drawLine(color, Offset(8 * sx, 3 * sy), Offset(8 * sx, 7 * sy), strokeWidth = 1.5.dp.toPx())
                drawLine(color, Offset(16 * sx, 3 * sy), Offset(16 * sx, 7 * sy), strokeWidth = 1.5.dp.toPx())
            }

            AppTab.Profile -> {
                drawCircle(color, radius = 4 * sx, center = Offset(12 * sx, 8 * sy), style = stroke)
                val path = Path().apply {
                    moveTo(4 * sx, 21 * sy)
                    cubicTo(4 * sx, 16 * sy, 8 * sx, 14 * sy, 12 * sx, 14 * sy)
                    cubicTo(16 * sx, 14 * sy, 20 * sx, 16 * sy, 20 * sx, 21 * sy)
                }
                drawPath(path, color, style = stroke)
            }
        }
    }
}

@Composable
private fun AddEventDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var eventName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a local event", color = NeutralDarkest, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Give your forecast a useful planning signal.", color = NeutralLight, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = eventName,
                    onValueChange = { eventName = it },
                    label = { Text("Event name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (eventName.isNotBlank()) onAdd(eventName.trim()) }, enabled = eventName.isNotBlank()) {
                Text("Add event", color = HighlightDarkest)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NeutralLight) } },
        containerColor = SurfaceWhite
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Composable
private fun ForecastPreview() {
    VolatileDemandForecastingTheme(darkTheme = false, dynamicColor = false) {
        DemandForecastApp()
    }
}
