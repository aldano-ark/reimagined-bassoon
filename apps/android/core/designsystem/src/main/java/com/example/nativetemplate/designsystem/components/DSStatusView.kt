package com.example.nativetemplate.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.nativetemplate.designsystem.theme.DSSpacing

enum class DSStatusKind { Neutral, Error }

/** Describes a state; optional actions and their policy remain with the caller. */
@Composable
fun DSStatusView(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    kind: DSStatusKind = DSStatusKind.Neutral,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.padding(DSSpacing.lg), verticalArrangement = Arrangement.spacedBy(DSSpacing.sm)) {
        Text(
            title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = if (kind == DSStatusKind.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}
