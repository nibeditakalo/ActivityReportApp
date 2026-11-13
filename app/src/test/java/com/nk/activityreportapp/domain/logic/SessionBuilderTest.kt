package com.nk.activityreportapp.domain.logic

import com.nk.activityreportapp.domain.model.UsageEvent
import com.nk.activityreportapp.domain.model.UsageEventType
import com.nk.activityreportapp.domain.model.UsageSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBuilderTest {
    private val builder = SessionBuilder()

    @Test
    fun `single session`() {
        val events = listOf(
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 1000L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_PAUSED, 2000L)
        )
        val sessions = builder.build(events, 3000L)
        assertEquals(1, sessions.size)
        assertEquals(UsageSession("pkg1", 1000L, 2000L), sessions[0])
    }

    @Test
    fun `merged sessions within gap`() {
        val events = listOf(
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 1000L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_PAUSED, 2000L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 2500L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_PAUSED, 3500L)
        )
        // Default gap is 1500ms. Gap here is 2500 - 2000 = 500ms.
        val sessions = builder.build(events, 4000L)
        assertEquals(1, sessions.size)
        assertEquals(UsageSession("pkg1", 1000L, 3500L), sessions[0])
    }

    @Test
    fun `separate sessions beyond gap`() {
        val events = listOf(
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 1000L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_PAUSED, 2000L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 4000L),
            UsageEvent("pkg1", UsageEventType.ACTIVITY_PAUSED, 5000L)
        )
        // Gap is 4000 - 2000 = 2000ms > 1500ms.
        val sessions = builder.build(events, 6000L)
        assertEquals(2, sessions.size)
        assertEquals(UsageSession("pkg1", 1000L, 2000L), sessions[0])
        assertEquals(UsageSession("pkg1", 4000L, 5000L), sessions[1])
    }

    @Test
    fun `switching packages closes previous session`() {
        val events = listOf(
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 1000L),
            UsageEvent("pkg2", UsageEventType.ACTIVITY_RESUMED, 2000L),
            UsageEvent("pkg2", UsageEventType.ACTIVITY_PAUSED, 3000L)
        )
        val sessions = builder.build(events, 4000L)
        assertEquals(2, sessions.size)
        assertEquals(UsageSession("pkg1", 1000L, 2000L), sessions[0])
        assertEquals(UsageSession("pkg2", 2000L, 3000L), sessions[1])
    }

    @Test
    fun `screen off closes session`() {
        val events = listOf(
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 1000L),
            UsageEvent("pkg1", UsageEventType.SCREEN_OFF, 2000L)
        )
        val sessions = builder.build(events, 3000L)
        assertEquals(1, sessions.size)
        assertEquals(UsageSession("pkg1", 1000L, 2000L), sessions[0])
    }

    @Test
    fun `session remains open until window end`() {
        val events = listOf(
            UsageEvent("pkg1", UsageEventType.ACTIVITY_RESUMED, 1000L)
        )
        val sessions = builder.build(events, 5000L)
        assertEquals(1, sessions.size)
        assertEquals(UsageSession("pkg1", 1000L, 5000L), sessions[0])
    }
}
