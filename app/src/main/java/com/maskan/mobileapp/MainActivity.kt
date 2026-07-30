package com.maskan.mobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.maskan.mobileapp.di.LocalAppContainer
import com.maskan.mobileapp.ui.nav.MaskanNavHost
import com.maskan.mobileapp.ui.theme.MaskanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MaskanApplication).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                MaskanTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MaskanNavHost()
                    }
                }
            }
        }
    }
}
