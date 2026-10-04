package com.example.ui.viewmodel

import com.example.model.TileModel
import com.example.model.TileSize

object GridManager {

    fun overlaps(t1Col: Int, t1Row: Int, t1Cols: Int, t1Rows: Int,
                 t2Col: Int, t2Row: Int, t2Cols: Int, t2Rows: Int): Boolean {
        if (t1Col + t1Cols <= t2Col || t1Col >= t2Col + t2Cols) return false
        if (t1Row + t1Rows <= t2Row || t1Row >= t2Row + t2Rows) return false
        return true
    }

    fun isPositionOccupied(
        col: Int,
        row: Int,
        cols: Int,
        rows: Int,
        tiles: List<TileModel>,
        excludeTileId: String? = null
    ): Boolean {
        for (t in tiles) {
            if (t.id == excludeTileId) continue
            if (overlaps(col, row, cols, rows, t.col, t.row, t.effectiveCols, t.effectiveRows)) {
                return true
            }
        }
        return false
    }

    fun findFirstAvailablePosition(
        cols: Int,
        rows: Int,
        tiles: List<TileModel>,
        totalColumns: Int = 6
    ): Pair<Int, Int> {
        var r = 0
        while (r < 1000) {
            for (c in 0..(totalColumns - cols)) {
                if (!isPositionOccupied(c, r, cols, rows, tiles)) {
                    return Pair(c, r)
                }
            }
            r++
        }
        return Pair(0, 0)
    }

    /**
     * Resizes a tile, adjusting column if it overflows totalColumns,
     * and resolving any collisions by pushing conflicting tiles down.
     */
    fun resizeTile(
        tileId: String,
        newSize: TileSize,
        currentTiles: List<TileModel>,
        totalColumns: Int = 6
    ): List<TileModel> {
        val target = currentTiles.firstOrNull { it.id == tileId } ?: return currentTiles
        var newCol = target.col
        if (newCol + newSize.cols > totalColumns) {
            newCol = (totalColumns - newSize.cols).coerceAtLeast(0)
        }
        val newRow = target.row

        val updatedTarget = target.copy(size = newSize, col = newCol, row = newRow)
        val otherTiles = currentTiles.filter { it.id != tileId }.toMutableList()

        // Push any overlapping tiles downwards
        val resolvedTiles = mutableListOf<TileModel>()
        resolvedTiles.add(updatedTarget)

        for (other in otherTiles) {
            if (overlaps(newCol, newRow, newSize.cols, newSize.rows, other.col, other.row, other.effectiveCols, other.effectiveRows)) {
                // Find next free position below the resized tile
                val nextPos = findFirstAvailablePosition(
                    other.effectiveCols,
                    other.effectiveRows,
                    resolvedTiles,
                    totalColumns
                )
                resolvedTiles.add(other.copy(col = nextPos.first, row = nextPos.second))
            } else {
                resolvedTiles.add(other)
            }
        }

        return compactGrid(resolvedTiles, totalColumns)
    }

    /**
     * Moves a tile to targetCol and targetRow, shifting conflicting tiles smoothly.
     */
    fun moveTile(
        tileId: String,
        targetCol: Int,
        targetRow: Int,
        currentTiles: List<TileModel>,
        totalColumns: Int = 6
    ): List<TileModel> {
        val target = currentTiles.firstOrNull { it.id == tileId } ?: return currentTiles
        val clampedCol = targetCol.coerceIn(0, (totalColumns - target.effectiveCols).coerceAtLeast(0))
        val clampedRow = targetRow.coerceAtLeast(0)

        val updatedTarget = target.copy(col = clampedCol, row = clampedRow)
        val otherTiles = currentTiles.filter { it.id != tileId }

        val resolvedTiles = mutableListOf<TileModel>()
        resolvedTiles.add(updatedTarget)

        for (other in otherTiles) {
            if (overlaps(clampedCol, clampedRow, target.effectiveCols, target.effectiveRows,
                    other.col, other.row, other.effectiveCols, other.effectiveRows)) {
                // Displace colliding tile
                val nextPos = findFirstAvailablePosition(
                    other.effectiveCols,
                    other.effectiveRows,
                    resolvedTiles,
                    totalColumns
                )
                resolvedTiles.add(other.copy(col = nextPos.first, row = nextPos.second))
            } else {
                resolvedTiles.add(other)
            }
        }

        return compactGrid(resolvedTiles, totalColumns)
    }

    /**
     * Compacts tiles upwards where possible to avoid unnecessary empty gaps
     * while preserving relative layout order.
     */
    fun compactGrid(tiles: List<TileModel>, totalColumns: Int = 6): List<TileModel> {
        val sorted = tiles.sortedWith(compareBy({ it.row }, { it.col }))
        val compacted = mutableListOf<TileModel>()

        for (tile in sorted) {
            var bestRow = tile.row
            // Try sliding upwards in the same column
            while (bestRow > 0) {
                val candidateRow = bestRow - 1
                if (!isPositionOccupied(tile.col, candidateRow, tile.effectiveCols, tile.effectiveRows, compacted)) {
                    bestRow = candidateRow
                } else {
                    break
                }
            }
            compacted.add(tile.copy(row = bestRow))
        }

        return compacted
    }

    fun getMaxRow(tiles: List<TileModel>): Int {
        if (tiles.isEmpty()) return 0
        return tiles.maxOf { it.row + it.effectiveRows }
    }
}
