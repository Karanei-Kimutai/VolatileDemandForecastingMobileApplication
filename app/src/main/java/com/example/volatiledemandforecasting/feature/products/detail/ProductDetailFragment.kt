package com.example.volatiledemandforecasting.feature.products.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.ARG_PRODUCT_ID
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastConfig
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.charts.ChartSeries
import com.example.volatiledemandforecasting.core.ui.charts.LineChart
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SectionHeader
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.feature.forecasts.ForecastRow
import com.example.volatiledemandforecasting.feature.products.Monogram

/**
 * One product: what it is, how it has sold, its latest forecasts, and the way to forecast it.
 * The forecast action stays pinned to the bottom so it never scrolls out of reach.
 */
class ProductDetailFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.screen_product

    private val viewModel: ProductDetailViewModel by appViewModels {
        ProductDetailViewModel(
            requireArguments().getString(ARG_PRODUCT_ID).orEmpty(),
            it.productRepository, it.storeRepository, it.forecastRepository, it.activeStore,
        )
    }

    @Composable
    override fun title(): String {
        val state by viewModel.state.collectAsStateWithLifecycle()
        return (state as? UiState.Content)?.data?.product?.name ?: stringResource(titleRes)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val days by viewModel.days.collectAsStateWithLifecycle()
        val navController = findNavController()
        UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { detail ->
            val product = detail.product
            Column(Modifier.padding(padding).fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                ) {
                    Header(detail)
                    SalesCard(detail, days, onDays = viewModel::setDays)
                    if (detail.recentForecasts.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.product_recent_forecasts), Modifier.padding(top = Spacing.sm))
                        detail.recentForecasts.forEach { item ->
                            ForecastRow(item, onClick = { navController.navigateToForecastResult(item.forecastId) })
                        }
                    }
                }
                if (detail.store.canGenerateForecasts) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        PrimaryButton(
                            stringResource(R.string.product_forecast_this),
                            onClick = { navController.navigateToForecastConfig(product.id) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = Spacing.md),
                            icon = Icons.Outlined.Insights,
                        )
                    }
                }
            }
        }
    }
}

/** The name is the screen title, so the header carries what identifies the product and how it sells. */
@Composable
private fun Header(detail: ProductDetail) {
    val product = detail.product
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Monogram(product.name)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            val identity = listOfNotNull(
                stringResource(R.string.product_sku, product.sku).takeIf { product.sku != product.name },
                product.category,
            ).joinToString(" · ")
            if (identity.isNotEmpty()) Text(identity, style = MaterialTheme.typography.titleSmall)
            product.firstSaleDate?.let {
                Text(
                    stringResource(R.string.product_first_sale, Formatting.date(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        product.demandClass?.label?.let { DemandClassChip(it) }
    }
}

/** The sales chart with its period picker, and three plain figures read straight off it. */
@Composable
private fun SalesCard(detail: ProductDetail, days: Int, onDays: (Int) -> Unit) {
    AppCard {
        Text(stringResource(R.string.product_sales_title), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = Spacing.md)) {
            HISTORY_RANGES.forEachIndexed { index, range ->
                SegmentedButton(
                    selected = range == days,
                    onClick = { onDays(range) },
                    shape = SegmentedButtonDefaults.itemShape(index, HISTORY_RANGES.size),
                    label = { Text(pluralStringResource(R.plurals.product_range_days, range, range)) },
                )
            }
        }
        if (detail.hasAnySales && detail.firstDate != null) {
            FlowRow(
                Modifier.fillMaxWidth().padding(top = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Stat(stringResource(R.string.product_stat_units), Formatting.units(detail.totalUnits))
                Stat(stringResource(R.string.product_stat_days), stringResource(R.string.product_stat_days_value, detail.recordedDays, detail.units.size))
                detail.lastRecorded?.let { (date, units) ->
                    Stat(stringResource(R.string.product_stat_last), stringResource(R.string.product_stat_last_value, Formatting.units(units), Formatting.shortDate(date.toString())))
                }
            }
            val first = detail.firstDate
            val labels = remember(detail) {
                listOf(0, detail.units.size / 2, detail.units.size - 1)
                    .map { it to Formatting.shortDate(first.plusDays(it.toLong()).toString()) }
            }
            val seriesLabel = stringResource(R.string.res_series_history)
            LineChart(
                series = listOf(ChartSeries(seriesLabel, detail.units, MaterialTheme.appColors.chartAlternative, fill = true)),
                pointCount = detail.units.size,
                xLabels = labels,
                description = pluralStringResource(
                    R.plurals.product_chart_description,
                    detail.recordedDays,
                    detail.units.size,
                    Formatting.units(detail.totalUnits),
                    detail.recordedDays,
                ),
                modifier = Modifier.padding(top = Spacing.lg),
            )
            Text(
                stringResource(R.string.product_history_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        } else {
            Text(
                stringResource(R.string.product_no_history),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.lg),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge.tabular())
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
