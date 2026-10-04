package com.example

import com.example.model.TileModel
import com.example.model.TileSize
import com.example.ui.viewmodel.GridManager
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
}
