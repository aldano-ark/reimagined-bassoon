package com.example.nativetemplate.designsystem.previews

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nativetemplate.designsystem.components.DSButton
import com.example.nativetemplate.designsystem.components.DSButtonIntent
import com.example.nativetemplate.designsystem.components.DSStatusKind
import com.example.nativetemplate.designsystem.components.DSStatusView
import com.example.nativetemplate.designsystem.components.DSTextField
import com.example.nativetemplate.designsystem.theme.DSSpacing
import com.example.nativetemplate.designsystem.theme.DSTheme

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 900)
@Preview(name = "Dark", showBackground = true, widthDp = 360, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", showBackground = true, widthDp = 320, heightDp = 1200, fontScale = 2f)
@Preview(name = "RTL", showBackground = true, widthDp = 360, heightDp = 900, locale = "ar")
@Composable
private fun DesignSystemGallery() {
    var value by remember { mutableStateOf("Example") }
    DSTheme(dynamicColor = false) {
        Surface {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(DSSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DSSpacing.md),
            ) {
                Text("Buttons", style = MaterialTheme.typography.headlineSmall)
                DSButton("Primary action", {})
                DSButton("Secondary action", {}, intent = DSButtonIntent.Secondary)
                DSButton("Destructive action", {}, intent = DSButtonIntent.Destructive)
                DSButton("Disabled action", {}, enabled = false)
                DSButton("Loading action", {}, loading = true)
                Text("Fields", style = MaterialTheme.typography.headlineSmall)
                DSTextField(value, { value = it }, "Label", supportingText = "Supporting text")
                DSTextField("", {}, "Required field", supportingText = "Replaced by error", errorText = "Enter a value")
                DSTextField("Unavailable", {}, "Disabled field", enabled = false)
                DSStatusView("No content", description = "Nothing to display yet")
                DSStatusView("Could not load", description = "Try again", kind = DSStatusKind.Error) {
                    DSButton("Retry", {}, intent = DSButtonIntent.Secondary)
                }
            }
        }
    }
}
