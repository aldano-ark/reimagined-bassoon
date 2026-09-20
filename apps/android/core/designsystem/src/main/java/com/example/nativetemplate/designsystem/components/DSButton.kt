package com.example.nativetemplate.designsystem.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.nativetemplate.designsystem.R
import com.example.nativetemplate.designsystem.theme.DSSpacing

enum class DSButtonIntent { Primary, Secondary, Destructive }

/** An action whose work and state are owned by the caller. Loading prevents activation. */
@Composable
fun DSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intent: DSButtonIntent = DSButtonIntent.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val loadingDescription = stringResource(R.string.design_system_loading)
    val buttonModifier = modifier.semantics {
        if (loading) stateDescription = loadingDescription
    }
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp).clearAndSetSemantics {},
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(DSSpacing.sm))
        }
        Text(text)
    }
    when (intent) {
        DSButtonIntent.Primary -> Button(
            onClick = onClick, modifier = buttonModifier, enabled = enabled && !loading, content = content,
        )
        DSButtonIntent.Secondary -> OutlinedButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled && !loading, content = content,
        )
        DSButtonIntent.Destructive -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            content = content,
        )
    }
}
