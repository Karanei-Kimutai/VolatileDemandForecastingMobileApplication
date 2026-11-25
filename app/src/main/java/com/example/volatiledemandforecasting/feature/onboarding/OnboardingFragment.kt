package com.example.volatiledemandforecasting.feature.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ComposeFragment
import com.example.volatiledemandforecasting.app.appContainer
import com.example.volatiledemandforecasting.core.security.SessionState
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.TertiaryButton
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.workspace.Selection
import kotlinx.coroutines.launch

const val ARG_TOUR_REPLAY = "tourReplay"

private data class TourPage(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    TourPage(Icons.Outlined.Storefront, R.string.tour_1_title, R.string.tour_1_body),
    TourPage(Icons.Outlined.UploadFile, R.string.tour_2_title, R.string.tour_2_body),
    TourPage(Icons.Outlined.Insights, R.string.tour_3_title, R.string.tour_3_body),
    TourPage(Icons.Outlined.AutoAwesome, R.string.tour_4_title, R.string.tour_4_body),
)

/**
 * The intro tour: a few swipeable pages on what the app does and the three steps to a forecast.
 * Shown once on first launch (before or after signing in), and again from Help on request.
 */
class OnboardingFragment : ComposeFragment() {

    private val replay get() = arguments?.getBoolean(ARG_TOUR_REPLAY) == true

    @Composable
    override fun Content() {
        val signedIn = appContainer.session.state.collectAsStateWithLifecycle().value is SessionState.SignedIn
        Tour(
            signedIn = signedIn,
            replay = replay,
            onFinish = { next -> finish(next) },
        )
    }

    /** [next] is where a signed-out user goes: the register or sign-in screen. Ignored when signed in. */
    private fun finish(next: Int) {
        val navController = findNavController()
        lifecycleScope.launch {
            appContainer.preferences.setTourSeen()
            val signedIn = appContainer.session.state.value is SessionState.SignedIn
            if (replay && signedIn) {
                navController.popBackStack()
                return@launch
            }
            val clear = navOptions { popUpTo(navController.graph.id) { inclusive = true } }
            if (!signedIn) {
                navController.navigate(next, null, clear)
                return@launch
            }
            navController.navigate(R.id.dashboardFragment, null, clear)
            // No shop yet: go straight to "set up your shop or try the sample". Back returns to the dashboard.
            if (appContainer.activeStore.state.value !is Selection.Selected) {
                navController.navigate(R.id.workspacePickerFragment)
            }
        }
    }
}

@Composable
private fun Tour(signedIn: Boolean, replay: Boolean, onFinish: (next: Int) -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val onLast = pagerState.currentPage == pages.lastIndex

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.lg)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // Signed in, Skip ends the tour. Signed out, it jumps to the last page, which holds the account choice.
                if (!onLast) {
                    TertiaryButton(
                        stringResource(R.string.tour_skip),
                        {
                            if (signedIn) onFinish(R.id.dashboardFragment)
                            else scope.launch { pagerState.animateScrollToPage(pages.lastIndex) }
                        },
                    )
                } else {
                    Spacer(Modifier.height(Sizes.touchTarget))
                }
            }
            HorizontalPager(pagerState, Modifier.weight(1f).fillMaxWidth()) { index ->
                PageContent(pages[index])
            }
            PageDots(current = pagerState.currentPage, count = pages.size)
            Spacer(Modifier.height(Spacing.xl))
            when {
                !onLast -> PrimaryButton(
                    stringResource(R.string.tour_next),
                    { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    Modifier.fillMaxWidth(),
                )
                signedIn -> PrimaryButton(
                    stringResource(if (replay) R.string.tour_done else R.string.tour_get_started),
                    { onFinish(R.id.dashboardFragment) },
                    Modifier.fillMaxWidth(),
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PrimaryButton(stringResource(R.string.tour_create_account), { onFinish(R.id.registerFragment) }, Modifier.fillMaxWidth())
                    SecondaryButton(stringResource(R.string.tour_have_account), { onFinish(R.id.loginFragment) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: TourPage) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The same rounded-square badge as list rows and cards, at hero size.
        Box(
            Modifier.size(104.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(Spacing.xxl))
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageDots(current: Int, count: Int) {
    val description = stringResource(R.string.tour_page_of, current + 1, count)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        // The current page is a longer pill, so position doesn't rely on colour alone.
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.height(8.dp).width(width).background(color, CircleShape))
        }
    }
}
