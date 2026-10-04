package com.ab.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import com.ab.model.AppInfo
import com.ab.model.IconRenderMode
import com.ab.model.ResolvedLauncherIcon
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
    companion object {
        private const val TAG = "LauncherPackage"
    }

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val iconRepository = LauncherIconRepository(context)

    private val launcherApps: LauncherApps? =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    private val launcherAppsCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                Log.d(TAG, "LauncherApps callback: onPackageRemoved for $packageName")
                invalidatePackage(packageName)
                reloadApps()
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                Log.d(TAG, "LauncherApps callback: onPackageAdded for $packageName")
                invalidatePackage(packageName)
                reloadApps()
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                Log.d(TAG, "LauncherApps callback: onPackageChanged for $packageName")
                invalidatePackage(packageName)
                reloadApps()
            }

            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                Log.d(TAG, "LauncherApps callback: onPackagesAvailable (${packageNames.joinToString()})")
                packageNames.forEach { invalidatePackage(it) }
                reloadApps()
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
                Log.d(TAG, "LauncherApps callback: onPackagesUnavailable (${packageNames.joinToString()})")
                reloadApps()
            }
        }
    } else null

    private val packageBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val uri: Uri? = intent.data
            val packageName = uri?.schemeSpecificPart ?: return

            Log.d(TAG, "Broadcast received: $action for package: $packageName")
            when (action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_CHANGED -> {
                    invalidatePackage(packageName)
                    reloadApps()
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (!replacing) {
                        Log.d(TAG, "Permanent package removal confirmed for: $packageName")
                        invalidatePackage(packageName)
                        reloadApps()
                    }
                }
            }
        }
    }

    init {
        registerReceivers()
        reloadApps()
    }

    private fun registerReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && launcherApps != null && launcherAppsCallback != null) {
            try {
                launcherApps.registerCallback(launcherAppsCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register LauncherApps callback", e)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
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
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register package broadcast receiver", e)
            }
        }
    }

    private fun invalidatePackage(packageName: String) {
        iconRepository.invalidatePackage(packageName)
    }

    fun reloadApps() {
        scope.launch(Dispatchers.IO) {
            val apps = queryAllLaunchableApps()
            _installedApps.value = apps
            _isLoaded.value = true
            Log.d(TAG, "Apps list updated: ${apps.size} launchable activities found.")
        }
    }

    private fun checkCanUninstall(pkg: String, appInfoFlags: Int): Boolean {
        if (pkg == context.packageName) return false
        val isSystem = (appInfoFlags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (appInfoFlags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        // Non-system apps or updated system apps can normally be uninstalled
        if (isSystem && !isUpdatedSystem) return false
        return true
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
                        val label = info.label?.toString() ?: pkg

                        val resolvedIcon = iconRepository.resolveIcon(pkg, activityName)
                        val iconBitmap = (resolvedIcon as? ResolvedLauncherIcon.OriginalBitmap)?.bitmap
                            ?: (resolvedIcon as? ResolvedLauncherIcon.MonochromeBitmap)?.bitmap

                        val firstRawChar = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
                        val firstLetter = if (firstRawChar in 'A'..'Z') firstRawChar else '#'
                        val canUninstall = checkCanUninstall(pkg, info.applicationInfo.flags)

                        appList.add(
                            AppInfo(
                                packageName = pkg,
                                activityName = activityName,
                                label = label,
                                iconBitmap = iconBitmap,
                                firstLetter = firstLetter,
                                canUninstall = canUninstall,
                                resolvedIcon = resolvedIcon
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error querying LauncherApps", e)
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
                val label = ri.loadLabel(pm)?.toString() ?: pkg

                val resolvedIcon = iconRepository.resolveIcon(pkg, activityName)
                val iconBitmap = (resolvedIcon as? ResolvedLauncherIcon.OriginalBitmap)?.bitmap
                    ?: (resolvedIcon as? ResolvedLauncherIcon.MonochromeBitmap)?.bitmap

                val firstRawChar = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
                val firstLetter = if (firstRawChar in 'A'..'Z') firstRawChar else '#'
                val canUninstall = checkCanUninstall(pkg, ri.activityInfo.applicationInfo.flags)

                appList.add(
                    AppInfo(
                        packageName = pkg,
                        activityName = activityName,
                        label = label,
                        iconBitmap = iconBitmap,
                        firstLetter = firstLetter,
                        canUninstall = canUninstall,
                        resolvedIcon = resolvedIcon
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

    fun getAppIcon(packageName: String, activityName: String? = null): ImageBitmap? {
        val resolved = iconRepository.resolveIcon(packageName, activityName)
        return (resolved as? ResolvedLauncherIcon.OriginalBitmap)?.bitmap
            ?: (resolved as? ResolvedLauncherIcon.MonochromeBitmap)?.bitmap
    }

    fun resolveLauncherIcon(
        packageName: String,
        activityName: String? = null,
        targetSizePx: Int = 144,
        iconModeOverride: IconRenderMode? = null,
        customIconId: String? = null
    ): ResolvedLauncherIcon {
        return iconRepository.resolveIcon(packageName, activityName, targetSizePx, iconModeOverride, customIconId)
    }

    fun isAppInstalled(packageName: String): Boolean {
        return _installedApps.value.any { it.packageName == packageName }
    }
}
