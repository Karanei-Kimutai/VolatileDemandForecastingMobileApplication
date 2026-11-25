package com.example.volatiledemandforecasting.feature.data.readiness

import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.Term
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.data.catalog.ProductRepository
import com.example.volatiledemandforecasting.feature.products.Monogram
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.navigateToProduct
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.components.toUiState
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.store.ProductReadinessDto
import com.example.volatiledemandforecasting.data.store.ReadinessDetailDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import com.example.volatiledemandforecasting.feature.workspace.StoreGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** The readiness report plus product names: the report itself only carries SKUs (P32). */
data class ReadinessScreen(val report: ReadinessDetailDto, val names: Map<String, String>)

/** Products are fetched in pages of the API's maximum size; names are a nicety, so this stops early. */
private const val NAME_PAGE_SIZE = 200
private const val NAME_MAX_PAGES = 10

class ReadinessViewModel(
    private val stores: StoreRepository,
    private val products: ProductRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<ReadinessScreen>>(UiState.Loading)
    val state: StateFlow<UiState<ReadinessScreen>> = _state.asStateFlow()
    private var store: StoreSelection? = null

    init {
        viewModelScope.launch {
            combine(activeStore.selected, activeStore.dataRevision) { s, _ -> s }.collect {
                store = it
                load()
            }
        }
    }

    fun load() {
        val s = store ?: return
        _state.value = UiState.Loading
        viewModelScope.launch {
            attempt { stores.readinessDetail(s.storeId) }
                .onSuccess { report ->
                    _state.value = if (report.products.isEmpty()) UiState.Empty else UiState.Content(ReadinessScreen(report, names(s.storeId)))
                }
                .onFailure { _state.value = it.toUiState() }
        }
    }

    /** Product id to name. If a page fails, the rows that are missing fall back to the SKU. */
    private suspend fun names(storeId: String): Map<String, String> = buildMap {
        var page = 1
        while (page <= NAME_MAX_PAGES) {
            val result = attempt { products.products(storeId, "", null, page, NAME_PAGE_SIZE) }.getOrNull() ?: break
            result.items.forEach { put(it.id, it.name) }
            if (page * result.pageSize >= result.total || result.items.isEmpty()) break
            page++
        }
    }
}

class ReadinessFragment : ShellFragment() {
    override val titleRes = R.string.screen_readiness

    private val viewModel: ReadinessViewModel by appViewModels { ReadinessViewModel(it.storeRepository, it.productRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()
        StoreGate(selection, modifier = Modifier.padding(padding)) {
            UiStateHost(
                state, onRetry = viewModel::load, modifier = Modifier.padding(padding),
                empty = { EmptyState(stringResource(R.string.ready_empty_title), stringResource(R.string.ready_empty_body)) },
            ) { screen ->
                val report = screen.report
                LazyColumn(
                    Modifier.padding(padding),
                    contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.ready_summary, report.productsReady, report.productsTotal),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                InfoHint(Term.Readiness)
                            }
                            // The thresholds are the API's own, for the current model.
                            Text(
                                stringResource(R.string.ready_rules, report.minHistoryDays, report.maxMissingDaysInLookback),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (report.stale) {
                                InlineMessage(stringResource(R.string.ready_stale), StatusKind.Warning)
                            }
                            Text(stringResource(R.string.ready_baselines_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(report.products, key = { it.productId }) { product ->
                        val open = { navController.navigateToProduct(product.productId) }
                        ContextMenuBox(
                            listOf(MenuEntry(stringResource(R.string.product_view_details), open)),
                            onClick = open,
                        ) { ProductReadinessRow(product, screen.names[product.productId], report) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductReadinessRow(product: ProductReadinessDto, name: String?, report: ReadinessDetailDto) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Monogram(name ?: product.sku)
                Column(Modifier.weight(1f)) {
                    Text(name ?: product.sku, style = MaterialTheme.typography.titleSmall)
                    if (name != null) {
                        Text(
                            stringResource(R.string.product_sku, product.sku),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(
                stringResource(if (product.ready) R.string.ready_ok else R.string.ready_not_ok),
                if (product.ready) StatusKind.Success else StatusKind.Warning,
                Modifier.padding(top = Spacing.xs),
            )
            product.reasons.forEach { reason ->
                Text(
                    stringResource(
                        when (reason) {
                            "INSUFFICIENT_HISTORY" -> R.string.ready_reason_history
                            "TOO_MANY_GAPS" -> R.string.ready_reason_gaps
                            "STALE_HISTORY" -> R.string.ready_reason_stale
                            else -> R.string.ready_reason_other
                        },
                        report.minHistoryDays,
                        report.maxMissingDaysInLookback,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                stringResource(R.string.ready_days, product.recordedDaysInLookback, product.missingDaysInLookback) +
                    (product.latestDate?.let { " · " + stringResource(R.string.ready_latest, Formatting.date(it)) } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
