package com.maskan.mobileapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maskan.mobileapp.data.prefs.AppTheme
import com.maskan.mobileapp.data.repository.UpdateStatus
import com.maskan.mobileapp.di.AppContainer
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
                MaskanApp(container)
            }
        }
    }
}

/** Theme = "maskan.appTheme" (System/Light/Dark), shared between landlord and tenant (08-landlord-settings.md). */
@Composable
private fun MaskanApp(container: AppContainer) {
    val theme by container.themePreferences.themeFlow.collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)
    val darkTheme = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaskanTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            MaskanNavHost()
        }
        UpdateCheck(container)
    }
}

/** Checks Remote Config once per process launch; non-dismissable when below the min supported version. */
@Composable
private fun UpdateCheck(container: AppContainer) {
    val context = LocalContext.current
    var updateStatus by remember { mutableStateOf<UpdateStatus?>(null) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateStatus = container.updateRepository.checkForUpdate()?.takeIf { it.isUpdateAvailable }
    }

    val status = updateStatus ?: return
    if (dismissed && !status.isUpdateRequired) return

    AlertDialog(
        onDismissRequest = { if (!status.isUpdateRequired) dismissed = true },
        title = { Text(if (status.isUpdateRequired) "Update Required" else "Update Available") },
        text = {
            Text(
                if (status.isUpdateRequired) {
                    "A newer version of Maskan is required to continue. Please update to keep using the app."
                } else {
                    "A newer version of Maskan is available with the latest features and fixes."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(status.updateUrl)))
                }.onFailure {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")),
                        )
                    }
                }
            }) { Text("Update Now") }
        },
        dismissButton = if (status.isUpdateRequired) null else {
            { TextButton(onClick = { dismissed = true }) { Text("Later") } }
        },
    )
}
