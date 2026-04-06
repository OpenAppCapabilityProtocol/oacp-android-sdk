package org.oacp.android

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowApplication
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
class OacpResultTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun `success sets completed status`() {
        val result = OacpResult.success("Done")
        result.send(context, "req-1", "play_music")

        val intent = lastBroadcast()
        assertEquals(Oacp.STATUS_COMPLETED, intent.getStringExtra(Oacp.EXTRA_STATUS))
    }

    @Test
    fun `success includes message`() {
        val result = OacpResult.success("Playing music")
        result.send(context, "req-1", "play_music")

        val intent = lastBroadcast()
        assertEquals("Playing music", intent.getStringExtra(Oacp.EXTRA_MESSAGE))
    }

    @Test
    fun `success with data serializes as JSON`() {
        val result = OacpResult.success("Result", mapOf("temp" to 22, "unit" to "celsius"))
        result.send(context, "req-1", "get_weather")

        val intent = lastBroadcast()
        val json = intent.getStringExtra(Oacp.EXTRA_RESULT)
        assertNotNull("EXTRA_RESULT should be set when data is provided", json)
        assertTrue(json!!.contains("\"temp\""))
        assertTrue(json.contains("22"))
        assertTrue(json.contains("\"celsius\""))
    }

    @Test
    fun `success without data omits EXTRA_RESULT`() {
        val result = OacpResult.success("Result")
        result.send(context, "req-1", "get_weather")

        val intent = lastBroadcast()
        assertNull(intent.getStringExtra(Oacp.EXTRA_RESULT))
    }

    @Test
    fun `accepted sets accepted status`() {
        val result = OacpResult.accepted("Processing...")
        result.send(context, "req-1", "search")

        val intent = lastBroadcast()
        assertEquals(Oacp.STATUS_ACCEPTED, intent.getStringExtra(Oacp.EXTRA_STATUS))
        assertEquals("Processing...", intent.getStringExtra(Oacp.EXTRA_MESSAGE))
    }

    @Test
    fun `started sets started status`() {
        val result = OacpResult.started()
        result.send(context, "req-1", "download")

        val intent = lastBroadcast()
        assertEquals(Oacp.STATUS_STARTED, intent.getStringExtra(Oacp.EXTRA_STATUS))
    }

    @Test
    fun `error sets failed status and error code`() {
        val result = OacpResult.error("not_found", "Article not found")
        result.send(context, "req-1", "search")

        val intent = lastBroadcast()
        assertEquals(Oacp.STATUS_FAILED, intent.getStringExtra(Oacp.EXTRA_STATUS))
        assertEquals("not_found", intent.getStringExtra(Oacp.EXTRA_ERROR_CODE))
        assertEquals("Article not found", intent.getStringExtra(Oacp.EXTRA_ERROR_MESSAGE))
        assertEquals("false", intent.getStringExtra(Oacp.EXTRA_ERROR_RETRYABLE))
    }

    @Test
    fun `error with retryable flag`() {
        val result = OacpResult.error("network_unavailable", "No connection", retryable = true)
        result.send(context, "req-1", "search")

        val intent = lastBroadcast()
        assertEquals("true", intent.getStringExtra(Oacp.EXTRA_ERROR_RETRYABLE))
    }

    @Test
    fun `cancelled sets cancelled status`() {
        val result = OacpResult.cancelled("User dismissed")
        result.send(context, "req-1", "capture")

        val intent = lastBroadcast()
        assertEquals(Oacp.STATUS_CANCELLED, intent.getStringExtra(Oacp.EXTRA_STATUS))
        assertEquals("User dismissed", intent.getStringExtra(Oacp.EXTRA_MESSAGE))
    }

    @Test
    fun `send includes request ID and capability ID`() {
        val result = OacpResult.success("OK")
        result.send(context, "req-42", "toggle_flash")

        val intent = lastBroadcast()
        assertEquals("req-42", intent.getStringExtra(Oacp.EXTRA_REQUEST_ID))
        assertEquals("toggle_flash", intent.getStringExtra(Oacp.EXTRA_CAPABILITY_ID))
    }

    @Test
    fun `send includes source package`() {
        val result = OacpResult.success("OK")
        result.send(context, "req-1", "test")

        val intent = lastBroadcast()
        assertEquals(context.packageName, intent.getStringExtra(Oacp.EXTRA_SOURCE_PACKAGE))
    }

    @Test
    fun `send broadcasts ACTION_RESULT`() {
        val result = OacpResult.success("OK")
        result.send(context, "req-1", "test")

        val intent = lastBroadcast()
        assertEquals(Oacp.ACTION_RESULT, intent.action)
    }

    @Test
    fun `send with targetPackage sets package on intent`() {
        val result = OacpResult.success("OK")
        result.send(context, "req-1", "test", targetPackage = "com.oacp.hark")

        val intent = lastBroadcast()
        assertEquals("com.oacp.hark", intent.`package`)
    }

    @Test
    fun `send without targetPackage leaves package null`() {
        val result = OacpResult.success("OK")
        result.send(context, "req-1", "test")

        val intent = lastBroadcast()
        assertNull(intent.`package`)
    }

    private fun lastBroadcast(): Intent {
        val shadow = ShadowApplication.getInstance()
        val intents = shadow.broadcastIntents
        assertTrue("Expected at least one broadcast", intents.isNotEmpty())
        return intents.last()
    }
}
