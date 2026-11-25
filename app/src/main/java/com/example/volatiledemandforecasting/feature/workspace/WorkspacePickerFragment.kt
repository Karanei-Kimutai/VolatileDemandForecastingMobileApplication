package com.example.volatiledemandforecasting.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Storefront
import com.example.volatiledemandforecasting.app.navigation.navigateToCreateShop
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.IconBadge
import com.example.volatiledemandforecasting.core.ui.components.TonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.DemoDataBadge
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.Roles
import com.example.volatiledemandforecasting.data.workspace.StoreDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceDto
import com.example.volatiledemandforecasting.data.workspace.WorkspaceWithStores

class WorkspacePickerFragment : ShellFragment() {
    override val titleRes = R.string.screen_workspace_picker

    private val viewModel: WorkspacePickerViewModel by appViewModels {
        WorkspacePickerViewModel(it.workspaceRepository, it.activeStore)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()
        UiStateHost(
            state = state,
            onRetry = viewModel::load,
            modifier = Modifier.padding(padding),
            empty = {},
        ) { data ->
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (data.own.isEmpty()) {
                    // First launch: set up a shop, or look around the sample shop first.
                    item { Welcome(sample = data.sample, onSample = { viewModel.trySample { navController.popBackStack() } }, onSetUp = { navController.navigateToCreateShop() }) }
                    return@LazyColumn
                }
                if (data.viewingSample) {
                    item { InlineMessage(stringResource(R.string.picker_viewing_sample), StatusKind.Neutral) }
                }
                item {
                    Text(stringResource(R.string.picker_your_workspaces), style = MaterialTheme.typography.titleSmall)
                }
                items(data.own, key = { it.workspace.id }) { entry ->
                    WorkspaceCard(
                        entry,
                        data.currentStoreId,
                        blurb = null,
                        onSetUp = { navController.navigateToCreateShop(entry.workspace) },
                    ) { workspace, store ->
                        viewModel.select(workspace, store) { navController.popBackStack() }
                    }
                }
                if (data.sample != null && !data.viewingSample) {
                    item {
                        TextButton(onClick = { viewModel.trySample { navController.popBackStack() } }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.picker_open_sample))
                        }
                    }
                }
                item {
                    SecondaryButton(
                        stringResource(R.string.picker_create_another),
                        { navController.navigateToCreateShop() },
                        Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.AddBusiness,
                    )
                }
            }
        }
    }
}

@Composable
private fun Welcome(sample: Pair<WorkspaceDto, StoreDto>?, onSample: () -> Unit, onSetUp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyMedium)
        PrimaryButton(stringResource(R.string.welcome_set_up), onSetUp, Modifier.fillMaxWidth())
        if (sample != null) {
            SecondaryButton(stringResource(R.string.welcome_try_sample), onSample, Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.welcome_sample_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkspaceCard(
    entry: WorkspaceWithStores,
    currentStoreId: String?,
    blurb: String?,
    onSetUp: () -> Unit,
    onPick: (WorkspaceDto, StoreDto) -> Unit,
) {
    val workspace = entry.workspace
    AppCard(contentPadding = false) {
        Column(Modifier.padding(vertical = Spacing.md)) {
            Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(workspace.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (workspace.isReference) DemoDataBadge()
                }
                if (blurb != null) Text(blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.picker_role, roleLabel(workspace)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (entry.stores.isEmpty()) {
                // Was a dead end (P01/P14): the business exists but its shop was never added.
                Column(Modifier.padding(horizontal = Spacing.lg).padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(stringResource(R.string.picker_no_stores), style = MaterialTheme.typography.bodyMedium)
                    if (workspace.role == Roles.OWNER || workspace.role == Roles.MANAGER) {
                        TonalButton(stringResource(R.string.picker_finish_setup), onSetUp, Modifier.fillMaxWidth(), icon = Icons.Outlined.Storefront)
                    }
                }
            }
            entry.stores.forEach { store ->
                HorizontalDivider(Modifier.padding(top = Spacing.md), color = MaterialTheme.colorScheme.outlineVariant)
                val selected = store.id == currentStoreId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(workspace, store) })
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    IconBadge(Icons.Outlined.Storefront)
                    Column(Modifier.weight(1f)) {
                        Text(store.name, style = MaterialTheme.typography.titleSmall)
                        if (store.historyStart != null && store.historyEnd != null) {
                            Text(
                                stringResource(R.string.picker_history_range, Formatting.date(store.historyStart), Formatting.date(store.historyEnd)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (selected) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.picker_current), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun roleLabel(workspace: WorkspaceDto): String = stringResource(
    when {
        workspace.isReference -> R.string.role_reference
        workspace.role == Roles.OWNER -> R.string.role_owner
        workspace.role == Roles.MANAGER -> R.string.role_manager
        else -> R.string.role_viewer
    }
)
