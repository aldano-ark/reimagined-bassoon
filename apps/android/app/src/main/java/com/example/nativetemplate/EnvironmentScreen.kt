package com.example.nativetemplate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.nativetemplate.configuration.AppConfig
import com.example.nativetemplate.designsystem.theme.DSSpacing
import com.example.nativetemplate.network.ApiRequest

@Composable
internal fun EnvironmentScreen(config: AppConfig, request: ApiRequest) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(DSSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DSSpacing.lg),
    ) {
        Text(config.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.environment_label, config.environment.value))
        Text(stringResource(R.string.api_base_url_label, config.apiBaseUrl.toString()))
        Text(stringResource(R.string.request_label, request.method, request.uri.toString()))
    }
}
