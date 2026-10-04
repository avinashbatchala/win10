package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val iconCache = mutableMapOf<String, ImageBitmap?>()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            reloadApps()
        }
    }

    init {
        registerPackageReceiver()
        reloadApps()
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            context.registerReceiver(packageReceiver, filter)
        } catch (_: Exception) {}
    }

    fun reloadApps() {
        scope.launch(Dispatchers.IO) {
            val apps = queryInstalledApps()
            _installedApps.value = apps
            _isLoaded.value = true
        }
    }

    private suspend fun queryInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val ownPackage = context.packageName

        val appList = mutableListOf<AppInfo>()
        for (ri in resolveInfos) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ownPackage) continue

            val activityName = ri.activityInfo.name
            val label = ri.loadLabel(pm)?.toString() ?: pkg
            val key = "$pkg/$activityName"

            val iconBitmap = iconCache.getOrPut(key) {
                try {
                    val drawable = ri.loadIcon(pm)
                    drawableToImageBitmap(drawable)
                } catch (_: Exception) {
                    null
                }
            }

            val firstRawChar = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
            val firstLetter = if (firstRawChar in 'A'..'Z') firstRawChar else '#'

            appList.add(
                AppInfo(
                    packageName = pkg,
                    activityName = activityName,
                    label = label,
                    iconBitmap = iconBitmap,
                    firstLetter = firstLetter
                )
            )
        }

        appList.sortWith(compareBy({ it.firstLetter == '#' }, { it.label.lowercase() }))
        appList
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            // Return scaled copy if too large, or asImageBitmap directly
            if (bmp.width <= 144 && bmp.height <= 144) {
                return bmp.asImageBitmap()
            }
            val scaled = Bitmap.createScaledBitmap(bmp, 128, 128, true)
            return scaled.asImageBitmap()
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 128
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 128
        val bitmap = Bitmap.createBitmap(width.coerceIn(48, 144), height.coerceIn(48, 144), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    fun getAppIcon(packageName: String): ImageBitmap? {
        val entry = iconCache.entries.firstOrNull { it.key.startsWith("$packageName/") }
        return entry?.value
    }
}
