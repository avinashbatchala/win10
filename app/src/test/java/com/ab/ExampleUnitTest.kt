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
}
