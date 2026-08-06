package com.sherif.ledger.feature.capture.source

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSourceAdapterTest {

    // Regression (2026-08-06): a real ADCB transfer notification on the owner's
    // device populated android.text/android.bigText with a redaction placeholder
    // for third-party listeners while the real transaction text sat in a
    // non-standard "content" extra — silently dropping every ADCB-app-notification
    // transfer (as opposed to SMS) from capture. Caught live by cross-checking a
    // ~AED 100 test transfer's raw notification extras against Ledger's pipeline log.
    @Test
    fun `falls back to content extra when standard fields are redacted`() {
        val extras = mapOf(
            "android.title" to "ADCB",
            "android.text" to "Sensitive notification content hidden",
            "android.bigText" to "Sensitive notification content hidden",
            "content" to "AED120.00 transferred via ADCB Personal Internet Banking / " +
                "Mobile App from acc. no. XXX920001 on Aug  6 2026 12:04PM. Avl. bal. AED 6280.04.",
        )

        assertEquals(
            "AED120.00 transferred via ADCB Personal Internet Banking / " +
                "Mobile App from acc. no. XXX920001 on Aug  6 2026 12:04PM. Avl. bal. AED 6280.04.",
            NotificationSourceAdapter.resolveText(extras),
        )
    }

    @Test
    fun `prefers bigText over text for an ordinary BigTextStyle notification`() {
        val extras = mapOf(
            "android.text" to "You spent AED 50",
            "android.bigText" to "You spent AED 50 at Costa Coffee on your card ending 1234.",
        )

        assertEquals(
            "You spent AED 50 at Costa Coffee on your card ending 1234.",
            NotificationSourceAdapter.resolveText(extras),
        )
    }

    @Test
    fun `falls back to standard text when nothing looks financial`() {
        val extras = mapOf(
            "android.text" to "Welcome back to your mobile app",
            "content" to "internal-deeplink-payload-not-user-text",
        )

        assertEquals("Welcome back to your mobile app", NotificationSourceAdapter.resolveText(extras))
    }

    @Test
    fun `returns empty string when no extras are present`() {
        assertEquals("", NotificationSourceAdapter.resolveText(emptyMap()))
    }
}
