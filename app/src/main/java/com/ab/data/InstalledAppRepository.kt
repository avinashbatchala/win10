package com.ab.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ab.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class InstalledAppRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    private val launcherApps: LauncherApps? =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    private val launcherAppsCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            reloadApps()
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            invalidatePackage(packageName)
            reloadApps()
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            invalidatePackage(packageName)
            reloadApps()
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            reloadApps()
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            reloadApps()
        }
    }

    private val packageBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart
            if (!pkg.isNullOrEmpty()) {
                invalidatePackage(pkg)
            }
            reloadApps()
        }
    }

    init {
        registerCallbacks()
        reloadApps()
    }

    private fun registerCallbacks() {
        try {
            launcherApps?.registerCallback(launcherAppsCallback)
        } catch (_: Exception) {}

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(packageBroadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(packageBroadcastReceiver, filter)
            }
        } catch (_: Exception) {
            try {
                context.registerReceiver(packageBroadcastReceiver, filter)
            } catch (_: Exception) {}
        }
    }

    private fun invalidatePackage(packageName: String) {
        val keysToRemove = iconCache.keys.filter { it.startsWith("$packageName/") }
        keysToRemove.forEach { iconCache.remove(it) }
    }

    fun reloadApps() {
        scope.launch(Dispatchers.IO) {
            val apps = queryAllLaunchableApps()
            _installedApps.value = apps
            _isLoaded.value = true
        }
    }

    private suspend fun queryAllLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val appList = mutableListOf<AppInfo>()
        val ownPackage = context.packageName

        // First attempt via LauncherApps API for multi-user / modern launcher compliance
        if (launcherApps != null) {
            try {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                val profiles = userManager?.userProfiles ?: listOf(Process.myUserHandle())

                for (user in profiles) {
                    val activityList: List<LauncherActivityInfo> = launcherApps.getActivityList(null, user)
                    for (info in activityList) {
                        val pkg = info.applicationInfo.packageName
                        if (pkg == ownPackage) continue

                        val component = info.componentName
                        val activityName = component.className
                        val componentKey = "${pkg}/$activityName"
                        val label = info.label?.toString() ?: pkg

                        val iconBitmap = iconCache.getOrPut(componentKey) {
                            val drawable = info.getIcon(context.resources.displayMetrics.densityDpi)
                            drawableToImageBitmap(drawable)
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
                }
            } catch (_: Exception) {
                // Fall back to standard PackageManager if LauncherApps throws
            }
        }

        // If LauncherApps returned empty (or failed), query via PackageManager
        if (appList.isEmpty()) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ownPackage) continue

                val activityName = ri.activityInfo.name
                val componentKey = "$pkg/$activityName"
                val label = ri.loadLabel(pm)?.toString() ?: pkg

                val iconBitmap = iconCache.getOrPut(componentKey) {
                    try {
                        val drawable = ri.loadIcon(pm)
                        drawableToImageBitmap(drawable)
                    } catch (_: Exception) {
                        fallbackIcon()
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
        }

        // Deduplicate by componentKey and sort case-insensitively
        val distinctApps = appList.distinctBy { it.key }
        distinctApps.sortedWith(
            compareBy<AppInfo> { it.firstLetter == '#' }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        )
    }

    private fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap {
        if (drawable == null) return fallbackIcon()

        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val bmp = drawable.bitmap
            if (bmp.width in 48..144 && bmp.height in 48..144) {
                return bmp.asImageBitmap()
            }
            val targetSize = 96
            val scaled = Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)
            return scaled.asImageBitmap()
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(48, 144) else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(48, 144) else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    private fun fallbackIcon(): ImageBitmap {
        val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.DKGRAY)
        return bitmap.asImageBitmap()
    }

    fun getAppIcon(packageName: String, activityName: String? = null): ImageBitmap? {
        if (!activityName.isNullOrEmpty()) {
            val key = "$packageName/$activityName"
            iconCache[key]?.let { return it }
        }
        val entry = iconCache.entries.firstOrNull { it.key.startsWith("$packageName/") }
        return entry?.value
    }

    fun isAppInstalled(packageName: String): Boolean {
        return _installedApps.value.any { it.packageName == packageName }
    }
}
