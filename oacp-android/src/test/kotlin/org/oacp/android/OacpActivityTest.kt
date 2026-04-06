package org.oacp.android

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowApplication

/** Concrete subclass for testing — exposes protected fields. */
class TestOacpActivity : OacpActivity() {
    var lastAction: String? = null
    var lastParams: OacpParams? = null
    var onOacpActionCallCount = 0

    // Expose protected members for test assertions
    val testRequestId: String? get() = requestId
    val testCapabilityId: String? get() = capabilityId
    val testParams: OacpParams get() = params
    fun testSendResult(result: OacpResult) = sendResult(result)

    override fun onOacpAction(action: String, params: OacpParams) {
        lastAction = action
        lastParams = params
        onOacpActionCallCount++
    }
}

@RunWith(RobolectricTestRunner::class)
class OacpActivityTest {

    @Test
    fun `onCreate with OACP intent calls onOacpAction`() {
        val intent = Intent("com.example.ACTION_SEARCH").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-1")
            putExtra("query", "weather")
        }

        val activity = Robolectric.buildActivity(TestOacpActivity::class.java, intent)
            .create()
            .get()

        assertEquals("com.example.ACTION_SEARCH", activity.lastAction)
        assertEquals(1, activity.onOacpActionCallCount)
        assertEquals("weather", activity.lastParams?.getString("query"))
    }

    @Test
    fun `requestId is extracted from intent`() {
        val intent = Intent("com.example.ACTION_TEST").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-42")
        }

        val activity = Robolectric.buildActivity(TestOacpActivity::class.java, intent)
            .create()
            .get()

        assertEquals("req-42", activity.testRequestId)
    }

    @Test
    fun `capabilityId derived from ACTION_ suffix`() {
        val intent = Intent("com.example.app.ACTION_PLAY_MUSIC")

        val activity = Robolectric.buildActivity(TestOacpActivity::class.java, intent)
            .create()
            .get()

        assertEquals("play_music", activity.testCapabilityId)
    }

    @Test
    fun `params initialized safely when launched without action`() {
        val intent = Intent() // no action

        val activity = Robolectric.buildActivity(TestOacpActivity::class.java, intent)
            .create()
            .get()

        // params should be accessible without UninitializedPropertyAccessException
        assertNotNull(activity.testParams)
        assertEquals(0, activity.onOacpActionCallCount)
    }

    @Test
    fun `sendResult broadcasts when requestId present`() {
        val intent = Intent("com.example.ACTION_TEST").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-1")
        }

        val activity = Robolectric.buildActivity(TestOacpActivity::class.java, intent)
            .create()
            .get()

        activity.testSendResult(OacpResult.success("Done"))

        val shadow = ShadowApplication.getInstance()
        val broadcasts = shadow.broadcastIntents.filter { it.action == Oacp.ACTION_RESULT }
        assertEquals(1, broadcasts.size)
        assertEquals("req-1", broadcasts[0].getStringExtra(Oacp.EXTRA_REQUEST_ID))
    }

    @Test
    fun `sendResult does nothing when requestId is null`() {
        val intent = Intent("com.example.ACTION_TEST") // no requestId

        val activity = Robolectric.buildActivity(TestOacpActivity::class.java, intent)
            .create()
            .get()

        val beforeCount = ShadowApplication.getInstance().broadcastIntents
            .count { it.action == Oacp.ACTION_RESULT }

        activity.testSendResult(OacpResult.success("Done"))

        val afterCount = ShadowApplication.getInstance().broadcastIntents
            .count { it.action == Oacp.ACTION_RESULT }

        assertEquals("No broadcast when requestId is null", beforeCount, afterCount)
    }

    @Test
    fun `onNewIntent replaces requestId and params`() {
        val intent1 = Intent("com.example.ACTION_A").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-1")
            putExtra("query", "first")
        }
        val intent2 = Intent("com.example.ACTION_B").apply {
            putExtra(Oacp.EXTRA_REQUEST_ID, "req-2")
            putExtra("query", "second")
        }

        val controller = Robolectric.buildActivity(TestOacpActivity::class.java, intent1)
            .create()
        val activity = controller.get()

        assertEquals("req-1", activity.testRequestId)
        assertEquals("first", activity.testParams.getString("query"))

        controller.newIntent(intent2)

        assertEquals("req-2", activity.testRequestId)
        assertEquals("second", activity.testParams.getString("query"))
        assertEquals(2, activity.onOacpActionCallCount)
    }
}
