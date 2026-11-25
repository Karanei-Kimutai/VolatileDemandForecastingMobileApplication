package com.example.volatiledemandforecasting.feature.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastConfig
import com.example.volatiledemandforecasting.app.navigation.navigateToProduct
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.ItemActionsButton
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.SearchField
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.catalog.DemandClasses
import com.example.volatiledemandforecasting.data.catalog.ProductDto
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.forecasts.NoSalesYet
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

class ProductsFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.nav_products
    override val tab = MainTab.Products

    private val viewModel: ProductsViewModel by appViewModels { ProductsViewModel(it.productRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val demandClass by viewModel.demandClass.collectAsStateWithLifecycle()

        StoreGate(selection, modifier = Modifier.padding(padding)) { store ->
            Column(Modifier.padding(padding)) {
                SearchField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = stringResource(R.string.products_search),
                    modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.md),
                )
                // Every demand kind is one tap away, instead of hidden behind a filter menu.
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(listOf<String?>(null) + DemandClasses.filterable) { code ->
                        val selected = code == demandClass
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setDemandClass(code) },
                            label = { Text(demandClassFilterLabel(code)) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall)) }
                            } else null,
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
                val content = (state as? UiState.Content)?.data
                if (content != null) {
                    Text(
                        pluralStringResource(R.plurals.products_count, content.total, content.total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.gutter).semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                UiStateHost(
                    state = state,
                    onRetry = viewModel::retry,
                    empty = {
                        if (query.isBlank() && demandClass == null) {
                            NoSalesYet(store.canManageData, onUpload = { navController.navigateSingle(R.id.importWizardFragment) }, products = true)
                        } else {
                            EmptyState(
                                title = stringResource(R.string.products_empty_title),
                                body = stringResource(R.string.products_empty_body),
                                icon = Icons.Outlined.SearchOff,
                            )
                        }
                    },
                ) { list ->
                    val listState = rememberLazyListState()
                    LaunchedEffect(listState, list.items.size) {
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                            .collect { last -> if (last != null && last >= list.items.size - 5) viewModel.loadMore() }
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm, bottom = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(list.items, key = { it.id }) { product ->
                            val entries = buildList {
                                add(MenuEntry(stringResource(R.string.product_view_details), { navController.navigateToProduct(product.id) }, Icons.Outlined.Info))
                                if (store.canGenerateForecasts) {
                                    add(MenuEntry(stringResource(R.string.product_forecast_next_day), { navController.navigateToForecastConfig(product.id, horizon = 1) }, Icons.Outlined.Insights))
                                    add(MenuEntry(stringResource(R.string.product_forecast_choose), { navController.navigateToForecastConfig(product.id) }, Icons.Outlined.Tune))
                                }
                            }
                            // Long-press still opens the context menu (spec §6.2); the ⋮ button shows the same actions.
                            ContextMenuBox(entries, onClick = { navController.navigateToProduct(product.id) }) {
                                ProductRow(product, entries)
                            }
                        }
                        if (list.loadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(Sizes.icon), strokeWidth = 2.dp)
                                }
                            }
                        }
                        if (list.loadMoreError != null) {
                            item {
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(list.loadMoreError.asString(), style = MaterialTheme.typography.bodySmall)
                                    SecondaryButton(stringResource(R.string.products_load_more), viewModel::loadMore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Name first, then what identifies it, then how it sells. The ⋮ button holds its actions. */
@Composable
private fun ProductRow(product: ProductDto, actions: List<MenuEntry>) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Monogram(product.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOfNotNull(
                    product.sku.takeIf { it != product.name }?.let { stringResource(R.string.product_sku, it) },
                    product.category,
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                product.demandClass?.label?.let { DemandClassChip(it, Modifier.padding(top = Spacing.xs)) }
            }
            ItemActionsButton(actions, product.name)
        }
    }
}

/** The product's initial on a tinted square: a quick visual anchor when scanning a long list. */
@Composable
internal fun Monogram(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier.size(Sizes.iconBadge),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}
