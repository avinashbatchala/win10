package com.ab

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.ab.model.LauncherOrientation
import com.ab.ui.screens.MainLauncherScreen
import com.ab.ui.theme.MetroTheme
import com.ab.ui.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG_ACTIVITY = "LauncherActivity"
        private const val TAG_BACK = "LauncherBack"
    }

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG_ACTIVITY, "onCreate: LauncherActivity initialized (instance: ${hashCode()})")
        enableEdgeToEdge()

        // Root fallback callback ensuring the activity NEVER finishes on Back gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG_BACK, "Root Activity OnBackPressedCallback invoked: consuming Back and remaining idle.")
                viewModel.handleInternalBack()
            }
        })

        setContent {
            val settings by viewModel.settings.collectAsState()
            val accentColor = Color(settings.accentColor)

            LaunchedEffect(settings.launcherOrientation) {
                requestedOrientation = when (settings.launcherOrientation) {
                    LauncherOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    LauncherOrientation.FOLLOW_SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            val displayScale = settings.displayScale.resolve(screenWidthDp)

            MetroTheme(
                accentColor = accentColor,
                darkTheme = settings.darkTheme,
                displayScale = displayScale
            ) {
                MainLauncherScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d(TAG_ACTIVITY, "onNewIntent received: action=${intent.action}, categories=${intent.categories}")
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            viewModel.resetToStartRoot()
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG_ACTIVITY, "onResume: LauncherActivity resumed")
        viewModel.checkDefaultLauncherStatus()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onStop()
    }
}
