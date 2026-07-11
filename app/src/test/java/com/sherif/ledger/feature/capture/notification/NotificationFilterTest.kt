package com.sherif.ledger.feature.capture.notification

import com.sherif.ledger.core.domain.model.IngestionSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NotificationFilterTest {

    private val filter = NotificationFilter()

    @Test
    fun `shouldProcess returns false for unknown non-financial package`() {
        val envelope = createEnvelope(packageName = "com.unknown.app", text = "Content")
        assertFalse(filter.shouldProcess(envelope))
    }

    @Test
    fun `shouldProcess returns true for financial package with transaction keywords`() {
        val envelope = createEnvelope(
            packageName = "com.adcb.nexgen",
            text = "You paid AED 50 at Costa Coffee",
        )
        assertTrue(filter.shouldProcess(envelope))
    }

    @Test
    fun `shouldProcess returns false for financial package without transaction content`() {
        val envelope = createEnvelope(
            packageName = "com.adcb.nexgen",
            text = "Welcome back to your mobile app",
        )
        assertFalse(filter.shouldProcess(envelope))
    }

    // ---- Regression: real bank app package names (Jul 2026) ----
    // These messages produced zero transactions on-device because the filter
    // gated on a guessed package name (com.adcb.mobileapp) that does not exist.
    // The real ADCB app is com.adcb.nexgen. The filter now admits financial
    // content regardless of package, so a renamed/unknown bank app is not dropped.

    @Test
    fun `real ADCB income notification is admitted`() {
        val e = createEnvelope(
            packageName = "com.adcb.nexgen",
            title = "",
            text = "A Cr. transaction of AED 1200.00 on your account no. XXX920001 was successful.Available balance is AED9155.36.",
        )
        assertTrue(filter.shouldProcess(e))
    }

    @Test
    fun `real ADCB transfer notifications are admitted`() {
        val t1 = createEnvelope(
            packageName = "com.adcb.nexgen",
            title = "",
            text = "AED1200.00 transferred via ADCB Personal Internet Banking / Mobile App from acc. no. XXX920001 on Jul  8 2026  9:07PM. Avl. bal. AED 7955.36.",
        )
        val t2 = createEnvelope(
            packageName = "com.adcb.nexgen",
            title = "",
            text = "AED660.00 transferred via ADCB Personal Internet Banking / Mobile App from acc. no. XXX920001 on Jul  8 2026  9:13PM. Avl. bal. AED 7294.86.",
        )
        assertTrue(filter.shouldProcess(t1))
        assertTrue(filter.shouldProcess(t2))
    }

    @Test
    fun `real Mashreq and FAB notifications are admitted regardless of package`() {
        val mashreq = createEnvelope(
            packageName = "com.vipera.ts.starter.MashreqAE",
            title = "",
            text = "Mashreq Credit Card ending 1959 was used for a transaction of AED 37.00 at PATHAYAPURA RESTAURANT on Tuesday, 7 July 2026, 10:29 pm. Available limit: AED 7,544.65",
        )
        val fab = createEnvelope(
            packageName = "com.fab.personalbanking",
            title = "Credit Card Purchase",
            text = "Card No XXXX6989 AED 54.69 Amazon Grocery Dubai ARE 15/06/26 20:01 Avl Bal AED 2077.96",
        )
        assertTrue(filter.shouldProcess(mashreq))
        assertTrue(filter.shouldProcess(fab))
    }

    @Test
    fun `financial content from a completely unknown package is admitted`() {
        // The core generalization: an unknown sender with clearly financial
        // content must not be silently dropped.
        val e = createEnvelope(
            packageName = "com.some.newbank.app",
            title = "",
            text = "AED 320.00 debited from your account XXX1234 at CARREFOUR.",
        )
        assertTrue(filter.shouldProcess(e))
    }

    @Test
    fun `SMS source is always admitted`() {
        val e = createEnvelope(
            packageName = "com.carrier.messages",
            text = "AED 100 debited",
            source = IngestionSource.SMS,
        )
        assertTrue(filter.shouldProcess(e))
    }

    @Test
    fun `non-financial notification from unknown package is dropped`() {
        val e = createEnvelope(packageName = "com.whatsapp", text = "Are we still on for dinner?")
        assertFalse(filter.shouldProcess(e))
    }

    @Test
    fun `blank notification is dropped`() {
        val e = createEnvelope(packageName = "com.adcb.nexgen", title = "", text = "")
        assertFalse(filter.shouldProcess(e))
    }

    @Test
    fun `balance-only notification is not a positive signal`() {
        // Refinement: "balance" removed from admission vocabulary. A balance-only
        // notification from an unknown package must be rejected.
        val e = createEnvelope(
            packageName = "com.some.unknownbank",
            title = "",
            text = "Your available balance is AED 9155.36 as of today.",
        )
        assertFalse(filter.shouldProcess(e))
    }

    @Test
    fun `evaluate exposes accept and reject reasons`() {
        val accepted = filter.evaluate(
            createEnvelope(
                packageName = "com.some.newbank",
                title = "",
                text = "AED 320.00 debited from your account XXX1234 at CARREFOUR.",
            ),
        )
        assertTrue(accepted is FilterResult.Accepted)

        val rejected = filter.evaluate(
            createEnvelope(packageName = "com.whatsapp", text = "dinner tonight?"),
        )
        assertTrue(rejected is FilterResult.Rejected)
    }

    private fun createEnvelope(
        packageName: String = "com.adcb.nexgen",
        title: String = "Alert",
        text: String = "Content",
        source: IngestionSource = IngestionSource.NOTIFICATION,
    ) = NotificationEnvelope(
        packageName = packageName,
        title = title,
        text = text,
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "key",
        source = source,
    )
}




