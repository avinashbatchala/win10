package com.ab.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.ab.model.IconRenderMode
import com.ab.model.ResolvedLauncherIcon
import com.ab.ui.icons.MetroIcons
import java.util.concurrent.ConcurrentHashMap

class LauncherIconRepository(private val context: Context) {

    companion object {
        private const val TAG = "MetroIconRepo"
    }

    private val pm: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, ResolvedLauncherIcon>()

    /**
     * Resolves the icon for the specified application or tile.
     * Follows the strict resolution priority:
     * 1. Explicit launcher-provided Metro glyph override (or manual override)
     * 2. Android app-provided monochrome adaptive-icon layer (API 33+)
     * 3. Original Android application icon (preserving branding without circular Pixel masks)
     */
    fun resolveIcon(
        packageName: String,
        activityName: String? = null,
        targetSizePx: Int = 144,
        iconModeOverride: IconRenderMode? = null,
        customIconId: String? = null
    ): ResolvedLauncherIcon {
        val cacheKey = "$packageName/${activityName ?: ""}/$targetSizePx/${iconModeOverride?.name ?: "AUTO"}/$customIconId"
        iconCache[cacheKey]?.let { return it }

        val resolved = computeIcon(packageName, activityName, targetSizePx, iconModeOverride, customIconId)
        iconCache[cacheKey] = resolved
        return resolved
    }

    private fun computeIcon(
        packageName: String,
        activityName: String?,
        targetSizePx: Int,
        iconModeOverride: IconRenderMode?,
        customIconId: String?
    ): ResolvedLauncherIcon {
        // Priority 1: Explicit custom icon ID if provided
        if (!customIconId.isNullOrEmpty()) {
            try {
                val semantic = MetroIconOverrides.SemanticIcon.valueOf(customIconId)
                return ResolvedLauncherIcon.VectorGlyph(
                    imageVector = MetroIconOverrides.getVector(semantic),
                    semanticId = customIconId
                )
            } catch (_: Exception) {}
        }

        // Priority 1: Match with Metro semantic glyph overrides. Skipped when the user
        // asked for the original icon or for monochrome-only appearance.
        if (iconModeOverride != IconRenderMode.ANDROID_ORIGINAL &&
            iconModeOverride != IconRenderMode.ANDROID_MONOCHROME
        ) {
            val override = MetroIconOverrides.findOverride(packageName, activityName)
            if (override != null) {
                return ResolvedLauncherIcon.VectorGlyph(
                    imageVector = MetroIconOverrides.getVector(override),
                    semanticId = override.name
                )
            }
        }

        // Load the actual Android app drawable
        val appDrawable = loadRawAppDrawable(packageName, activityName) ?: run {
            return ResolvedLauncherIcon.VectorGlyph(
                imageVector = MetroIcons.GenericApp,
                semanticId = "GENERIC_FALLBACK"
            )
        }

        // Priority 2: Android app-provided monochrome adaptive-icon layer (API 33+)
        if (iconModeOverride != IconRenderMode.ANDROID_ORIGINAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (appDrawable is AdaptiveIconDrawable) {
                try {
                    val monoDrawable = appDrawable.monochrome
                    if (monoDrawable != null) {
                        val monoBitmap = drawableToBitmap(monoDrawable, targetSizePx, targetSizePx)
                        if (monoBitmap != null) {
                            return ResolvedLauncherIcon.MonochromeBitmap(monoBitmap.asImageBitmap())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error extracting monochrome layer for $packageName", e)
                }
            }
        }

        // Priority 3: Original Android application icon (preserving original colors & transparency)
        val originalBitmap = drawableToBitmap(appDrawable, targetSizePx, targetSizePx)
        if (originalBitmap != null) {
            return ResolvedLauncherIcon.OriginalBitmap(originalBitmap.asImageBitmap())
        }

        // Fallback: Generic Windows Metro app glyph
        return ResolvedLauncherIcon.VectorGlyph(
            imageVector = MetroIcons.GenericApp,
            semanticId = "GENERIC_FALLBACK"
        )
    }

    private fun loadRawAppDrawable(packageName: String, activityName: String?): Drawable? {
        if (!activityName.isNullOrEmpty()) {
            try {
                val cn = ComponentName(packageName, activityName)
                return pm.getActivityIcon(cn)
            } catch (_: Exception) {}
        }
        return try {
            pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val b = drawable.bitmap
            if (b.width == width && b.height == height) return b
            return Bitmap.createScaledBitmap(b, width, height, true)
        }

        return try {
            val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else width
            val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            if (w != width || h != height) {
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to rasterize drawable to bitmap", e)
            null
        }
    }

    fun invalidatePackage(packageName: String) {
        val keysToRemove = iconCache.keys.filter { it.startsWith("$packageName/") }
        keysToRemove.forEach { iconCache.remove(it) }
        Log.d(TAG, "Invalidated ${keysToRemove.size} icon cache entries for $packageName")
    }

    fun clearCache() {
        iconCache.clear()
    }
}
