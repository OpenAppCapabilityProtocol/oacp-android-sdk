package org.oacp.android

import org.junit.Assert.*
import org.junit.Test

class OacpEntityTest {

    @Test
    fun `entity with required fields only`() {
        val entity = OacpEntity(id = "1", displayName = "Work Alarm")
        assertEquals("1", entity.id)
        assertEquals("Work Alarm", entity.displayName)
        assertTrue(entity.aliases.isEmpty())
        assertNull(entity.description)
        assertTrue(entity.keywords.isEmpty())
        assertTrue(entity.metadata.isEmpty())
    }

    @Test
    fun `entity with all fields`() {
        val entity = OacpEntity(
            id = "work",
            displayName = "Work Alarm",
            aliases = listOf("office", "morning"),
            description = "Weekday 6:30am",
            keywords = listOf("weekday", "morning"),
            metadata = mapOf("hour" to 6, "minute" to 30)
        )
        assertEquals("work", entity.id)
        assertEquals("Work Alarm", entity.displayName)
        assertEquals(listOf("office", "morning"), entity.aliases)
        assertEquals("Weekday 6:30am", entity.description)
        assertEquals(listOf("weekday", "morning"), entity.keywords)
        assertEquals(6, entity.metadata["hour"])
        assertEquals(30, entity.metadata["minute"])
    }

    @Test
    fun `entity data class equality`() {
        val a = OacpEntity(id = "1", displayName = "A")
        val b = OacpEntity(id = "1", displayName = "A")
        assertEquals(a, b)
    }

    @Test
    fun `entity data class copy`() {
        val original = OacpEntity(id = "1", displayName = "Original")
        val modified = original.copy(displayName = "Modified")
        assertEquals("1", modified.id)
        assertEquals("Modified", modified.displayName)
    }
}
