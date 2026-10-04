package com.ab.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.livetile.model.LiveTileFace
import com.ab.livetile.model.LiveTileState
import com.ab.livetile.model.LiveTileTemplate
import com.ab.livetile.ui.LiveTileRenderer
import com.ab.model.TileSize
import com.ab.ui.icons.MetroIcons
import com.ab.ui.theme.MetroColors
import com.ab.ui.theme.MetroTypography

/**
 * Debug-only Live Tile inspection screen.
 * Displays provider diagnostics, active face indices, refresh states, and visual previews.
 * (Not exposed in the production launcher UI).
 */
@Composable
fun LiveTileDebugScreen(
    tileStates: Map<String, LiveTileState> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MetroColors.BackgroundBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "LIVE TILE INSPECTION",
            style = MetroTypography.settingsTitle,
            color = Color.White
        )
        Text(
            text = "Diagnostics for registered providers, templates, and face rotations",
            style = MetroTypography.settingsSubtext,
            color = MetroColors.TextDim,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 1. Registered Live Tile States
        Text(
            text = "ACTIVE LIVE TILES (${tileStates.size})",
            style = MetroTypography.settingsSection,
            color = MetroColors.LumiaBlue,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (tileStates.isEmpty()) {
            Text(
                text = "No active Live Tile states registered yet.",
                style = MetroTypography.settingsSubtext,
                color = MetroColors.TextDim,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            tileStates.forEach { (key, state) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151515), RectangleShape)
                        .border(1.dp, Color(0xFF333333), RectangleShape)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = key,
                            style = MetroTypography.tileLabel.copy(fontSize = 15.sp),
                            color = Color.White
                        )
                        Text(
                            text = "Provider: ${state.providerId}",
                            style = MetroTypography.settingsSubtext,
                            color = MetroColors.LumiaBlue
                        )
                        Text(
                            text = "Faces: ${state.faces.size} | Active: Face ${state.activeFaceIndex + 1} | Stale: ${state.isStale}",
                            style = MetroTypography.settingsSubtext,
                            color = MetroColors.TextDim
                        )
                        if (state.activeFace != null) {
                            Text(
                                text = "Template: ${state.activeFace?.template?.name} | Primary: \"${state.activeFace?.primaryText ?: ""}\"",
                                style = MetroTypography.settingsSubtext,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        if (state.error != null) {
                            Text(
                                text = "Error: ${state.error}",
                                style = MetroTypography.settingsSubtext,
                                color = Color(0xFFE81123)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Demonstration Previews for all Templates
        Text(
            text = "TEMPLATE VISUAL INSPECTION",
            style = MetroTypography.settingsSection,
            color = MetroColors.LumiaBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // DATE template (Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("DATE (Medium 2x2)", style = MetroTypography.editBadge, color = MetroColors.TextDim)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(MetroColors.LumiaBlue, RectangleShape)
                ) {
                    LiveTileRenderer(
                        liveState = LiveTileState(
                            providerId = "demo.date",
                            faces = listOf(
                                LiveTileFace(
                                    template = LiveTileTemplate.DATE,
                                    primaryText = "4",
                                    secondaryText = "Sunday",
                                    tertiaryText = "October",
                                    iconVector = MetroIcons.Calendar,
                                    accessibilityDescription = "Sunday, October 4"
                                )
                            )
                        ),
                        tileSize = TileSize.MEDIUM,
                        defaultLabel = "Calendar"
                    )
                }
            }

            // PRIMARY_TEXT template (Clock Medium)
            Column {
                Text("PRIMARY_TEXT (Clock)", style = MetroTypography.editBadge, color = MetroColors.TextDim)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFF004E8C), RectangleShape)
                ) {
                    LiveTileRenderer(
                        liveState = LiveTileState(
                            providerId = "demo.clock",
                            faces = listOf(
                                LiveTileFace(
                                    template = LiveTileTemplate.PRIMARY_TEXT,
                                    primaryText = "14:02",
                                    secondaryText = "Sunday",
                                    iconVector = MetroIcons.Clock,
                                    accessibilityDescription = "Time 14:02"
                                )
                            )
                        ),
                        tileSize = TileSize.MEDIUM,
                        defaultLabel = "Alarms & Clock"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // COUNT template (Battery Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("COUNT (Battery 2x2)", style = MetroTypography.editBadge, color = MetroColors.TextDim)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFF107C10), RectangleShape)
                ) {
                    LiveTileRenderer(
                        liveState = LiveTileState(
                            providerId = "demo.battery",
                            faces = listOf(
                                LiveTileFace(
                                    template = LiveTileTemplate.COUNT,
                                    primaryText = "85%",
                                    secondaryText = "remaining",
                                    iconVector = MetroIcons.Settings,
                                    accessibilityDescription = "Battery 85%"
                                )
                            )
                        ),
                        tileSize = TileSize.MEDIUM,
                        defaultLabel = "Battery"
                    )
                }
            }

            // TEXT_LINES template (Wide 4x2)
            Column {
                Text("TEXT_LINES (Wide 4x2)", style = MetroTypography.editBadge, color = MetroColors.TextDim)
                Box(
                    modifier = Modifier
                        .size(width = 220.dp, height = 110.dp)
                        .background(Color(0xFF6B007B), RectangleShape)
                ) {
                    LiveTileRenderer(
                        liveState = LiveTileState(
                            providerId = "demo.text",
                            faces = listOf(
                                LiveTileFace(
                                    template = LiveTileTemplate.TEXT_LINES,
                                    primaryText = "Team Standup",
                                    textLines = listOf("10:30 AM - Room 4B", "Organizer: Alex"),
                                    accessibilityDescription = "Team Standup, 10:30 AM"
                                )
                            )
                        ),
                        tileSize = TileSize.WIDE,
                        defaultLabel = "Calendar"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LiveTileDebugPreview() {
    LiveTileDebugScreen()
}
