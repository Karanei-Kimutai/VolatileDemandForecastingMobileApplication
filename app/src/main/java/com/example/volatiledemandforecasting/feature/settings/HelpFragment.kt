package com.example.volatiledemandforecasting.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.IconBadge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.Term
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.feature.onboarding.ARG_TOUR_REPLAY
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController

/** Getting started, the app tour again, and plain explanations of how forecasts work and what the app does with data (spec §8.12). */
class HelpFragment : ShellFragment() {
    override val titleRes = R.string.screen_help

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
        ) {
            HelpCard(Icons.Outlined.Flag, R.string.help_start_title, R.string.help_start_body) {
                SecondaryButton(
                    stringResource(R.string.help_show_tour),
                    { navController.navigate(R.id.onboardingFragment, bundleOf(ARG_TOUR_REPLAY to true)) },
                    Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    icon = Icons.Outlined.PlayCircle,
                )
            }
            listOf(
                Triple(Icons.Outlined.Description, R.string.help_file_title, R.string.help_file_body),
                Triple(Icons.Outlined.Insights, R.string.help_forecasts_title, R.string.help_forecasts_body),
                Triple(Icons.Outlined.AutoAwesome, R.string.help_ai_title, R.string.help_ai_body),
                Triple(Icons.Outlined.Info, R.string.help_limits_title, R.string.help_limits_body),
                Triple(Icons.Outlined.Lock, R.string.help_privacy_title, R.string.help_privacy_body),
            ).forEach { (icon, title, body) -> HelpCard(icon, title, body) }
            // The same definitions the ⓘ buttons show, gathered in one place.
            AppCard {
                Text(
                    stringResource(R.string.help_terms_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Term.entries.forEachIndexed { index, term ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
                    else Spacer(Modifier.height(Spacing.sm))
                    Text(stringResource(term.title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(term.body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun HelpCard(icon: ImageVector, title: Int, body: Int, extra: @Composable () -> Unit = {}) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            IconBadge(icon)
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        }
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.md),
        )
        extra()
    }
}
