package com.example.volatiledemandforecasting.app

import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.components.AppScaffold
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.data.workspace.Selection

/**
 * A [ComposeFragment] that sits inside the app shell: top bar with the shop switcher and
 * Options menu, plus the bottom navigation bar when [tab] is set (spec §5).
 */
abstract class ShellFragment : ComposeFragment() {

    protected val shell: ShellViewModel by appActivityViewModels {
        ShellViewModel(it.session, it.userRepository, it.authRepository, it.activeStore)
    }

    @get:StringRes
    protected abstract val titleRes: Int

    /** The bottom-navigation tab this screen belongs to, or null for a supporting screen. */
    protected open val tab: MainTab? = null

    /** True for screens whose data can come from the offline cache, so they show the "saved copy" notice. */
    protected open val showsCachedData: Boolean get() = false

    /** Supporting screens (no bottom bar) get a back arrow. */
    protected open val showBackButton: Boolean get() = tab == null

    /** What the top bar's back arrow does. Screens with their own steps make it match system Back. */
    protected open fun onTopBarBack() {
        findNavController().navigateUp()
    }

    /** The top bar title. Screens about one thing (a product, a forecast) override it with that thing's name. */
    /** The sign-out confirmation; screens other than the ⋮ menu (Settings) can open it too. */
    private val signOutConfirm = mutableStateOf(false)

    protected fun requestSignOut() {
        signOutConfirm.value = true
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        signOutConfirm.value = savedInstanceState?.getBoolean(STATE_SIGN_OUT) ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SIGN_OUT, signOutConfirm.value)
    }

    @Composable
    protected open fun title(): String = stringResource(titleRes)

    /** A contextual action for the top bar, left of the Options menu. */
    @Composable
    protected open fun RowScope.ContextualAction() {}

    @Composable
    protected abstract fun ScreenContent(padding: PaddingValues)

    /** The workspace and store the user is working in. Store-scoped screens pass this to [StoreGate]. */
    @Composable
    protected fun collectSelection(): State<Selection> = appContainer.activeStore.state.collectAsStateWithLifecycle()

    @Composable
    final override fun Content() {
        val navController = findNavController()
        val state by shell.state.collectAsStateWithLifecycle()
        var showAbout by rememberSaveable { mutableStateOf(false) }
        var showSignOutConfirm by signOutConfirm

        val isDebuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        val optionsMenu = buildList {
            add(MenuEntry(stringResource(R.string.menu_switch_shop), { navController.navigateSingle(R.id.workspacePickerFragment) }, Icons.Outlined.SwapHoriz))
            add(MenuEntry(stringResource(R.string.menu_model_info), { navController.navigateSingle(R.id.modelInfoFragment) }, Icons.Outlined.AutoAwesome))
            add(MenuEntry(stringResource(R.string.menu_settings), { navController.navigateSingle(R.id.settingsFragment) }, Icons.Outlined.Settings))
            add(MenuEntry(stringResource(R.string.menu_help), { navController.navigateSingle(R.id.helpFragment) }, Icons.AutoMirrored.Outlined.HelpOutline))
            add(MenuEntry(stringResource(R.string.menu_about), { showAbout = true }, Icons.Outlined.Info))
            add(MenuEntry(stringResource(R.string.menu_sign_out), { showSignOutConfirm = true }, Icons.AutoMirrored.Outlined.Logout))
            if (isDebuggable) {
                add(MenuEntry(stringResource(R.string.menu_dev_gallery), { navController.navigateSingle(R.id.devGalleryFragment) }, Icons.Outlined.Widgets))
            }
        }

        AppScaffold(
            title = title(),
            workspace = state.workspace,
            onWorkspaceClick = { navController.navigateSingle(R.id.workspacePickerFragment) },
            optionsMenu = optionsMenu,
            selectedTab = tab,
            onTabSelected = navController::navigateToTab,
            onBack = if (showBackButton) ({ onTopBarBack() }) else null,
            actions = { ContextualAction() },
            content = { padding ->
                val savedAt by appContainer.responseCache.servedFromCache.collectAsStateWithLifecycle()
                Column(Modifier.padding(top = padding.calculateTopPadding())) {
                    if (showsCachedData) savedAt?.let { OfflineCopyNotice(it) }
                    ScreenContent(PaddingValues(bottom = padding.calculateBottomPadding()))
                }
            },
        )

        if (showAbout) AboutDialog(onDismiss = { showAbout = false })
        if (showSignOutConfirm) {
            ConfirmationDialog(
                title = stringResource(R.string.sign_out_title),
                body = stringResource(R.string.sign_out_body),
                confirmLabel = stringResource(R.string.menu_sign_out),
                onConfirm = {
                    showSignOutConfirm = false
                    shell.signOut()
                },
                onDismiss = { showSignOutConfirm = false },
            )
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = { Text(stringResource(R.string.about_body, stringResource(R.string.app_name), version)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

private const val STATE_SIGN_OUT = "signOutConfirm"
