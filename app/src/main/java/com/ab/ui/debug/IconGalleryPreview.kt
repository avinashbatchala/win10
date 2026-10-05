package com.ab.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ab.model.ResolvedLauncherIcon
import com.ab.model.TileSize
import com.ab.ui.components.LauncherIconView
import com.ab.ui.components.TileMetrics
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroDimensions
import com.ab.ui.theme.MetroTypography

/**
 * Debug-only icon gallery showing all Metro glyphs, small-tile rendering,
 * medium-tile rendering, and Apps-list row rendering for visual consistency inspection.
 * (Not exposed in the production launcher navigation/UX).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconGalleryScreen(modifier: Modifier = Modifier) {
    val glyphs = listOf(
        "Phone" to MetroIcons.Phone,
        "Messaging" to MetroIcons.Messaging,
        "People" to MetroIcons.People,
        "Calendar" to MetroIcons.Calendar,
        "Camera" to MetroIcons.Camera,
        "Photos" to MetroIcons.Photos,
        "Settings" to MetroIcons.Settings,
        "Calculator" to MetroIcons.Calculator,
        "Clock" to MetroIcons.Clock,
        "Mail" to MetroIcons.Mail,
        "Browser" to MetroIcons.Browser,
        "Maps" to MetroIcons.Maps,
        "Files" to MetroIcons.Files,
        "Store" to MetroIcons.Store,
        "Music" to MetroIcons.Music,
        "Video" to MetroIcons.Video,
        "Search" to MetroIcons.Search,
        "Pin" to MetroIcons.Pin,
        "Unpin" to MetroIcons.Unpin,
        "Resize" to MetroIcons.Resize,
        "Ellipsis" to MetroIcons.Ellipsis,
        "Back" to MetroIcons.Back,
        "Forward" to MetroIcons.Forward,
        "Add" to MetroIcons.Add,
        "Check" to MetroIcons.Check,
        "Close" to MetroIcons.Close,
        "GenericApp" to MetroIcons.GenericApp
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroColors.BackgroundBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "ICON GALLERY",
            style = MetroTypography.settingsTitle,
            color = Color.White
        )
        Text(
            text = "Visual verification of Segoe MDL2-style stroke weight and optical alignment",
            style = MetroTypography.settingsSubtext,
            color = MetroColors.TextDim,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Raw Glyphs on Accent Tiles
        Text(
            text = "METRO VECTOR GLYPHS (24x24)",
            style = MetroTypography.settingsSection,
            color = MetroColors.LumiaBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            glyphs.forEach { (name, vector) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(60.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MetroColors.LumiaBlue, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = name,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = name,
                        style = MetroTypography.editBadge,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Tile Renderings: Small vs Medium
        Text(
            text = "START TILE PROPORTIONS",
            style = MetroTypography.settingsSection,
            color = MetroColors.LumiaBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Small Tile preview (1x1)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Small (1x1)",
                    style = MetroTypography.editBadge,
                    color = MetroColors.TextDim,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MetroColors.LumiaBlue, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    LauncherIconView(
                        icon = ResolvedLauncherIcon.VectorGlyph(MetroIcons.Phone, "PHONE"),
                        contentDescription = "Phone",
                        tint = Color.White,
                        modifier = Modifier.size(TileMetrics.iconSize(TileSize.SMALL, 48.dp))
                    )
                }
            }

            // Medium Tile preview (2x2)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Medium (2x2)",
                    style = MetroTypography.editBadge,
                    color = MetroColors.TextDim,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(MetroColors.LumiaBlue, RectangleShape)
                        .padding(MetroDimensions.tileContentPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LauncherIconView(
                            icon = ResolvedLauncherIcon.VectorGlyph(MetroIcons.Phone, "PHONE"),
                            contentDescription = "Phone",
                            tint = Color.White,
                            modifier = Modifier.size(TileMetrics.iconSize(TileSize.MEDIUM, 104.dp))
                        )
                    }
                    Text(
                        text = "Phone",
                        style = MetroTypography.tileLabel,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Apps-list row preview
        Text(
            text = "APPS LIST ROW PRESENTATION",
            style = MetroTypography.settingsSection,
            color = MetroColors.LumiaBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MetroDimensions.appListRowHeight)
                .background(Color(0xFF111111), RectangleShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(MetroDimensions.appListIconBoxSize)
                    .background(MetroColors.LumiaBlue, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = MetroIcons.Messaging,
                    contentDescription = "Messaging",
                    tint = Color.White,
                    modifier = Modifier.size(MetroDimensions.appListIconInnerSize)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "Messaging",
                style = MetroTypography.appListItem,
                color = Color.White
            )
        }
    }
}

@Preview
@Composable
private fun IconGalleryPreview() {
    IconGalleryScreen()
}
