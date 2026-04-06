package org.oacp.android

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OacpParamsTest {

    @Test
    fun `getString returns exact match`() {
        val intent = Intent().apply { putExtra("query", "weather") }
        val params = OacpParams(intent)
        assertEquals("weather", params.getString("query"))
    }

    @Test
    fun `getString returns null for missing key`() {
        val intent = Intent()
        val params = OacpParams(intent)
        assertNull(params.getString("query"))
    }

    @Test
    fun `getString resolves suffix match with EXTRA prefix`() {
        val intent = Intent().apply {
            putExtra("com.example.app.EXTRA_QUERY", "test")
        }
        val params = OacpParams(intent)
        assertEquals("test", params.getString("query"))
    }

    @Test
    fun `getString resolves suffix match with extra dot prefix`() {
        val intent = Intent().apply {
            putExtra("org.oacp.extra.QUERY", "test2")
        }
        val params = OacpParams(intent)
        assertEquals("test2", params.getString("query"))
    }

    @Test
    fun `getInt returns integer value`() {
        val intent = Intent().apply { putExtra("count", 42) }
        val params = OacpParams(intent)
        assertEquals(42, params.getInt("count"))
    }

    @Test
    fun `getInt coerces string to int`() {
        val intent = Intent().apply { putExtra("count", "42") }
        val params = OacpParams(intent)
        assertEquals(42, params.getInt("count"))
    }

    @Test
    fun `getInt coerces long to int`() {
        val intent = Intent().apply { putExtra("count", 42L) }
        val params = OacpParams(intent)
        assertEquals(42, params.getInt("count"))
    }

    @Test
    fun `getInt coerces double to int`() {
        val intent = Intent().apply { putExtra("count", 42.7) }
        val params = OacpParams(intent)
        assertEquals(42, params.getInt("count"))
    }

    @Test
    fun `getInt returns null for non-numeric string`() {
        val intent = Intent().apply { putExtra("count", "abc") }
        val params = OacpParams(intent)
        assertNull(params.getInt("count"))
    }

    @Test
    fun `getLong returns long value`() {
        val intent = Intent().apply { putExtra("timestamp", 1234567890L) }
        val params = OacpParams(intent)
        assertEquals(1234567890L, params.getLong("timestamp"))
    }

    @Test
    fun `getLong coerces int to long`() {
        val intent = Intent().apply { putExtra("timestamp", 42) }
        val params = OacpParams(intent)
        assertEquals(42L, params.getLong("timestamp"))
    }

    @Test
    fun `getBoolean returns boolean value`() {
        val intent = Intent().apply { putExtra("verbose", true) }
        val params = OacpParams(intent)
        assertEquals(true, params.getBoolean("verbose"))
    }

    @Test
    fun `getBoolean coerces string true`() {
        val intent = Intent().apply { putExtra("verbose", "true") }
        val params = OacpParams(intent)
        assertEquals(true, params.getBoolean("verbose"))
    }

    @Test
    fun `getBoolean coerces string TRUE case insensitive`() {
        val intent = Intent().apply { putExtra("verbose", "TRUE") }
        val params = OacpParams(intent)
        assertEquals(true, params.getBoolean("verbose"))
    }

    @Test
    fun `getBoolean coerces int nonzero to true`() {
        val intent = Intent().apply { putExtra("verbose", 1) }
        val params = OacpParams(intent)
        assertEquals(true, params.getBoolean("verbose"))
    }

    @Test
    fun `getBoolean coerces int zero to false`() {
        val intent = Intent().apply { putExtra("verbose", 0) }
        val params = OacpParams(intent)
        assertEquals(false, params.getBoolean("verbose"))
    }

    @Test
    fun `getDouble returns double value`() {
        val intent = Intent().apply { putExtra("lat", 37.7749) }
        val params = OacpParams(intent)
        assertEquals(37.7749, params.getDouble("lat")!!, 0.0001)
    }

    @Test
    fun `getDouble coerces float to double`() {
        val intent = Intent().apply { putExtra("lat", 37.7f) }
        val params = OacpParams(intent)
        assertEquals(37.7, params.getDouble("lat")!!, 0.1)
    }

    @Test
    fun `getDouble coerces string to double`() {
        val intent = Intent().apply { putExtra("lat", "37.7749") }
        val params = OacpParams(intent)
        assertEquals(37.7749, params.getDouble("lat")!!, 0.0001)
    }

    @Test
    fun `getRaw returns uncoerced value`() {
        val intent = Intent().apply { putExtra("data", 42) }
        val params = OacpParams(intent)
        assertEquals(42, params.getRaw("data"))
    }

    @Test
    fun `has returns true for existing key`() {
        val intent = Intent().apply { putExtra("query", "test") }
        val params = OacpParams(intent)
        assertTrue(params.has("query"))
    }

    @Test
    fun `has returns false for missing key`() {
        val intent = Intent()
        val params = OacpParams(intent)
        assertFalse(params.has("query"))
    }

    @Test
    fun `has returns true for suffix-matched key`() {
        val intent = Intent().apply {
            putExtra("com.example.EXTRA_QUERY", "test")
        }
        val params = OacpParams(intent)
        assertTrue(params.has("query"))
    }

    @Test
    fun `keys excludes OACP standard extras`() {
        val intent = Intent().apply {
            putExtra("org.oacp.extra.REQUEST_ID", "abc")
            putExtra("query", "weather")
            putExtra("limit", 10)
        }
        val params = OacpParams(intent)
        val keys = params.keys()
        assertTrue(keys.contains("query"))
        assertTrue(keys.contains("limit"))
        assertFalse(keys.contains("org.oacp.extra.REQUEST_ID"))
    }

    @Test
    fun `keys returns empty set for empty intent`() {
        val intent = Intent()
        val params = OacpParams(intent)
        assertTrue(params.keys().isEmpty())
    }

    @Test
    fun `exact match takes priority over suffix match`() {
        val intent = Intent().apply {
            putExtra("query", "exact")
            putExtra("com.example.EXTRA_QUERY", "suffix")
        }
        val params = OacpParams(intent)
        assertEquals("exact", params.getString("query"))
    }
}
