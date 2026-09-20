package com.example.nativetemplate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.nativetemplate.configuration.AppConfig
import com.example.nativetemplate.designsystem.theme.DSTheme
import com.example.nativetemplate.network.ApiRequestFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val config = AppConfig.fromBuildConfig()
        val request = ApiRequestFactory(config.apiBaseUrl).healthRequest()
        setContent {
            DSTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EnvironmentScreen(config, request)
                }
            }
        }
    }
}
