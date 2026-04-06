package org.oacp.android

import android.net.Uri
import android.os.Looper
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OacpProviderTest {

    private lateinit var provider: OacpProvider
    private lateinit var context: Context
    private lateinit var authority: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authority = context.packageName + Oacp.PROVIDER_AUTHORITY_SUFFIX

        provider = Robolectric.setupContentProvider(
            OacpProvider::class.java,
            authority
        )
    }

    @After
    fun tearDown() {
        OacpProvider.setEntitySource(null)
    }

    // --- MIME Types ---

    @Test
    fun `getType returns application json for manifest`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_MANIFEST}")
        assertEquals("application/json", provider.getType(uri))
    }

    @Test
    fun `getType returns text markdown for context`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_CONTEXT}")
        assertEquals("text/markdown", provider.getType(uri))
    }

    @Test
    fun `getType returns application json for entities`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/alarm")
        assertEquals("application/json", provider.getType(uri))
    }

    @Test
    fun `getType returns null for unknown path`() {
        val uri = Uri.parse("content://$authority/unknown")
        assertNull(provider.getType(uri))
    }

    // --- Read-Only Enforcement ---

    @Test(expected = UnsupportedOperationException::class)
    fun `insert throws UnsupportedOperationException`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_MANIFEST}")
        provider.insert(uri, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `update throws UnsupportedOperationException`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_MANIFEST}")
        provider.update(uri, null, null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `delete throws UnsupportedOperationException`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_MANIFEST}")
        provider.delete(uri, null, null)
    }

    @Test
    fun `query returns null`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_MANIFEST}")
        assertNull(provider.query(uri, null, null, null, null))
    }

    // --- openFile rejects write mode ---

    @Test(expected = FileNotFoundException::class)
    fun `openFile rejects write mode`() {
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_MANIFEST}")
        provider.openFile(uri, "w")
    }

    // --- Unknown URI ---

    @Test(expected = FileNotFoundException::class)
    fun `openFile throws for unknown URI`() {
        val uri = Uri.parse("content://$authority/nonexistent")
        provider.openFile(uri, "r")
    }

    // --- Entity Source ---

    @Test(expected = FileNotFoundException::class)
    fun `openFile entities throws when no entity source registered`() {
        OacpProvider.setEntitySource(null)
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/alarm")
        provider.openFile(uri, "r")
    }

    @Test
    fun `entity source can be registered and replaced`() {
        val source1 = object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                return listOf(OacpEntity(id = "1", displayName = "Source 1"))
            }
        }
        val source2 = object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                return listOf(OacpEntity(id = "2", displayName = "Source 2"))
            }
        }

        OacpProvider.setEntitySource(source1)
        OacpProvider.setEntitySource(source2)
        // No exception — replacement is allowed
        OacpProvider.setEntitySource(null)
        // No exception — clearing is allowed
    }

    @Test
    fun `entities endpoint serves registered entity source`() {
        OacpProvider.setEntitySource(object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                if (entityType != "alarm") return emptyList()
                return listOf(
                    OacpEntity(
                        id = "work",
                        displayName = "Work Alarm",
                        aliases = listOf("morning alarm"),
                        description = "Weekday 6:30am",
                        keywords = listOf("weekday"),
                        metadata = mapOf("hour" to 6, "minute" to 30)
                    )
                )
            }
        })

        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/alarm")
        val pfd = provider.openFile(uri, "r")
        assertNotNull(pfd)

        // openPipeHelper writes asynchronously — flush the looper so the pipe is populated
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)

        val json = android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use {
            it.bufferedReader(Charsets.UTF_8).readText()
        }

        val obj = org.json.JSONObject(json)
        assertEquals("alarm", obj.getString("entityType"))

        val items = obj.getJSONArray("items")
        assertEquals(1, items.length())

        val entity = items.getJSONObject(0)
        assertEquals("work", entity.getString("id"))
        assertEquals("Work Alarm", entity.getString("displayName"))
        assertEquals("Weekday 6:30am", entity.getString("description"))
        assertEquals("morning alarm", entity.getJSONArray("aliases").getString(0))
        assertEquals("weekday", entity.getJSONArray("keywords").getString(0))
        assertEquals(6, entity.getJSONObject("metadata").getInt("hour"))
        assertEquals(30, entity.getJSONObject("metadata").getInt("minute"))
    }

    @Test
    fun `entities endpoint returns empty items for unknown entity type`() {
        OacpProvider.setEntitySource(object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                return emptyList()
            }
        })

        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/unknown_type")
        val pfd = provider.openFile(uri, "r")
        assertNotNull(pfd)

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)

        val json = android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use {
            it.bufferedReader(Charsets.UTF_8).readText()
        }

        val obj = org.json.JSONObject(json)
        assertEquals("unknown_type", obj.getString("entityType"))
        assertEquals(0, obj.getJSONArray("items").length())
    }

    @Test
    fun `entities endpoint passes query parameter`() {
        var capturedQuery: String? = "not-set"

        OacpProvider.setEntitySource(object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                capturedQuery = query
                return emptyList()
            }
        })

        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/alarm?q=work")
        provider.openFile(uri, "r")

        assertEquals("work", capturedQuery)
    }

    @Test
    fun `entities endpoint passes null query when absent`() {
        var capturedQuery: String? = "not-set"

        OacpProvider.setEntitySource(object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                capturedQuery = query
                return emptyList()
            }
        })

        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/alarm")
        provider.openFile(uri, "r")

        assertNull(capturedQuery)
    }

    @Test(expected = FileNotFoundException::class)
    fun `entities endpoint throws for missing entity type segment`() {
        OacpProvider.setEntitySource(object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                return emptyList()
            }
        })

        // This URI matches the entities/* pattern but has no last path segment
        // In practice UriMatcher won't match bare /entities, so this should fall through to unknown
        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}")
        provider.openFile(uri, "r")
    }

    @Test
    fun `entity without optional fields omits them from JSON`() {
        OacpProvider.setEntitySource(object : OacpEntitySource {
            override fun queryEntities(context: Context, entityType: String, query: String?): List<OacpEntity> {
                return listOf(OacpEntity(id = "1", displayName = "Simple"))
            }
        })

        val uri = Uri.parse("content://$authority/${Oacp.PROVIDER_PATH_ENTITIES}/item")
        val pfd = provider.openFile(uri, "r")

        Shadows.shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(50)

        val json = android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use {
            it.bufferedReader(Charsets.UTF_8).readText()
        }

        val entity = org.json.JSONObject(json).getJSONArray("items").getJSONObject(0)
        assertEquals("1", entity.getString("id"))
        assertEquals("Simple", entity.getString("displayName"))
        assertFalse(entity.has("aliases"))
        assertFalse(entity.has("description"))
        assertFalse(entity.has("keywords"))
        assertFalse(entity.has("metadata"))
    }
}
