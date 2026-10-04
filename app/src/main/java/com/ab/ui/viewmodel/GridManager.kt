package com.ab.ui.viewmodel

import com.ab.model.TileModel
import com.ab.model.TileSize

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
     * Finds the nearest valid non-overlapping placement to (preferredCol, preferredRow).
     */
    fun findNearestValidPlacement(
        preferredCol: Int,
        preferredRow: Int,
        cols: Int,
        rows: Int,
        existingTiles: List<TileModel>,
        totalColumns: Int = 6,
        excludeTileId: String? = null
    ): Pair<Int, Int> {
        val clampedCol = preferredCol.coerceIn(0, (totalColumns - cols).coerceAtLeast(0))
        val clampedRow = preferredRow.coerceAtLeast(0)

        if (!isPositionOccupied(clampedCol, clampedRow, cols, rows, existingTiles, excludeTileId)) {
            return Pair(clampedCol, clampedRow)
        }

        var bestPos = Pair(clampedCol, clampedRow)
        var minDistance = Double.MAX_VALUE

        val maxSearchRow = (existingTiles.maxOfOrNull { it.row + it.effectiveRows } ?: 0) + 4
        for (r in 0..maxSearchRow) {
            for (c in 0..(totalColumns - cols)) {
                if (!isPositionOccupied(c, r, cols, rows, existingTiles, excludeTileId)) {
                    val dc = (c - clampedCol).toDouble()
                    val dr = (r - clampedRow).toDouble()
                    val dist = (dc * dc) + (if (dr >= 0) dr * dr * 2.0 else dr * dr * 5.0)
                    if (dist < minDistance) {
                        minDistance = dist
                        bestPos = Pair(c, r)
                    }
                }
            }
        }

        if (minDistance < Double.MAX_VALUE) {
            return bestPos
        }

        val nextRow = getMaxRow(existingTiles)
        return Pair(0, nextRow)
    }

    /**
     * Resizes a tile, finding the nearest valid placement that avoids collisions
     * and preserving exact grid alignment.
     */
    fun resizeTile(
        tileId: String,
        newSize: TileSize,
        currentTiles: List<TileModel>,
        totalColumns: Int = 6
    ): List<TileModel> {
        val target = currentTiles.firstOrNull { it.id == tileId } ?: return currentTiles
        val otherTiles = currentTiles.filter { it.id != tileId }

        var newCol = target.col
        if (newCol + newSize.cols > totalColumns) {
            newCol = (totalColumns - newSize.cols).coerceAtLeast(0)
        }
        val newRow = target.row

        // If fits without collision, use this exact position
        if (!isPositionOccupied(newCol, newRow, newSize.cols, newSize.rows, otherTiles)) {
            val updatedTarget = target.copy(size = newSize, col = newCol, row = newRow)
            return compactGrid(otherTiles + updatedTarget, totalColumns)
        }

        // If collides, find the nearest valid non-overlapping placement
        val nearestPlacement = findNearestValidPlacement(
            preferredCol = newCol,
            preferredRow = newRow,
            cols = newSize.cols,
            rows = newSize.rows,
            existingTiles = otherTiles,
            totalColumns = totalColumns,
            excludeTileId = tileId
        )

        val updatedTarget = target.copy(size = newSize, col = nearestPlacement.first, row = nearestPlacement.second)
        return compactGrid(otherTiles + updatedTarget, totalColumns)
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
                val nextPos = findNearestValidPlacement(
                    preferredCol = other.col,
                    preferredRow = clampedRow + target.effectiveRows,
                    cols = other.effectiveCols,
                    rows = other.effectiveRows,
                    existingTiles = resolvedTiles,
                    totalColumns = totalColumns
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
     * while preserving relative layout order and never allowing overlapping tiles.
     */
    fun compactGrid(tiles: List<TileModel>, totalColumns: Int = 6): List<TileModel> {
        val sorted = tiles.sortedWith(compareBy({ it.row }, { it.col }, { it.order }))
        val compacted = mutableListOf<TileModel>()

        for (tile in sorted) {
            var bestRow = tile.row
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
