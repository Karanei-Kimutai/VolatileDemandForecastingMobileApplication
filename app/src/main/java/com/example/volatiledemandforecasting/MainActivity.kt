package com.example.volatiledemandforecasting

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.volatiledemandforecasting.app.appContainer
import com.example.volatiledemandforecasting.app.navigation.AUTH_DESTINATIONS
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.data.workspace.Selection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns only the Fragment host (spec §4.2) and the one rule about the session: signed-out users see
 * the sign-in screens, signed-in users don't. That covers start-up, signing out, and a refresh
 * token that stops working. On the very first launch the intro tour comes before either.
 */
class MainActivity : FragmentActivity() {

    private var firstRouteDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Keep the splash up until the first screen is chosen (spec §8.1). No fake progress.
        installSplashScreen().setKeepOnScreenCondition { !firstRouteDone }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(FragmentContainerView(this).apply { id = R.id.nav_host_container })

        val navHost = if (savedInstanceState == null) {
            NavHostFragment.create(R.navigation.main_graph).also { host ->
                supportFragmentManager.commitNow {
                    setReorderingAllowed(true)
                    replace(R.id.nav_host_container, host)
                    setPrimaryNavigationFragment(host)
                }
            }
        } else {
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment
        }

        val navController = navHost.navController
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appContainer.session.state.collect { state ->
                    route(state, navController, tourSeen = appContainer.preferences.tourSeen.first())
                    firstRouteDone = true
                }
            }
        }
    }

    private fun route(state: SessionState, navController: NavController, tourSeen: Boolean) {
        val current = navController.currentDestination?.id
        // The tour decides where to go when it ends (register, sign in, or the dashboard).
        if (current == R.id.onboardingFragment) return
        val onAuthScreen = current in AUTH_DESTINATIONS
        val target = when {
            !tourSeen -> R.id.onboardingFragment
            state is SessionState.SignedOut && !onAuthScreen -> R.id.loginFragment
            state is SessionState.SignedIn && onAuthScreen -> R.id.dashboardFragment
            else -> return
        }
        // Clear the whole back stack, so Back never returns to the previous user's screens.
        navController.navigate(target, null, navOptions { popUpTo(navController.graph.id) { inclusive = true } })
        // Just signed in with no shop chosen (a new account, or after signing out): open the picker,
        // as the tour does, rather than a Home that can only say "choose a shop". Back returns to Home.
        if (target == R.id.dashboardFragment && appContainer.activeStore.state.value is Selection.None) {
            navController.navigate(R.id.workspacePickerFragment)
        }
    }
}
