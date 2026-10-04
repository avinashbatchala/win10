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
     * Resizes a tile while keeping its top-left anchor whenever possible.
     *
     * Unlike [moveTile]/[repackGrid], resizing deliberately does NOT run a global
     * compaction: shrinking a tile must not pull unrelated tiles upward, and
     * enlarging one must only displace the tiles that actually conflict with it.
     */
    fun resizeTile(
        tileId: String,
        newSize: TileSize,
        currentTiles: List<TileModel>,
        totalColumns: Int = 6
    ): List<TileModel> {
        val target = currentTiles.firstOrNull { it.id == tileId } ?: return currentTiles
        val otherTiles = currentTiles.filter { it.id != tileId }

        // 1. Resized target keeps its top-left, clamped horizontally so the new
        //    width fits inside the grid.
        val newCol = target.col.coerceIn(0, (totalColumns - newSize.cols).coerceAtLeast(0))
        val newRow = target.row.coerceAtLeast(0)
        val updatedTarget = target.copy(size = newSize, col = newCol, row = newRow)

        // 2. Resolve the rest in stable order. A tile is left untouched unless it
        //    overlaps the resized tile or an already-relocated tile.
        val resolved = mutableListOf(updatedTarget)
        for (other in otherTiles) {
            val conflicts = resolved.any { placed ->
                overlaps(
                    placed.col, placed.row, placed.effectiveCols, placed.effectiveRows,
                    other.col, other.row, other.effectiveCols, other.effectiveRows
                )
            }

            if (!conflicts) {
                resolved.add(other)
            } else {
                // Prefer staying near the original column but below the resized tile.
                val nextPos = findNearestValidPlacement(
                    preferredCol = other.col,
                    preferredRow = updatedTarget.row + updatedTarget.effectiveRows,
                    cols = other.effectiveCols,
                    rows = other.effectiveRows,
                    existingTiles = resolved,
                    totalColumns = totalColumns,
                    excludeTileId = other.id
                )
                resolved.add(other.copy(col = nextPos.first, row = nextPos.second))
            }
        }

        return resolved
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

    /**
     * Repacks the grid deterministically when the column count changes (e.g. 6 <-> 8).
     * Preserves pinned applications and tile sizes, avoids overlaps,
     * finds the nearest valid placement for invalid coordinates, and compacts.
     */
    fun repackGrid(currentTiles: List<TileModel>, newTotalColumns: Int): List<TileModel> {
        val sorted = currentTiles.sortedWith(compareBy({ it.row }, { it.col }, { it.order }))
        val repacked = mutableListOf<TileModel>()

        for (tile in sorted) {
            val targetCol = if (tile.col + tile.effectiveCols > newTotalColumns) {
                (newTotalColumns - tile.effectiveCols).coerceAtLeast(0)
            } else {
                tile.col
            }
            val targetRow = tile.row

            val pos = findNearestValidPlacement(
                preferredCol = targetCol,
                preferredRow = targetRow,
                cols = tile.effectiveCols,
                rows = tile.effectiveRows,
                existingTiles = repacked,
                totalColumns = newTotalColumns,
                excludeTileId = tile.id
            )
            repacked.add(tile.copy(col = pos.first, row = pos.second))
        }

        return compactGrid(repacked, newTotalColumns)
    }

    fun getMaxRow(tiles: List<TileModel>): Int {
        if (tiles.isEmpty()) return 0
        return tiles.maxOf { it.row + it.effectiveRows }
    }
}
