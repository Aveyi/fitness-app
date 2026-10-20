package ru.pacani.fitnessdiary.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressChartScaleTest {

    @Test
    fun axisTicks_roundStep() {
        assertEquals(listOf(0, 5, 10, 15), chartAxisTicks(14))   // как в макете Статистики
        assertEquals(listOf(0, 5, 10, 15), chartAxisTicks(15))
        assertEquals(listOf(0, 10, 20, 30), chartAxisTicks(30))
        assertEquals(listOf(0, 50, 100), chartAxisTicks(100))
    }

    @Test
    fun axisTicks_smallValues() {
        assertEquals(listOf(0, 1), chartAxisTicks(1))
        assertEquals(listOf(0, 1, 2, 3), chartAxisTicks(3))
        assertEquals(listOf(0, 2, 4), chartAxisTicks(4))
    }

    @Test
    fun axisTicks_topCoversMax() {
        (1..500).forEach { max ->
            val ticks = chartAxisTicks(max)
            assertTrue("max=$max ticks=$ticks", ticks.last() >= max)
            assertTrue("max=$max ticks=$ticks", ticks.size in 2..5)
        }
    }

    @Test
    fun labelIndices_fewPoints_allLabeled() {
        assertEquals(listOf(0, 1, 2, 3), chartLabelIndices(4))
        assertEquals(emptyList<Int>(), chartLabelIndices(0))
    }

    @Test
    fun labelIndices_manyPoints_firstLastAndBetween() {
        assertEquals(listOf(0, 4, 8, 11), chartLabelIndices(12)) // как в макете Статистики
        assertEquals(listOf(0, 2, 4), chartLabelIndices(5))
        assertEquals(listOf(0, 9, 18, 26), chartLabelIndices(27))
    }

    @Test
    fun labelIndices_neverMoreThanMax() {
        (1..60).forEach { count ->
            val indices = chartLabelIndices(count)
            assertTrue("count=$count indices=$indices", indices.size <= 4)
            assertEquals(count - 1, indices.last())
            assertEquals(0, indices.first())
        }
    }
}
