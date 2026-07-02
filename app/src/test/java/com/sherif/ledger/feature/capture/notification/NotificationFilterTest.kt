package com.sherif.ledger.feature.capture.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NotificationFilterTest {

    private val filter = NotificationFilter()

    @Test
    fun `shouldProcess returns false for unknown package`() {
        val envelope = createEnvelope(packageName = "com.unknown.app")
        assertFalse(filter.shouldProcess(envelope))
    }

    @Test
    fun `shouldProcess returns true for financial package with transaction keywords`() {
        val envelope = createEnvelope(
            packageName = "com.adcb.mobileapp",
            text = "You paid AED 50 at Costa Coffee"
        )
        assertTrue(filter.shouldProcess(envelope))
    }

    @Test
    fun `shouldProcess returns false for financial package without transaction keywords`() {
        val envelope = createEnvelope(
            packageName = "com.adcb.mobileapp",
            text = "Welcome back to your mobile app"
        )
        assertFalse(filter.shouldProcess(envelope))
    }

    private fun createEnvelope(
        packageName: String = "com.adcb.mobileapp",
        title: String = "Alert",
        text: String = "Content"
    ) = NotificationEnvelope(
        packageName = packageName,
        title = title,
        text = text,
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "key"
    )
}
