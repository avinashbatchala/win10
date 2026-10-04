package com.ab.ui.settings

import androidx.compose.ui.graphics.vector.ImageVector
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons

/** The six horizontally swipeable pivot pages inside launcher Settings. */
enum class SettingsPivot(val title: String) {
    START("start"),
    TILES("tiles"),
    APPS("apps"),
    MEDIA("media"),
    SYSTEM("system"),
    ABOUT("about")
}

/**
 * Internal settings navigation model. Deep links (search results, long-press actions,
 * context menus) select a destination instead of scattering boolean flags through the UI.
 */
sealed interface SettingsDestination {
    data object Root : SettingsDestination

    data class Launcher(
        val pivot: SettingsPivot,
        val scrollToSettingId: String? = null
    ) : SettingsDestination

    data object HiddenApps : SettingsDestination

    data object ManageApps : SettingsDestination

    data class AppDetails(val packageName: String) : SettingsDestination
}

/** A root Settings category row. */
data class SettingsRootCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val pivot: SettingsPivot
)

object SettingsCategories {
    val ALL: List<SettingsRootCategory> = listOf(
        SettingsRootCategory(
            title = "Personalization",
            subtitle = "Start, colors, background",
            icon = MetroIcons.Photos,
            pivot = SettingsPivot.START
        ),
        SettingsRootCategory(
            title = "Tiles + Start",
            subtitle = "Live tiles, system tiles, appearance",
            icon = MetroIcons.GenericApp,
            pivot = SettingsPivot.TILES
        ),
        SettingsRootCategory(
            title = "Apps",
            subtitle = "App list, icons, search",
            icon = MetroIcons.Store,
            pivot = SettingsPivot.APPS
        ),
        SettingsRootCategory(
            title = "Media",
            subtitle = "Now Playing, media access",
            icon = MetroIcons.Music,
            pivot = SettingsPivot.MEDIA
        ),
        SettingsRootCategory(
            title = "System",
            subtitle = "Home, gestures, behavior",
            icon = MetroIcons.Settings,
            pivot = SettingsPivot.SYSTEM
        ),
        SettingsRootCategory(
            title = "About",
            subtitle = "Version, diagnostics, licenses",
            icon = MetroIcons.Info,
            pivot = SettingsPivot.ABOUT
        )
    )
}

/** A launcher-owned special tile that can be pinned without appearing in the Apps list. */
data class SystemTileDef(
    val packageName: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val defaultSize: TileSize
)

object SystemTiles {
    const val NOW_PLAYING_PACKAGE = "livetile.nowplaying"
    const val CLOCK_PACKAGE = "livetile.demo.clock"
    const val DATE_PACKAGE = "livetile.demo.date"
    const val BATTERY_PACKAGE = "livetile.demo.battery"

    val ALL: List<SystemTileDef> = listOf(
        SystemTileDef(
            packageName = NOW_PLAYING_PACKAGE,
            label = "Now Playing",
            description = "Media playback from compatible apps",
            icon = MetroIcons.Music,
            defaultSize = TileSize.WIDE
        ),
        SystemTileDef(
            packageName = CLOCK_PACKAGE,
            label = "Clock",
            description = "Time and date updates",
            icon = MetroIcons.Clock,
            defaultSize = TileSize.SMALL
        ),
        SystemTileDef(
            packageName = DATE_PACKAGE,
            label = "Calendar Date",
            description = "Today's date at a glance",
            icon = MetroIcons.Calendar,
            defaultSize = TileSize.MEDIUM
        ),
        SystemTileDef(
            packageName = BATTERY_PACKAGE,
            label = "Battery",
            description = "Charge level and status",
            icon = MetroIcons.Battery,
            defaultSize = TileSize.SMALL
        )
    )
}

/**
 * A single searchable settings entry. Search stays local and indexes titles,
 * descriptions, page names and common synonyms.
 */
data class SettingsSearchEntry(
    val settingId: String,
    val title: String,
    val breadcrumb: String,
    val description: String,
    val destination: SettingsDestination,
    val keywords: List<String> = emptyList()
)

object SettingsSearchIndex {
    val ENTRIES: List<SettingsSearchEntry> = listOf(
        SettingsSearchEntry(
            "start.background", "Background", "Start › Background",
            "Choose a Start background picture or style",
            SettingsDestination.Launcher(SettingsPivot.START, "start.background"),
            listOf("wallpaper", "picture", "photo", "image", "background", "full screen", "tile picture")
        ),
        SettingsSearchEntry(
            "start.transparency", "Tile transparency", "Start › Tile transparency",
            "Make Start tiles transparent",
            SettingsDestination.Launcher(SettingsPivot.START, "start.transparency"),
            listOf("transparent", "transparency", "opacity", "see through")
        ),
        SettingsSearchEntry(
            "start.color", "Choose your color", "Start › Choose your color",
            "Accent color used across the launcher",
            SettingsDestination.Launcher(SettingsPivot.START, "start.color"),
            listOf("accent", "color", "colour", "theme color")
        ),
        SettingsSearchEntry(
            "start.theme", "Theme", "Start › Theme",
            "Dark or light launcher theme",
            SettingsDestination.Launcher(SettingsPivot.START, "start.theme"),
            listOf("dark", "light", "theme", "mode", "appearance")
        ),
        SettingsSearchEntry(
            "start.density", "Show more tiles", "Start › Show more tiles",
            "Fit more tiles on Start",
            SettingsDestination.Launcher(SettingsPivot.START, "start.density"),
            listOf("density", "columns", "more tiles", "grid", "layout")
        ),
        SettingsSearchEntry(
            "tiles.live.enable", "Enable Live Tiles", "Tiles + Start › Live Tiles",
            "Turn live tile data on or off",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.live.enable"),
            listOf("live tile", "live tiles", "enable", "disable")
        ),
        SettingsSearchEntry(
            "tiles.live.animate", "Animate Live Tiles", "Tiles + Start › Live Tiles",
            "Suppress automatic face transitions while keeping data visible",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.live.animate"),
            listOf("animate", "animation", "flip", "spin", "live tile")
        ),
        SettingsSearchEntry(
            "tiles.live.frequency", "Animation frequency", "Tiles + Start › Live Tiles",
            "How often tile faces rotate",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.live.frequency"),
            listOf("frequency", "speed", "animation", "live tile", "rate")
        ),
        SettingsSearchEntry(
            "tiles.live.pause_hidden", "Pause animations when launcher is not visible", "Tiles + Start › Live Tile behavior",
            "Stop tile animation while the launcher is in the background",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.live.pause_hidden"),
            listOf("pause", "background", "hidden", "battery")
        ),
        SettingsSearchEntry(
            "tiles.live.pause_battery", "Pause animations in battery saver", "Tiles + Start › Live Tile behavior",
            "Stop tile animation when battery saver is on",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.live.pause_battery"),
            listOf("battery", "saver", "power", "pause", "battery saver")
        ),
        SettingsSearchEntry(
            "tiles.system_tiles", "System tiles", "Tiles + Start › System tiles",
            "Pin launcher-owned Clock, Date, Battery and Now Playing tiles",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.system_tiles"),
            listOf("system tile", "clock", "battery", "date", "now playing", "pin")
        ),
        SettingsSearchEntry(
            "tiles.default_size", "Default tile size", "Tiles + Start › Default tile size",
            "Size used for newly pinned applications",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.default_size"),
            listOf("default size", "small", "medium", "new tiles")
        ),
        SettingsSearchEntry(
            "tiles.app_names", "Show app names", "Tiles + Start › Show app names",
            "Show app names on medium and larger tiles",
            SettingsDestination.Launcher(SettingsPivot.TILES, "tiles.app_names"),
            listOf("app name", "labels", "tile label", "text")
        ),
        SettingsSearchEntry(
            "apps.jumplist", "Alphabet jump list", "Apps › App list",
            "Show the A–Z jump list in the Apps list",
            SettingsDestination.Launcher(SettingsPivot.APPS, "apps.jumplist"),
            listOf("jump list", "alphabet", "a-z", "index", "letters")
        ),
        SettingsSearchEntry(
            "apps.icons", "Icon appearance", "Apps › Icons",
            "Choose automatic, original or monochrome app icons",
            SettingsDestination.Launcher(SettingsPivot.APPS, "apps.icons"),
            listOf("icons", "monochrome", "original icon", "metro icon", "appearance")
        ),
        SettingsSearchEntry(
            "apps.hidden", "Hidden apps", "Apps › Hidden apps",
            "Hide applications from the Apps list without uninstalling them",
            SettingsDestination.HiddenApps,
            listOf("hidden", "hide apps", "hidden apps")
        ),
        SettingsSearchEntry(
            "apps.manage", "Manage apps", "Apps › Manage apps",
            "View, pin, hide or uninstall installed applications",
            SettingsDestination.ManageApps,
            listOf("manage apps", "uninstall", "app info", "installed apps")
        ),
        SettingsSearchEntry(
            "media.show", "Show media on Live Tiles", "Media › Media Live Tiles",
            "Display playback from compatible media apps on tiles",
            SettingsDestination.Launcher(SettingsPivot.MEDIA, "media.show"),
            listOf("music", "media", "now playing", "live tile")
        ),
        SettingsSearchEntry(
            "media.access", "Media access", "Media › Media access",
            "Allow notification access so media sessions can be discovered",
            SettingsDestination.Launcher(SettingsPivot.MEDIA, "media.access"),
            listOf("notification access", "media access", "permission", "allow")
        ),
        SettingsSearchEntry(
            "media.nowplaying", "Now Playing tile", "Media › Now Playing",
            "Pin or unpin the Now Playing tile",
            SettingsDestination.Launcher(SettingsPivot.MEDIA, "media.nowplaying"),
            listOf("now playing", "pin", "unpin", "tile")
        ),
        SettingsSearchEntry(
            "media.artwork", "Show album artwork", "Media › Artwork",
            "Display album art on media tiles",
            SettingsDestination.Launcher(SettingsPivot.MEDIA, "media.artwork"),
            listOf("artwork", "album art", "cover", "image")
        ),
        SettingsSearchEntry(
            "media.controls", "Show playback controls", "Media › Playback controls",
            "Show controls on wide and large media tiles",
            SettingsDestination.Launcher(SettingsPivot.MEDIA, "media.controls"),
            listOf("controls", "play", "pause", "skip", "transport")
        ),
        SettingsSearchEntry(
            "media.progress", "Show playback progress", "Media › Progress",
            "Show a progress bar for the current track",
            SettingsDestination.Launcher(SettingsPivot.MEDIA, "media.progress"),
            listOf("progress", "seek", "position", "bar")
        ),
        SettingsSearchEntry(
            "system.home", "Default Home app", "System › Home",
            "Set the launcher as the default Home app",
            SettingsDestination.Launcher(SettingsPivot.SYSTEM, "system.home"),
            listOf("default", "home", "launcher", "set default")
        ),
        SettingsSearchEntry(
            "system.orientation", "Screen orientation", "System › Orientation",
            "Portrait only or follow the system",
            SettingsDestination.Launcher(SettingsPivot.SYSTEM, "system.orientation"),
            listOf("orientation", "portrait", "rotate", "landscape")
        ),
        SettingsSearchEntry(
            "system.reset_layout", "Reset Start layout", "System › Reset",
            "Restore the default arrangement of tiles",
            SettingsDestination.Launcher(SettingsPivot.SYSTEM, "system.reset_layout"),
            listOf("reset", "layout", "restore", "start")
        ),
        SettingsSearchEntry(
            "system.reset_all", "Reset all launcher settings", "System › Reset",
            "Restore every launcher setting to its default value",
            SettingsDestination.Launcher(SettingsPivot.SYSTEM, "system.reset_all"),
            listOf("reset", "factory", "defaults", "all settings")
        ),
        SettingsSearchEntry(
            "about.version", "Version", "About › Version",
            "Launcher version and package information",
            SettingsDestination.Launcher(SettingsPivot.ABOUT, "about.version"),
            listOf("version", "build", "package", "about")
        ),
        SettingsSearchEntry(
            "about.diagnostics", "Diagnostics", "About › Diagnostics",
            "Launcher status, access and tile counts",
            SettingsDestination.Launcher(SettingsPivot.ABOUT, "about.diagnostics"),
            listOf("diagnostics", "status", "debug", "info")
        ),
        SettingsSearchEntry(
            "about.licenses", "Open source licenses", "About › Open source licenses",
            "Licenses for software used by the launcher",
            SettingsDestination.Launcher(SettingsPivot.ABOUT, "about.licenses"),
            listOf("licenses", "open source", "legal")
        ),
        SettingsSearchEntry(
            "about.privacy", "Privacy", "About › Privacy",
            "How the launcher handles your data",
            SettingsDestination.Launcher(SettingsPivot.ABOUT, "about.privacy"),
            listOf("privacy", "data", "permissions")
        )
    )

    /**
     * Local-only search over page names, titles, breadcrumbs, descriptions and synonyms.
     * A blank query returns no results.
     */
    fun search(query: String): List<SettingsSearchEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return ENTRIES.filter { entry ->
            entry.title.lowercase().contains(q) ||
                entry.breadcrumb.lowercase().contains(q) ||
                entry.description.lowercase().contains(q) ||
                entry.keywords.any { it.lowercase().contains(q) }
        }
    }
}
