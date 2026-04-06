package org.oacp.android

import android.content.Context
import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowApplication
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class OacpReceiverTest {

    private val appContext get() = ApplicationProvider.getApplicationContext<android.app.Application>()

    @Test
    fun `onAction receives correct action and params`() {
        val latch = CountDownLatch(1)
        var capturedAction: String? = null
        var capturedQuery: String? = null

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                capturedAction = action
                capturedQuery = params.getString("query")
                latch.countDown()
                return OacpResult.success("OK")
            }
        }

        val intent = Intent("com.example.ACTION_SEARCH").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-1")
            putExtra("query", "weather")
        }

        receiver.onReceive(appContext, intent)
        latch.await(2, TimeUnit.SECONDS)

        assertEquals("com.example.ACTION_SEARCH", capturedAction)
        assertEquals("weather", capturedQuery)
    }

    @Test
    fun `requestId is extracted from intent`() {
        val latch = CountDownLatch(1)
        var capturedRequestId: String? = null

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                capturedRequestId = requestId
                latch.countDown()
                return null
            }
        }

        val intent = Intent("com.example.ACTION_TEST").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-42")
        }

        receiver.onReceive(appContext, intent)
        latch.await(2, TimeUnit.SECONDS)

        assertEquals("req-42", capturedRequestId)
    }

    @Test
    fun `null action is ignored`() {
        var called = false

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                called = true
                return null
            }
        }

        receiver.onReceive(appContext, Intent())
        Thread.sleep(100)
        assertFalse("onAction should not be called for null action", called)
    }

    @Test
    fun `exception in onAction produces error result`() {
        val latch = CountDownLatch(1)

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                throw IllegalStateException("Something broke")
            }
        }

        val intent = Intent("com.example.ACTION_FAIL").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-err")
        }

        receiver.onReceive(appContext, intent)
        // Wait for async execution
        Thread.sleep(500)

        val shadow = ShadowApplication.getInstance()
        val broadcasts = shadow.broadcastIntents
            .filter { it.action == Oacp.ACTION_RESULT }

        assertTrue("Expected an error broadcast", broadcasts.isNotEmpty())
        val errorIntent = broadcasts.last()
        assertEquals(Oacp.STATUS_FAILED, errorIntent.getStringExtra(Oacp.EXTRA_STATUS))
        assertEquals(Oacp.ERROR_INTERNAL, errorIntent.getStringExtra(Oacp.EXTRA_ERROR_CODE))
        assertEquals("Something broke", errorIntent.getStringExtra(Oacp.EXTRA_ERROR_MESSAGE))
    }

    @Test
    fun `null result does not broadcast`() {
        val latch = CountDownLatch(1)

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                latch.countDown()
                return null // fire-and-forget
            }
        }

        val intent = Intent("com.example.ACTION_FIRE").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-1")
        }

        val beforeCount = ShadowApplication.getInstance().broadcastIntents
            .count { it.action == Oacp.ACTION_RESULT }

        receiver.onReceive(appContext, intent)
        latch.await(2, TimeUnit.SECONDS)
        Thread.sleep(100)

        val afterCount = ShadowApplication.getInstance().broadcastIntents
            .count { it.action == Oacp.ACTION_RESULT }

        assertEquals("No result broadcast expected for null return", beforeCount, afterCount)
    }

    @Test
    fun `no requestId means no result broadcast`() {
        val latch = CountDownLatch(1)

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                latch.countDown()
                return OacpResult.success("OK")
            }
        }

        val intent = Intent("com.example.ACTION_TEST") // no requestId

        val beforeCount = ShadowApplication.getInstance().broadcastIntents
            .count { it.action == Oacp.ACTION_RESULT }

        receiver.onReceive(appContext, intent)
        latch.await(2, TimeUnit.SECONDS)
        Thread.sleep(100)

        val afterCount = ShadowApplication.getInstance().broadcastIntents
            .count { it.action == Oacp.ACTION_RESULT }

        assertEquals("No broadcast when requestId is null", beforeCount, afterCount)
    }

    @Test
    fun `guessCapabilityId extracts from ACTION_ suffix`() {
        val latch = CountDownLatch(1)

        val receiver = object : OacpReceiver() {
            override fun onAction(
                context: Context,
                action: String,
                params: OacpParams,
                requestId: String?
            ): OacpResult? {
                latch.countDown()
                return OacpResult.success("OK")
            }
        }

        val intent = Intent("com.example.app.ACTION_PLAY_MUSIC").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-1")
        }

        receiver.onReceive(appContext, intent)
        latch.await(2, TimeUnit.SECONDS)
        Thread.sleep(200)

        val broadcast = ShadowApplication.getInstance().broadcastIntents
            .filter { it.action == Oacp.ACTION_RESULT }
            .last()

        assertEquals("play_music", broadcast.getStringExtra(Oacp.EXTRA_CAPABILITY_ID))
    }
}
