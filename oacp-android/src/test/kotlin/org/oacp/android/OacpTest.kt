package org.oacp.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OacpTest {

    @Test
    fun `version matches protocol v0_3`() {
        assertEquals("0.3", Oacp.VERSION)
    }

    @Test
    fun `action result uses org_oacp namespace`() {
        assertEquals("org.oacp.ACTION_RESULT", Oacp.ACTION_RESULT)
    }

    @Test
    fun `standard extras use org_oacp_extra prefix`() {
        val extras = listOf(
            Oacp.EXTRA_REQUEST_ID,
            Oacp.EXTRA_STATUS,
            Oacp.EXTRA_CAPABILITY_ID,
            Oacp.EXTRA_SOURCE_PACKAGE,
            Oacp.EXTRA_MESSAGE,
            Oacp.EXTRA_RESULT,
            Oacp.EXTRA_ERROR_CODE,
            Oacp.EXTRA_ERROR_MESSAGE,
            Oacp.EXTRA_ERROR_RETRYABLE
        )
        extras.forEach { extra ->
            assertTrue(
                "Extra '$extra' should start with 'org.oacp.extra.'",
                extra.startsWith("org.oacp.extra.")
            )
        }
    }

    @Test
    fun `status values are lowercase strings`() {
        val statuses = listOf(
            Oacp.STATUS_ACCEPTED,
            Oacp.STATUS_STARTED,
            Oacp.STATUS_COMPLETED,
            Oacp.STATUS_FAILED,
            Oacp.STATUS_CANCELLED
        )
        statuses.forEach { status ->
            assertEquals(status, status.lowercase())
        }
    }

    @Test
    fun `provider authority suffix is dot_oacp`() {
        assertEquals(".oacp", Oacp.PROVIDER_AUTHORITY_SUFFIX)
    }

    @Test
    fun `provider paths match spec`() {
        assertEquals("manifest", Oacp.PROVIDER_PATH_MANIFEST)
        assertEquals("context", Oacp.PROVIDER_PATH_CONTEXT)
        assertEquals("entities", Oacp.PROVIDER_PATH_ENTITIES)
    }

    @Test
    fun `application id placeholder is correct`() {
        assertEquals("__APPLICATION_ID__", Oacp.APPLICATION_ID_PLACEHOLDER)
    }

    @Test
    fun `all 11 error codes exist`() {
        val codes = listOf(
            Oacp.ERROR_INVALID_PARAMETERS,
            Oacp.ERROR_MISSING_PARAMETERS,
            Oacp.ERROR_NOT_FOUND,
            Oacp.ERROR_NOT_AUTHENTICATED,
            Oacp.ERROR_NOT_AUTHORIZED,
            Oacp.ERROR_REQUIRES_FOREGROUND,
            Oacp.ERROR_REQUIRES_UNLOCK,
            Oacp.ERROR_UNSUPPORTED_STATE,
            Oacp.ERROR_NETWORK_UNAVAILABLE,
            Oacp.ERROR_CANCELLED,
            Oacp.ERROR_INTERNAL
        )
        assertEquals(11, codes.size)
        codes.forEach { code ->
            assertTrue("Error code should not be empty", code.isNotEmpty())
        }
    }
}
