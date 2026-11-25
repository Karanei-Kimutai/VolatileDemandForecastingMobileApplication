package com.example.volatiledemandforecasting.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.DestructiveButton
import com.example.volatiledemandforecasting.core.ui.components.KeyValueRow
import com.example.volatiledemandforecasting.core.ui.components.NavigationRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appContainer
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.datastore.ThemeMode
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.Roles
import com.example.volatiledemandforecasting.data.workspace.StoreDto
import kotlinx.coroutines.launch

class SettingsFragment : ShellFragment() {
    override val titleRes = R.string.screen_settings

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val shellState by shell.state.collectAsStateWithLifecycle()
        val selection by collectSelection()
        val prefs = appContainer.preferences
        val theme by prefs.themeMode.collectAsStateWithLifecycle(ThemeMode.System)
        val display by prefs.tableDisplay.collectAsStateWithLifecycle(TableDisplay.Decimals)
        val scope = rememberCoroutineScope()
        var storeDetail by remember { mutableStateOf<StoreDto?>(null) }

        val selected = (selection as? com.example.volatiledemandforecasting.data.workspace.Selection.Selected)?.store
        // Time zone and currency belong to the store; a failed lookup just leaves them out.
        LaunchedEffect(selected?.storeId) {
            storeDetail = null
            selected?.let { s -> attempt { appContainer.workspaceRepository.store(s.storeId) }.onSuccess { storeDetail = it } }
        }

        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        ) {
            Section(R.string.set_profile) {
                shellState.user?.let {
                    KeyValueRow(stringResource(R.string.set_name), it.displayName)
                    KeyValueRow(stringResource(R.string.auth_email), it.email)
                }
                LinkRow(stringResource(R.string.set_change_password), Icons.Outlined.Lock) { navController.navigateSingle(R.id.changePasswordFragment) }
            }

            Section(R.string.set_store) {
                if (selected == null) {
                    Text(stringResource(R.string.set_no_store), style = MaterialTheme.typography.bodyMedium)
                } else {
                    KeyValueRow(stringResource(R.string.set_store), if (selected.isReference) stringResource(R.string.sample_shop_name) else selected.storeName)
                    if (!selected.isReference && selected.workspaceName != selected.storeName) {
                        KeyValueRow(stringResource(R.string.set_workspace), selected.workspaceName)
                    }
                    KeyValueRow(stringResource(R.string.set_role), roleName(if (selected.isReference) "REFERENCE" else selected.role))
                    storeDetail?.let {
                        KeyValueRow(stringResource(R.string.set_timezone), it.timezone)
                        KeyValueRow(stringResource(R.string.set_currency), it.currency)
                    }
                }
                LinkRow(stringResource(R.string.set_switch), Icons.Outlined.SwapHoriz) { navController.navigateSingle(R.id.workspacePickerFragment) }
            }

            Section(R.string.set_appearance) {
                Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.labelLarge)
                SegmentedChoice(
                    options = ThemeMode.entries,
                    selected = theme,
                    label = { mode ->
                        stringResource(
                            when (mode) {
                                ThemeMode.System -> R.string.set_theme_system
                                ThemeMode.Light -> R.string.set_theme_light
                                ThemeMode.Dark -> R.string.set_theme_dark
                            }
                        )
                    },
                    onSelect = { mode -> scope.launch { prefs.setThemeMode(mode) } },
                )
                Text(stringResource(R.string.set_numbers), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = Spacing.sm))
                SegmentedChoice(
                    options = TableDisplay.entries,
                    selected = display,
                    label = { mode -> stringResource(if (mode == TableDisplay.Decimals) R.string.set_numbers_decimals else R.string.set_numbers_whole) },
                    onSelect = { mode -> scope.launch { prefs.setTableDisplay(mode) } },
                )
            }

            Section(R.string.set_about) {
                LinkRow(stringResource(R.string.screen_help), Icons.AutoMirrored.Outlined.HelpOutline) { navController.navigateSingle(R.id.helpFragment) }
                KeyValueRow(stringResource(R.string.set_version), appVersion())
                Text(stringResource(R.string.set_notifications_later), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Also in the ⋮ menu; here it is where people look for it.
            DestructiveButton(
                stringResource(R.string.menu_sign_out),
                ::requestSignOut,
                Modifier.fillMaxWidth().padding(top = Spacing.sm),
                icon = Icons.AutoMirrored.Outlined.Logout,
            )
        }
    }

    private fun appVersion(): String =
        runCatching { requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName }.getOrNull().orEmpty()
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.xs))
            content()
        }
    }
}

/** A row of connected toggle buttons: one choice from a few, all visible at once. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SegmentedChoice(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}

@Composable
private fun roleName(role: String): String = stringResource(
    when (role) {
        Roles.OWNER -> R.string.role_owner
        Roles.MANAGER -> R.string.role_manager
        Roles.VIEWER -> R.string.role_viewer
        else -> R.string.role_reference
    }
)

@Composable
private fun LinkRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationRow(title = label, onClick = onClick, icon = icon, inset = false)
}
