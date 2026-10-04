package com.ab.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
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

    // Cached brand colours for non-system apps (0L = "no usable colour").
    private val brandColorCache = ConcurrentHashMap<String, Long>()

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
                    semanticId = customIconId,
                    brandColor = resolveBrandColor(packageName, activityName)
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
                    semanticId = override.name,
                    // Third-party apps keep their own brand colour behind the Metro glyph;
                    // system apps use the accent (brandColor == null).
                    brandColor = resolveBrandColor(packageName, activityName)
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
                            return ResolvedLauncherIcon.MonochromeBitmap(
                                bitmap = monoBitmap.asImageBitmap(),
                                brandColor = resolveBrandColor(packageName, activityName)
                            )
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
            return ResolvedLauncherIcon.OriginalBitmap(
                bitmap = originalBitmap.asImageBitmap(),
                brandColor = if (isSystemApp(packageName)) null else extractBrandColor(originalBitmap)
            )
        }

        // Fallback: Generic Windows Metro app glyph
        return ResolvedLauncherIcon.VectorGlyph(
            imageVector = MetroIcons.GenericApp,
            semanticId = "GENERIC_FALLBACK"
        )
    }

    /**
     * Picks a representative brand colour from an app icon so third-party tiles can use the
     * app's own colour (Windows 10 Mobile behaviour) instead of a single accent everywhere.
     *
     * Samples a small grid, ignores transparent/near-grey/near-black/near-white pixels and
     * returns the average of the most common vivid colour bucket, or null if none is found.
     */
    private fun extractBrandColor(bitmap: Bitmap): Long? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val stepX = (width / 32).coerceAtLeast(1)
        val stepY = (height / 32).coerceAtLeast(1)
        val hsv = FloatArray(3)
        // bucket key -> [count, sumR, sumG, sumB]
        val buckets = HashMap<Int, IntArray>()

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha >= 128) {
                    android.graphics.Color.colorToHSV(pixel, hsv)
                    val saturation = hsv[1]
                    val value = hsv[2]
                    // Keep only reasonably vivid, mid-brightness colours.
                    if (saturation >= 0.25f && value in 0.15f..0.97f) {
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                        val acc = buckets.getOrPut(key) { intArrayOf(0, 0, 0, 0) }
                        acc[0]++
                        acc[1] += r
                        acc[2] += g
                        acc[3] += b
                    }
                }
                x += stepX
            }
            y += stepY
        }

        val best = buckets.maxByOrNull { it.value[0] } ?: return null
        val count = best.value[0]
        if (count <= 0) return null
        val r = best.value[1] / count
        val g = best.value[2] / count
        val b = best.value[3] / count
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    /**
     * Returns the brand colour for a non-system app, extracting it from its launcher icon.
     * System/first-party apps return null so they keep the theme accent (Windows 10 Mobile style).
     */
    private fun resolveBrandColor(packageName: String, activityName: String?): Long? {
        val key = "$packageName/${activityName ?: ""}"
        val cached = brandColorCache[key]
        if (cached != null) return if (cached == 0L) null else cached

        val color = if (isSystemApp(packageName)) {
            null
        } else {
            // Use the application's main icon (what the user recognises) rather than the
            // launcher activity icon, which is often a generic/inherited Chromium icon.
            // Try the full icon first; if it has no usable colour (e.g. a white logo),
            // fall back to the adaptive background layer, which often carries the brand.
            runCatching { pm.getApplicationIcon(packageName) }.getOrNull()?.let { appIcon ->
                val fromIcon = drawableToBitmap(appIcon, 64, 64)?.let { extractBrandColor(it) }
                fromIcon ?: (appIcon as? AdaptiveIconDrawable)?.background?.let { bg ->
                    drawableToBitmap(bg, 64, 64)?.let { extractBrandColor(it) }
                }
            }
        }
        brandColorCache[key] = color ?: 0L
        return color
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        } catch (_: Exception) {
            false
        }
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
        val brandKeysToRemove = brandColorCache.keys.filter { it.startsWith("$packageName/") }
        brandKeysToRemove.forEach { brandColorCache.remove(it) }
        Log.d(TAG, "Invalidated ${keysToRemove.size} icon cache entries for $packageName")
    }

    fun clearCache() {
        iconCache.clear()
        brandColorCache.clear()
    }
}
