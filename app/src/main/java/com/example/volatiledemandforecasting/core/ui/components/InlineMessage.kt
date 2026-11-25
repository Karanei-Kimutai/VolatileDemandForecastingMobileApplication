package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

/**
 * A message inside a screen or form: an error, a notice, or a confirmation. It always has an
 * icon as well as a colour, and screen readers announce it. An [action] goes underneath.
 */
@Composable
fun InlineMessage(
    text: String,
    kind: StatusKind,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector = kind.icon,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    announce: Boolean = true,
) {
    val (container, content) = statusColors(kind)
    Surface(
        modifier.fillMaxWidth().then(
            if (announce) Modifier.semantics { liveRegion = LiveRegionMode.Assertive } else Modifier
        ),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(top = Spacing.xxs).size(Sizes.iconMedium))
            Column(Modifier.weight(1f)) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodyMedium)
                if (action != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        modifier = Modifier.heightIn(min = Sizes.touchTarget).align(Alignment.Start).offset(x = -Spacing.sm),
                        colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
                        contentPadding = PaddingValues(horizontal = Spacing.sm),
                    ) { Text(action, style = MaterialTheme.typography.labelLarge, textDecoration = TextDecoration.Underline) }
                }
            }
        }
    }
}
