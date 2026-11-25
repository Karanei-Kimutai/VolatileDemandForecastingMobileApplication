package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors

/** What the shop switcher shows. `null` means no shop has been chosen yet. */
data class WorkspaceChipUi(val label: String, val isDemo: Boolean)

/**
 * The shop the user is working in, shown under the screen title so it costs no extra row
 * (spec §5.1). It marks the sample shop, and tapping it opens the shop picker.
 */
@Composable
fun ShopSwitcher(workspace: WorkspaceChipUi?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The backend names it after the M5 store; the app calls it the sample shop everywhere.
    val label = when {
        workspace == null -> stringResource(R.string.workspace_none)
        workspace.isDemo -> stringResource(R.string.sample_shop_name)
        else -> workspace.label
    }
    val description = if (workspace?.isDemo == true) {
        stringResource(R.string.shop_switcher_description_sample, label)
    } else {
        stringResource(R.string.workspace_chip_description, label)
    }
    Row(
        modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            }
            .padding(vertical = Spacing.xxs, horizontal = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            Icons.Outlined.Storefront,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (workspace?.isDemo == true) {
            Text(
                stringResource(R.string.demo_data_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.appColors.onDemoBadge,
                modifier = Modifier
                    .background(MaterialTheme.appColors.demoBadge, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = Spacing.xs, vertical = 1.dp),
            )
        }
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
