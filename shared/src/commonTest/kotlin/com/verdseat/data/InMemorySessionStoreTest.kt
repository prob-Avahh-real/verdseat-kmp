package com.verdseat.data

import com.verdseat.model.*
import kotlin.test.*

/// InMemorySessionStore 测试
class InMemorySessionStoreTest {

    @Test
    fun `adds and counts records`() {
        val store = InMemorySessionStore(maxSize = 100)
        repeat(5) { store.add(emptySnapshot(it.toLong())) }
        assertEquals(5, store.count())
    }

    @Test
    fun `evicts oldest when full`() {
        val store = InMemorySessionStore(maxSize = 3)
        store.add(emptySnapshot(1)); store.add(emptySnapshot(2))
        store.add(emptySnapshot(3)); store.add(emptySnapshot(4))
        assertEquals(3, store.count())
        assertTrue(store.recentSnapshots(10).none { it.timestamp == 1L })
    }

    @Test
    fun `recent snapshots returns correct order`() {
        val store = InMemorySessionStore()
        store.add(emptySnapshot(10)); store.add(emptySnapshot(20))
        val recent = store.recentSnapshots(5)
        assertEquals(2, recent.size)
        assertTrue(recent[0].timestamp <= recent[1].timestamp)
    }

    @Test
    fun `today filter excludes yesterday`() {
        val store = InMemorySessionStore()
        val yesterday = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - 86_400_001L
        store.add(emptySnapshot(yesterday))
        assertTrue(store.todaySnapshots().isEmpty())
    }

    @Test
    fun `clear empties everything`() {
        val store = InMemorySessionStore()
        repeat(10) { store.add(emptySnapshot(it.toLong())) }
        store.clear()
        assertEquals(0, store.count())
        assertTrue(store.todaySnapshots().isEmpty())
        assertTrue(store.recentAnalyses(10).isEmpty())
    }

    private fun emptySnapshot(ts: Long) = PressureSnapshot(
        timestamp = ts,
        grid = (0 until 12).map { PressurePoint(it / 3, it % 3, 0.5) }
    )
}
