package com.example.volatiledemandforecasting.app.navigation

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.example.volatiledemandforecasting.R

/**
 * The four bottom-navigation destinations (spec §5.1). The selected tab shows the filled icon,
 * so the current place is marked by shape as well as colour.
 */
enum class MainTab(
    @param:IdRes val destinationId: Int,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Dashboard(R.id.dashboardFragment, R.string.nav_dashboard, Icons.Outlined.Home, Icons.Filled.Home),
    Products(R.id.productsFragment, R.string.nav_products, Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
    Data(R.id.dataManagementFragment, R.string.nav_data, Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    Forecasts(R.id.forecastsFragment, R.string.nav_forecasts, Icons.Outlined.Insights, Icons.Filled.Insights),
}

/**
 * Switches tab without stacking duplicates: reselecting a tab does nothing, and each tab
 * keeps its own back stack (spec §5.2).
 */
fun NavController.navigateToTab(tab: MainTab) {
    navigate(
        tab.destinationId,
        null,
        navOptions {
            popUpTo(graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        },
    )
}

/** Key for the Forecasts tab's view ("Products" or "Store") requested by another screen. */
const val KEY_FORECASTS_VIEW = "forecastsView"

/**
 * Opens the Forecasts tab on a given view. Tabs restore their saved state, so arguments would be
 * ignored; the request goes on the tab's own back-stack entry, which the tab reads and clears.
 */
fun NavController.navigateToForecasts(view: String) {
    navigateToTab(MainTab.Forecasts)
    currentBackStackEntry?.savedStateHandle?.set(KEY_FORECASTS_VIEW, view)
}

/** Opens a supporting screen once: tapping its menu entry again doesn't stack a second copy. */
fun NavController.navigateSingle(destinationId: Int, args: android.os.Bundle? = null) {
    if (currentDestination?.id == destinationId && args == null) return
    navigate(destinationId, args, navOptions { launchSingleTop = true })
}
