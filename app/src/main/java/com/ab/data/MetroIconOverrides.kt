package com.ab.data

import androidx.compose.ui.graphics.vector.ImageVector
import com.ab.ui.icons.MetroIcons

object MetroIconOverrides {

    enum class SemanticIcon {
        PHONE,
        MESSAGING,
        PEOPLE,
        CALENDAR,
        CAMERA,
        PHOTOS,
        SETTINGS,
        CALCULATOR,
        CLOCK,
        MAIL,
        BROWSER,
        MAPS,
        STORE,
        FILES,
        MUSIC,
        VIDEO
    }

    fun getVector(semantic: SemanticIcon): ImageVector {
        return when (semantic) {
            SemanticIcon.PHONE -> MetroIcons.Phone
            SemanticIcon.MESSAGING -> MetroIcons.Messaging
            SemanticIcon.PEOPLE -> MetroIcons.People
            SemanticIcon.CALENDAR -> MetroIcons.Calendar
            SemanticIcon.CAMERA -> MetroIcons.Camera
            SemanticIcon.PHOTOS -> MetroIcons.Photos
            SemanticIcon.SETTINGS -> MetroIcons.Settings
            SemanticIcon.CALCULATOR -> MetroIcons.Calculator
            SemanticIcon.CLOCK -> MetroIcons.Clock
            SemanticIcon.MAIL -> MetroIcons.Mail
            SemanticIcon.BROWSER -> MetroIcons.Browser
            SemanticIcon.MAPS -> MetroIcons.Maps
            SemanticIcon.STORE -> MetroIcons.Store
            SemanticIcon.FILES -> MetroIcons.Files
            SemanticIcon.MUSIC -> MetroIcons.Music
            SemanticIcon.VIDEO -> MetroIcons.Video
        }
    }

    fun findOverride(packageName: String, activityName: String? = null): SemanticIcon? {
        val pkg = packageName.lowercase()
        val act = activityName?.lowercase() ?: ""

        // 1. Phone / Dialer
        if (pkg.contains("dialer") || pkg.contains("telecom") || pkg.contains("phone") ||
            act.contains("dialer") || act.contains("telecom") || act.contains("phone")) {
            return SemanticIcon.PHONE
        }

        // 2. Messaging / SMS
        if (pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") ||
            act.contains("conversation") || act.contains("mms") || act.contains("sms")) {
            return SemanticIcon.MESSAGING
        }

        // 3. Contacts / People
        if (pkg.contains("contacts") || pkg.contains("people") ||
            act.contains("contacts") || act.contains("people")) {
            return SemanticIcon.PEOPLE
        }

        // 4. Calendar
        if (pkg.contains("calendar") || act.contains("calendar")) {
            return SemanticIcon.CALENDAR
        }

        // 5. Camera
        if (pkg.contains("camera") || act.contains("camera")) {
            return SemanticIcon.CAMERA
        }

        // 6. Photos / Gallery
        if (pkg.contains("photos") || pkg.contains("gallery") ||
            act.contains("photos") || act.contains("gallery")) {
            return SemanticIcon.PHOTOS
        }

        // 7. Settings
        if (pkg == "com.android.settings" || pkg.contains("settings") || act.contains("settings")) {
            return SemanticIcon.SETTINGS
        }

        // 8. Calculator
        if (pkg.contains("calculator") || pkg.contains("calc") ||
            act.contains("calculator") || act.contains("calc")) {
            return SemanticIcon.CALCULATOR
        }

        // 9. Clock / Alarm
        if (pkg.contains("deskclock") || pkg.contains("clock") || pkg.contains("alarm") ||
            act.contains("deskclock") || act.contains("clock")) {
            return SemanticIcon.CLOCK
        }

        // 10. Mail
        if (pkg == "com.google.android.gm" || pkg.contains("mail") || pkg.contains("email") || pkg.contains("outlook") ||
            act.contains("mail") || act.contains("email")) {
            return SemanticIcon.MAIL
        }

        // 11. Browser
        if (pkg == "com.android.chrome" || pkg.contains("browser") || pkg.contains("firefox") || pkg.contains("edge") ||
            act.contains("browser")) {
            return SemanticIcon.BROWSER
        }

        // 12. Maps
        if (pkg == "com.google.android.apps.maps" || pkg.contains("maps") || pkg.contains("navigation") ||
            act.contains("maps")) {
            return SemanticIcon.MAPS
        }

        // 13. Store / Play Store
        if (pkg == "com.android.vending" || pkg.contains("vending") || pkg.contains("appstore") ||
            act.contains("vending")) {
            return SemanticIcon.STORE
        }

        // 14. Files
        if (pkg.contains("documentsui") || pkg.contains("files") || pkg.contains("filemanager") ||
            act.contains("files") || act.contains("filemanager")) {
            return SemanticIcon.FILES
        }

        // 15. Music
        if (pkg.contains("music") || pkg.contains("spotify") || act.contains("music")) {
            return SemanticIcon.MUSIC
        }

        // 16. Video
        if (pkg == "com.google.android.youtube" || pkg.contains("youtube") || pkg.contains("videoplayer") ||
            act.contains("video")) {
            return SemanticIcon.VIDEO
        }

        return null
    }
}
