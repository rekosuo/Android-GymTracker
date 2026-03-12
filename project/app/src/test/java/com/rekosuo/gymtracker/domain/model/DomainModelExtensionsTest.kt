package com.rekosuo.gymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the domain extension functions in DomainModels.kt.
 */
class DomainModelExtensionsTest {

    // ========================================================================
    // toWeightRows() — converts flat SetEntry list to grouped WeightRows
    // ========================================================================

    @Test
    fun `toWeightRows - empty list returns empty`() {
        // Edge case: no sets at all. The function should handle this gracefully
        // rather than crashing on .first() or similar.
        val sets = emptyList<SetEntry>()
        val rows = sets.toWeightRows()
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `toWeightRows - single set becomes single row`() {
        // Simplest case: one set should produce one row with one rep entry.
        val sets = listOf(SetEntry(weight = 20f, reps = 10, order = 0))
        val rows = sets.toWeightRows()

        assertEquals(1, rows.size)
        assertEquals(20f, rows[0].weight)
        assertEquals(listOf(10), rows[0].reps)
        assertEquals(0, rows[0].startOrder)
    }

    @Test
    fun `toWeightRows - consecutive same weight groups into one row`() {
        // When a user does multiple sets at the same weight, they should appear
        // as a single row in the UI: "20kg | 10 | 10 | 8"
        val sets = listOf(
            SetEntry(weight = 20f, reps = 10, order = 0),
            SetEntry(weight = 20f, reps = 10, order = 1),
            SetEntry(weight = 20f, reps = 8, order = 2)
        )
        val rows = sets.toWeightRows()

        assertEquals(1, rows.size)
        assertEquals(20f, rows[0].weight)
        assertEquals(listOf(10, 10, 8), rows[0].reps)
        assertEquals(0, rows[0].startOrder)
    }

    @Test
    fun `toWeightRows - weight change creates new row`() {
        // When the user changes weight, a new row should start.
        val sets = listOf(
            SetEntry(weight = 20f, reps = 10, order = 0),
            SetEntry(weight = 25f, reps = 8, order = 1)
        )
        val rows = sets.toWeightRows()

        assertEquals(2, rows.size)
        assertEquals(20f, rows[0].weight)
        assertEquals(listOf(10), rows[0].reps)
        assertEquals(25f, rows[1].weight)
        assertEquals(listOf(8), rows[1].reps)
        assertEquals(1, rows[1].startOrder)
    }

    @Test
    fun `toWeightRows - returning to previous weight creates separate row`() {
        // This is the key behavior documented in the codebase:
        // 20kg, 20kg, 22kg, 20kg should produce 3 rows, NOT merge the 20kg rows.
        // This preserves the chronological order of the workout.
        val sets = listOf(
            SetEntry(weight = 20f, reps = 10, order = 0),
            SetEntry(weight = 20f, reps = 10, order = 1),
            SetEntry(weight = 22f, reps = 7, order = 2),
            SetEntry(weight = 20f, reps = 8, order = 3)
        )
        val rows = sets.toWeightRows()

        assertEquals(3, rows.size)
        // Row 1: 20kg with two reps
        assertEquals(20f, rows[0].weight)
        assertEquals(listOf(10, 10), rows[0].reps)
        assertEquals(0, rows[0].startOrder)
        // Row 2: 22kg with one rep
        assertEquals(22f, rows[1].weight)
        assertEquals(listOf(7), rows[1].reps)
        assertEquals(2, rows[1].startOrder)
        // Row 3: back to 20kg — separate row, not merged with row 1
        assertEquals(20f, rows[2].weight)
        assertEquals(listOf(8), rows[2].reps)
        assertEquals(3, rows[2].startOrder)
    }

    @Test
    fun `toWeightRows - sets are sorted by order before grouping`() {
        // Even if the list arrives out of order, the function should sort by
        // the 'order' field first. This matters because Room queries or list
        // manipulations might not guarantee order.
        val sets = listOf(
            SetEntry(weight = 25f, reps = 8, order = 2),
            SetEntry(weight = 20f, reps = 10, order = 0),
            SetEntry(weight = 20f, reps = 10, order = 1)
        )
        val rows = sets.toWeightRows()

        assertEquals(2, rows.size)
        assertEquals(20f, rows[0].weight)
        assertEquals(listOf(10, 10), rows[0].reps)
        assertEquals(25f, rows[1].weight)
        assertEquals(listOf(8), rows[1].reps)
    }

    // ========================================================================
    // toSets() — converts WeightRows back to flat SetEntry list
    // ========================================================================

    @Test
    fun `toSets - empty rows returns empty`() {
        val rows = emptyList<WeightRow>()
        val sets = rows.toSets()
        assertTrue(sets.isEmpty())
    }

    @Test
    fun `toSets - single row with multiple reps expands correctly`() {
        // A row like "20kg | 10 | 10 | 8" should become three separate SetEntry objects.
        val rows = listOf(WeightRow(weight = 20f, reps = listOf(10, 10, 8), startOrder = 0))
        val sets = rows.toSets()

        assertEquals(3, sets.size)
        // All sets should have the same weight
        assertTrue(sets.all { it.weight == 20f })
        // Reps should match
        assertEquals(listOf(10, 10, 8), sets.map { it.reps })
        // Orders should be recalculated as 0, 1, 2 (continuous)
        assertEquals(listOf(0, 1, 2), sets.map { it.order })
    }

    @Test
    fun `toSets - multiple rows get continuous order values`() {
        // Order values must be recalculated from 0, regardless of the original
        // startOrder values. This ensures no gaps after row deletions.
        val rows = listOf(
            WeightRow(weight = 20f, reps = listOf(10, 10), startOrder = 0),
            WeightRow(weight = 25f, reps = listOf(8), startOrder = 5) // gap in startOrder
        )
        val sets = rows.toSets()

        assertEquals(3, sets.size)
        assertEquals(listOf(0, 1, 2), sets.map { it.order }) // continuous, no gap
        assertEquals(20f, sets[0].weight)
        assertEquals(20f, sets[1].weight)
        assertEquals(25f, sets[2].weight)
    }

    @Test
    fun `toSets - row with empty reps produces no sets`() {
        // A row with no reps (user hasn't entered any yet) should not create
        // phantom SetEntry objects.
        val rows = listOf(
            WeightRow(weight = 20f, reps = emptyList(), startOrder = 0),
            WeightRow(weight = 25f, reps = listOf(8), startOrder = 0)
        )
        val sets = rows.toSets()

        assertEquals(1, sets.size)
        assertEquals(25f, sets[0].weight)
        assertEquals(0, sets[0].order) // order recalculated from 0
    }

    // ========================================================================
    // Round-trip: toWeightRows() -> toSets() and back
    // ========================================================================

    @Test
    fun `round trip - sets to rows and back preserves data`() {
        // This is a critical property: converting sets -> rows -> sets should
        // give back equivalent data (same weights and reps in the same order).
        // The order values will be renumbered but the sequence must be preserved.
        val originalSets = listOf(
            SetEntry(weight = 20f, reps = 10, order = 0),
            SetEntry(weight = 20f, reps = 10, order = 1),
            SetEntry(weight = 25f, reps = 8, order = 2),
            SetEntry(weight = 20f, reps = 12, order = 3)
        )

        val roundTripped = originalSets.toWeightRows().toSets()

        assertEquals(originalSets.size, roundTripped.size)
        for (i in originalSets.indices) {
            assertEquals(originalSets[i].weight, roundTripped[i].weight)
            assertEquals(originalSets[i].reps, roundTripped[i].reps)
            assertEquals(originalSets[i].order, roundTripped[i].order)
        }
    }

    // ========================================================================
    // nextStartOrder() — calculates where the next row should start
    // ========================================================================

    @Test
    fun `nextStartOrder - empty list returns 0`() {
        val rows = emptyList<WeightRow>()
        assertEquals(0, rows.nextStartOrder())
    }

    @Test
    fun `nextStartOrder - after one row with two reps returns 2`() {
        // If the last row starts at 0 and has 2 reps, next should be 2.
        val rows = listOf(WeightRow(weight = 20f, reps = listOf(10, 10), startOrder = 0))
        assertEquals(2, rows.nextStartOrder())
    }

    @Test
    fun `nextStartOrder - calculates from last row only`() {
        // Only the last row matters, not earlier rows.
        val rows = listOf(
            WeightRow(weight = 20f, reps = listOf(10, 10), startOrder = 0),
            WeightRow(weight = 25f, reps = listOf(8, 6, 5), startOrder = 2)
        )
        assertEquals(5, rows.nextStartOrder()) // 2 + 3 reps = 5
    }

    // ========================================================================
    // toGraphDataPoint() — transforms a Performance into graph data
    // ========================================================================

    @Test
    fun `toGraphDataPoint - computes all metrics correctly`() {
        // This function aggregates set data into a single data point for graphing.
        // We need to verify all the computed fields are correct.
        val performance = Performance(
            id = 1,
            exerciseId = 10,
            date = 1000L,
            sets = listOf(
                SetEntry(weight = 20f, reps = 10, order = 0),
                SetEntry(weight = 25f, reps = 8, order = 1),
                SetEntry(weight = 30f, reps = 6, order = 2)
            )
        )

        val dataPoint = performance.toGraphDataPoint()

        assertEquals(1L, dataPoint.performanceId)
        assertEquals(1000L, dataPoint.date)
        assertEquals(30f, dataPoint.maxWeight)      // max of 20, 25, 30
        assertEquals(20f, dataPoint.minWeight)      // min of 20, 25, 30
        assertEquals(25f, dataPoint.avgWeight)      // (20+25+30)/3 = 25
        assertEquals(3, dataPoint.totalSets)        // 3 sets
        assertEquals(24, dataPoint.totalReps)       // 10+8+6 = 24
    }

    @Test
    fun `toGraphDataPoint - empty sets produces zeroes`() {
        // A performance with no sets shouldn't crash — it should produce
        // zero values for all metrics.
        val performance = Performance(
            id = 1,
            exerciseId = 10,
            date = 1000L,
            sets = emptyList()
        )

        val dataPoint = performance.toGraphDataPoint()

        assertEquals(0f, dataPoint.maxWeight)
        assertEquals(0f, dataPoint.minWeight)
        assertEquals(0f, dataPoint.avgWeight)
        assertEquals(0, dataPoint.totalSets)
        assertEquals(0, dataPoint.totalReps)
    }

    @Test
    fun `toGraphDataPoint - single set has equal min max avg`() {
        val performance = Performance(
            id = 1,
            exerciseId = 10,
            date = 1000L,
            sets = listOf(SetEntry(weight = 50f, reps = 5, order = 0))
        )

        val dataPoint = performance.toGraphDataPoint()

        assertEquals(50f, dataPoint.maxWeight)
        assertEquals(50f, dataPoint.minWeight)
        assertEquals(50f, dataPoint.avgWeight)
    }

    // ========================================================================
    // filterByTime() — filters graph data points by month range
    // ========================================================================

    @Test
    fun `filterByTime - null months returns all points`() {
        // null means "all time" — no filtering applied.
        val points = listOf(
            GraphDataPoint(1, date = 0L, 100f, 50f, 75f, 3, 24),
            GraphDataPoint(2, date = 1000L, 120f, 60f, 90f, 4, 30)
        )

        val filtered = points.filterByTime(null)
        assertEquals(2, filtered.size)
    }

    @Test
    fun `filterByTime - filters out old data points`() {
        val now = System.currentTimeMillis()
        val twoMonthsAgo = now - (2L * 30 * 24 * 60 * 60 * 1000)
        val sixMonthsAgo = now - (6L * 30 * 24 * 60 * 60 * 1000)

        val points = listOf(
            GraphDataPoint(1, date = sixMonthsAgo, 100f, 50f, 75f, 3, 24),
            GraphDataPoint(2, date = twoMonthsAgo, 120f, 60f, 90f, 4, 30),
            GraphDataPoint(3, date = now, 130f, 70f, 100f, 5, 35)
        )

        // Filter to last 3 months — should exclude the 6-month-old point
        val filtered = points.filterByTime(3)
        assertEquals(2, filtered.size)
        assertEquals(2L, filtered[0].performanceId)
        assertEquals(3L, filtered[1].performanceId)
    }

    @Test
    fun `filterByTime - 1 month filters tightly`() {
        val now = System.currentTimeMillis()
        val twoMonthsAgo = now - (2L * 30 * 24 * 60 * 60 * 1000)

        val points = listOf(
            GraphDataPoint(1, date = twoMonthsAgo, 100f, 50f, 75f, 3, 24),
            GraphDataPoint(2, date = now, 130f, 70f, 100f, 5, 35)
        )

        val filtered = points.filterByTime(1)
        assertEquals(1, filtered.size)
        assertEquals(2L, filtered[0].performanceId)
    }
}