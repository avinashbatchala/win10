package com.ab

import com.ab.model.TileModel
import com.ab.model.TileSize
import com.ab.ui.viewmodel.GridManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testOverlapDetection() {
        // Tile 1 at (0, 0) size 2x2
        // Tile 2 at (1, 1) size 2x2 -> should overlap
        assertTrue(GridManager.overlaps(0, 0, 2, 2, 1, 1, 2, 2))

        // Tile 3 at (2, 0) size 2x2 -> touches right edge, should NOT overlap
        assertFalse(GridManager.overlaps(0, 0, 2, 2, 2, 0, 2, 2))

        // Tile 4 at (0, 2) size 2x2 -> touches bottom edge, should NOT overlap
        assertFalse(GridManager.overlaps(0, 0, 2, 2, 0, 2, 2, 2))
    }

    @Test
    fun testFindFirstAvailablePosition() {
        val tiles = listOf(
            TileModel(id = "1", packageName = "p1", label = "P1", size = TileSize.MEDIUM, col = 0, row = 0),
            TileModel(id = "2", packageName = "p2", label = "P2", size = TileSize.MEDIUM, col = 2, row = 0),
            TileModel(id = "3", packageName = "p3", label = "P3", size = TileSize.MEDIUM, col = 4, row = 0)
        )
        // Row 0 is full (6 columns: 0, 1, 2, 3, 4, 5). Next 2x2 tile must be placed at col 0, row 2
        val pos = GridManager.findFirstAvailablePosition(2, 2, tiles, 6)
        assertEquals(0, pos.first)
        assertEquals(2, pos.second)
    }

    @Test
    fun testResizeTileCycle() {
        val initialTiles = listOf(
            TileModel(id = "t1", packageName = "p1", label = "P1", size = TileSize.MEDIUM, col = 0, row = 0)
        )
        val resized = GridManager.resizeTile("t1", TileSize.WIDE, initialTiles, 6)
        assertEquals(1, resized.size)
        assertEquals(TileSize.WIDE, resized[0].size)
        assertEquals(4, resized[0].effectiveCols)
        assertEquals(2, resized[0].effectiveRows)
    }

    @Test
    fun testMoveTile() {
        val initialTiles = listOf(
            TileModel(id = "t1", packageName = "p1", label = "P1", size = TileSize.MEDIUM, col = 0, row = 0),
            TileModel(id = "t2", packageName = "p2", label = "P2", size = TileSize.MEDIUM, col = 2, row = 0)
        )
        val moved = GridManager.moveTile("t1", 4, 0, initialTiles, 6)
        val t1 = moved.first { it.id == "t1" }
        assertEquals(4, t1.col)
        assertEquals(0, t1.row)
    }

    @Test
    fun testNoCollisionsAfterResizeAndMove() {
        val tiles = listOf(
            TileModel(id = "t1", packageName = "p1", label = "P1", size = TileSize.MEDIUM, col = 0, row = 0),
            TileModel(id = "t2", packageName = "p2", label = "P2", size = TileSize.MEDIUM, col = 2, row = 0),
            TileModel(id = "t3", packageName = "p3", label = "P3", size = TileSize.MEDIUM, col = 4, row = 0)
        )
        val resized = GridManager.resizeTile("t1", TileSize.WIDE, tiles, 6)

        for (i in resized.indices) {
            for (j in i + 1 until resized.size) {
                val a = resized[i]
                val b = resized[j]
                val collides = GridManager.overlaps(
                    a.col, a.row, a.effectiveCols, a.effectiveRows,
                    b.col, b.row, b.effectiveCols, b.effectiveRows
                )
                assertFalse("Tiles ${a.id} and ${b.id} must not collide", collides)
            }
        }
    }

    @Test
    fun testRepackGridBetweenDensities() {
        val originalTiles = listOf(
            TileModel(id = "t1", packageName = "p1", label = "P1", size = TileSize.MEDIUM, col = 0, row = 0),
            TileModel(id = "t2", packageName = "p2", label = "P2", size = TileSize.WIDE, col = 2, row = 0),
            TileModel(id = "t3", packageName = "p3", label = "P3", size = TileSize.SMALL, col = 0, row = 2),
            TileModel(id = "t4", packageName = "p4", label = "P4", size = TileSize.LARGE, col = 1, row = 2)
        )

        // Switch density to 8 columns
        val repacked8 = GridManager.repackGrid(originalTiles, 8)
        assertEquals("All tiles must be preserved", originalTiles.size, repacked8.size)

        for (t in repacked8) {
            assertTrue("Tile ${t.id} must fit in 8 columns", t.col + t.effectiveCols <= 8)
        }

        // Verify no collisions in 8-column layout
        for (i in repacked8.indices) {
            for (j in i + 1 until repacked8.size) {
                val a = repacked8[i]
                val b = repacked8[j]
                assertFalse(
                    "Collision detected in 8-col layout between ${a.id} and ${b.id}",
                    GridManager.overlaps(a.col, a.row, a.effectiveCols, a.effectiveRows, b.col, b.row, b.effectiveCols, b.effectiveRows)
                )
            }
        }

        // Switch density back to 6 columns
        val repacked6 = GridManager.repackGrid(repacked8, 6)
        assertEquals("All tiles must be preserved", originalTiles.size, repacked6.size)

        for (t in repacked6) {
            assertTrue("Tile ${t.id} must fit in 6 columns", t.col + t.effectiveCols <= 6)
        }

        // Verify no collisions in 6-column layout
        for (i in repacked6.indices) {
            for (j in i + 1 until repacked6.size) {
                val a = repacked6[i]
                val b = repacked6[j]
                assertFalse(
                    "Collision detected in 6-col layout between ${a.id} and ${b.id}",
                    GridManager.overlaps(a.col, a.row, a.effectiveCols, a.effectiveRows, b.col, b.row, b.effectiveCols, b.effectiveRows)
                )
            }
        }
    }

    @Test
    fun testMetroIconOverrides() {
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.PHONE,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.dialer")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.MESSAGING,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.apps.messaging")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.PEOPLE,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.contacts")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.CALENDAR,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.calendar")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.CAMERA,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.GoogleCamera")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.PHOTOS,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.apps.photos")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.SETTINGS,
            com.ab.data.MetroIconOverrides.findOverride("com.android.settings")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.CALCULATOR,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.calculator")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.CLOCK,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.deskclock")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.MAIL,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.gm")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.BROWSER,
            com.ab.data.MetroIconOverrides.findOverride("com.android.chrome")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.MAPS,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.apps.maps")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.STORE,
            com.ab.data.MetroIconOverrides.findOverride("com.android.vending")
        )
        assertEquals(
            com.ab.data.MetroIconOverrides.SemanticIcon.FILES,
            com.ab.data.MetroIconOverrides.findOverride("com.google.android.apps.nbu.files")
        )

        // Non-system / arbitrary 3rd party app should return null to preserve original branding
        assertEquals(
            null,
            com.ab.data.MetroIconOverrides.findOverride("com.supercell.clashofclans")
        )
    }

    @Test
    fun testLiveTileRegistry() {
        val registry = com.ab.livetile.engine.LiveTileRegistry()

        // Clock matching
        val clockProvider = registry.findProvider("com.google.android.deskclock")
        assertTrue("Clock provider should be resolved for deskclock", clockProvider != null)
        assertEquals("livetile.system.clock", clockProvider?.providerId)

        // Calendar / Date matching
        val dateProvider = registry.findProvider("com.google.android.calendar")
        assertTrue("Date provider should be resolved for calendar", dateProvider != null)
        assertEquals("livetile.system.date", dateProvider?.providerId)

        // Battery matching
        val batteryProvider = registry.findProvider("livetile.demo.battery")
        assertTrue("Battery provider should be resolved for demo battery", batteryProvider != null)
        assertEquals("livetile.system.battery", batteryProvider?.providerId)

        // Unrelated third party app -> must return null for static fallback
        val unknownProvider = registry.findProvider("com.spotify.music")
        assertEquals(null, unknownProvider)
    }

    @Test
    fun testLiveTileMultiFaceTransitions() {
        val face1 = com.ab.livetile.model.LiveTileFace(
            template = com.ab.livetile.model.LiveTileTemplate.COUNT,
            primaryText = "85%",
            accessibilityDescription = "Battery 85%"
        )
        val face2 = com.ab.livetile.model.LiveTileFace(
            template = com.ab.livetile.model.LiveTileTemplate.PRIMARY_TEXT,
            primaryText = "Charging",
            accessibilityDescription = "Charging"
        )

        val state = com.ab.livetile.model.LiveTileState(
            providerId = "demo.test",
            faces = listOf(face1, face2),
            activeFaceIndex = 0
        )

        assertEquals(face1, state.activeFace)

        // Step to next face
        val state2 = state.nextFace()
        assertEquals(1, state2.activeFaceIndex)
        assertEquals(face2, state2.activeFace)

        // Step again -> wraps around to 0
        val state3 = state2.nextFace()
        assertEquals(0, state3.activeFaceIndex)
        assertEquals(face1, state3.activeFace)
    }

    // ---------------------------------------------------------------------------
    // resizeTile regression tests
    // ---------------------------------------------------------------------------

    private fun tile(
        id: String,
        size: TileSize,
        col: Int,
        row: Int
    ) = TileModel(id = id, packageName = "p.$id", label = id, size = size, col = col, row = row)

    private fun assertGridInvariants(tiles: List<TileModel>, totalColumns: Int) {
        for (t in tiles) {
            assertTrue(
                "Tile ${t.id} must be within $totalColumns columns (col=${t.col}, cols=${t.effectiveCols})",
                t.col >= 0 && t.col + t.effectiveCols <= totalColumns
            )
            assertTrue("Tile ${t.id} must not have a negative row", t.row >= 0)
        }
        for (i in tiles.indices) {
            for (j in i + 1 until tiles.size) {
                val a = tiles[i]
                val b = tiles[j]
                assertFalse(
                    "Tiles ${a.id} and ${b.id} must not overlap",
                    GridManager.overlaps(
                        a.col, a.row, a.effectiveCols, a.effectiveRows,
                        b.col, b.row, b.effectiveCols, b.effectiveRows
                    )
                )
            }
        }
    }

    @Test
    fun resizeToWideWithFreeSpaceAnchorsAndLeavesUnrelatedTiles() {
        val tiles = listOf(
            tile("t1", TileSize.MEDIUM, col = 0, row = 0),
            tile("t2", TileSize.MEDIUM, col = 4, row = 0)
        )

        val resized = GridManager.resizeTile("t1", TileSize.WIDE, tiles, 6)

        val t1 = resized.first { it.id == "t1" }
        assertEquals(TileSize.WIDE, t1.size)
        assertEquals("Resized tile must stay at its column", 0, t1.col)
        assertEquals("Resized tile must stay at its row", 0, t1.row)

        val t2 = resized.first { it.id == "t2" }
        assertEquals("Unrelated tile must not move", 4, t2.col)
        assertEquals("Unrelated tile must not move", 0, t2.row)

        assertGridInvariants(resized, 6)
    }

    @Test
    fun resizeIntoCollisionMovesOnlyConflictingTiles() {
        val tiles = listOf(
            tile("t1", TileSize.MEDIUM, col = 0, row = 0), // resized target
            tile("t2", TileSize.MEDIUM, col = 2, row = 0), // conflicts once t1 becomes WIDE
            tile("t3", TileSize.SMALL, col = 5, row = 0),  // unrelated
            tile("t4", TileSize.MEDIUM, col = 0, row = 2)  // unrelated
        )

        val resized = GridManager.resizeTile("t1", TileSize.WIDE, tiles, 6)

        val t1 = resized.first { it.id == "t1" }
        assertEquals("Resized tile must remain anchored", 0, t1.col)
        assertEquals("Resized tile must remain anchored", 0, t1.row)

        val t3 = resized.first { it.id == "t3" }
        assertEquals("Unrelated tile t3 must keep its column", 5, t3.col)
        assertEquals("Unrelated tile t3 must keep its row", 0, t3.row)

        val t4 = resized.first { it.id == "t4" }
        assertEquals("Unrelated tile t4 must keep its column", 0, t4.col)
        assertEquals("Unrelated tile t4 must keep its row", 2, t4.row)

        assertGridInvariants(resized, 6)
    }

    @Test
    fun resizeLargeToSmallDoesNotCompactUnrelatedTiles() {
        val tiles = listOf(
            tile("t1", TileSize.LARGE, col = 0, row = 0),
            tile("t2", TileSize.MEDIUM, col = 2, row = 4)
        )

        val resized = GridManager.resizeTile("t1", TileSize.SMALL, tiles, 6)

        val t1 = resized.first { it.id == "t1" }
        assertEquals(TileSize.SMALL, t1.size)
        assertEquals("Shrinking must keep the resized tile in place", 0, t1.col)
        assertEquals("Shrinking must keep the resized tile in place", 0, t1.row)

        val t2 = resized.first { it.id == "t2" }
        assertEquals("Shrinking must not compact unrelated tiles", 2, t2.col)
        assertEquals("Shrinking must not compact unrelated tiles", 4, t2.row)

        assertGridInvariants(resized, 6)
    }

    @Test
    fun resizeNearRightEdgeClampsHorizontally() {
        val tiles = listOf(
            tile("t1", TileSize.MEDIUM, col = 4, row = 0)
        )

        val resized = GridManager.resizeTile("t1", TileSize.WIDE, tiles, 6)

        val t1 = resized.first { it.id == "t1" }
        assertEquals("WIDE tile must be clamped to fit", 2, t1.col)
        assertEquals("Row must be preserved", 0, t1.row)
        assertGridInvariants(resized, 6)
    }

    @Test
    fun everyResizeProducesValidNonOverlappingLayout() {
        val base = listOf(
            tile("t1", TileSize.MEDIUM, col = 0, row = 0),
            tile("t2", TileSize.MEDIUM, col = 2, row = 0),
            tile("t3", TileSize.WIDE, col = 0, row = 2),
            tile("t4", TileSize.SMALL, col = 5, row = 0),
            tile("t5", TileSize.LARGE, col = 0, row = 4)
        )

        val sizes = listOf(TileSize.SMALL, TileSize.MEDIUM, TileSize.WIDE, TileSize.LARGE)
        for (tileId in base.map { it.id }) {
            for (size in sizes) {
                val resized = GridManager.resizeTile(tileId, size, base, 6)
                assertEquals("All tiles must be preserved", base.size, resized.size)
                assertGridInvariants(resized, 6)
            }
        }
    }

    @Test
    fun displayScaleAutoTracksScreenWidthAndClamps() {
        assertEquals(0.9f, com.ab.model.DisplayScale.AUTO.resolve(200), 0.001f)
        assertEquals(1.0f, com.ab.model.DisplayScale.AUTO.resolve(360), 0.001f)
        assertEquals(1.35f, com.ab.model.DisplayScale.AUTO.resolve(900), 0.001f)
        assertEquals(0.9f, com.ab.model.DisplayScale.SMALL.resolve(411), 0.001f)
        assertEquals(1.3f, com.ab.model.DisplayScale.EXTRA.resolve(411), 0.001f)
    }

    @Test
    fun tileIconIsRoughlyHalfTheTile() {
        // Medium tile icon should read like the Windows Phone iconic tile (~50-60%).
        val medium = com.ab.ui.components.TileMetrics
            .iconSize(TileSize.MEDIUM, androidx.compose.ui.unit.Dp(120f))
        assertEquals(66f, medium.value, 0.001f)
        // Small is slightly larger relative to the tile.
        val small = com.ab.ui.components.TileMetrics
            .iconSize(TileSize.SMALL, androidx.compose.ui.unit.Dp(50f))
        assertEquals(29f, small.value, 0.001f)
    }

    @Test
    fun tileEditControlsFitSmallTilesInBothGridDensities() {
        // 8-column SMALL tile is the tightest case (~34dp); 6-column is ~48dp.
        for (tileMinDp in listOf(34f, 48f, 56.7f)) {
            assertTrue(
                "controls must not overlap at ${tileMinDp}dp",
                com.ab.ui.components.TileEditMetrics.controlsFit(tileMinDp)
            )
        }
        // Medium/large tiles keep the full 32dp control.
        assertEquals(32f, com.ab.ui.components.TileEditMetrics.buttonSizeDp(120f), 0.001f)
    }

    @Test
    fun tileEditControlsShrinkMonotonicallyWithTile() {
        val small = com.ab.ui.components.TileEditMetrics.buttonSizeDp(34f)
        val medium = com.ab.ui.components.TileEditMetrics.buttonSizeDp(48f)
        val large = com.ab.ui.components.TileEditMetrics.buttonSizeDp(120f)
        assertTrue(small < medium)
        assertTrue(medium < large)
    }

    @Test
    fun resizePreservesTileListOrder() {
        val tiles = listOf(
            tile("a", TileSize.SMALL, col = 0, row = 0),
            tile("b", TileSize.MEDIUM, col = 2, row = 0),
            tile("c", TileSize.SMALL, col = 4, row = 0)
        )

        // Resizing the middle tile must not reorder the list, so UI per-tile state
        // (keyed by id) stays stable without a navigation-driven re-render.
        val resized = GridManager.resizeTile("b", TileSize.WIDE, tiles, 6)
        assertEquals(listOf("a", "b", "c"), resized.map { it.id })
        assertGridInvariants(resized, 6)
    }
}
