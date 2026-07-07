package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Phase 4D: bank-agnostic financial-language understanding. Real transactions
 * from many banks must persist with the correct type; promotions/marketing/OTP/
 * statements must not; confirmations must not double-count.
 */
class FinancialLanguageTest {

    private val extractor = HeuristicExtractor(TextNormalizer(), MerchantNormalizer(), FinancialPhraseLibrary())

    private fun run(text: String): ExtractionResult = runBlocking {
        extractor.extract(
            NotificationEnvelope(
                packageName = "com.google.android.apps.messaging",
                title = "", text = text, subText = null,
                timestamp = Instant.now(), notificationKey = "t",
            ),
        )
    }

    private fun persists(text: String): ExtractionResult.Extracted {
        val r = run(text)
        assertTrue("Expected Extracted, got $r", r is ExtractionResult.Extracted)
        return r as ExtractionResult.Extracted
    }

    private fun ignored(text: String) {
        val r = run(text)
        assertTrue("Expected Ignore, got $r", r is ExtractionResult.Ignore)
    }

    private fun confirmed(text: String) {
        val r = run(text)
        assertTrue("Expected Confirmation, got $r", r is ExtractionResult.Confirmation)
    }

    // ---- Multi-bank real transactions (must persist) ----

    @Test fun `adcb salary`() { assertEquals(TransactionType.INCOME, persists("Your salary AED6000.00 has been credited to account no. XXX920001.").candidate.transactionType) }
    @Test fun `fab purchase`() { assertEquals(5469L, persists("Credit Card Purchase Card No XXXX6989 AED 54.69 Amazon Grocery Dubai.").candidate.amountMinor) }
    @Test fun `mashreq pos`() { assertEquals(TransactionType.EXPENSE, persists("Mashreq Credit Card ending 1959 used for AED 3.00 at FRESH WAY BAQALA.").candidate.transactionType) }
    @Test fun `enbd payment`() { assertEquals(3775L, persists("Payment of AED 37.75 with Credit Card ending 8165 at Noon Minutes.").candidate.amountMinor) }
    @Test fun `rakbank debit`() { assertEquals(12050L, persists("AED 120.50 debited from your RAKBANK account XXXX4455 at CARREFOUR.").candidate.amountMinor) }
    @Test fun `hsbc transfer`() { assertEquals(TransactionType.TRANSFER, persists("AED 500.00 transferred to JOHN DOE from account ending 7788.").candidate.transactionType) }
    @Test fun `dib fund transfer`() { assertEquals(TransactionType.TRANSFER, persists("AED 75.00 sent to AHMED via fund transfer from A/C XXXX2233.").candidate.transactionType) }
    @Test fun `unknown bank purchase`() { assertEquals(25000L, persists("AED 250.00 spent at STARBUCKS using card ending 9999.").candidate.amountMinor) }

    // ---- Concept types (must persist with correct type) ----

    @Test fun `loan disbursal is income`() { assertEquals(TransactionType.INCOME, persists("Your loan of AED 50,000 has been disbursed and credited to account XXXX1234.").candidate.transactionType) }
    @Test fun `emi deduction is expense`() { assertEquals(TransactionType.EXPENSE, persists("EMI of AED 1,200 debited from your account XXXX1234 towards loan.").candidate.transactionType) }
    @Test fun `cashback credit is income`() { assertEquals(TransactionType.INCOME, persists("Cashback of AED 15.00 credited to your account XXXX1234.").candidate.transactionType) }
    @Test fun `interest earned is income`() { assertEquals(TransactionType.INCOME, persists("Interest of AED 45.20 credited to your savings account XXXX9012.").candidate.transactionType) }
    @Test fun `fee is expense`() { assertEquals(TransactionType.EXPENSE, persists("Annual fee of AED 100.00 debited from card ending 5566.").candidate.transactionType) }
    @Test fun `refund is refund`() { assertEquals(TransactionType.REFUND, persists("Refund of AED 30.00 for your purchase has been credited to card ending 1234.").candidate.transactionType) }
    @Test fun `atm is expense`() { assertEquals(TransactionType.EXPENSE, persists("AED3000.00 withdrawn from acc. XXX920001 at ATM.").candidate.transactionType) }
    @Test fun `cheque deposit persists`() { assertEquals(300000L, persists("Cheque of AED 3000.00 deposited to account XXXX1122.").candidate.amountMinor) }

    // ---- Merchant extraction variants ----

    @Test fun `merchant from received from`() { assertEquals("John Doe", persists("AED 500.00 received from JOHN DOE to account ending 1234.").candidate.merchantName) }
    @Test fun `merchant from pos`() { assertEquals(TransactionType.EXPENSE, persists("AED 40.00 spent POS CARREFOUR card ending 1234.").candidate.transactionType) }

    // ---- Must ignore: offers, marketing, OTP, statement ----

    @Test fun `loan offer ignored`() { ignored("You are eligible for a personal loan of AED 50,000. Apply now for instant approval.") }
    @Test fun `emi offer ignored`() { ignored("Convert your purchase to easy EMI. Limited time offer.") }
    @Test fun `cashback offer ignored`() { ignored("Get 10% cashback on your next purchase. Avail now.") }
    @Test fun `credit limit offer ignored`() { ignored("Increase your credit limit instantly. You are pre-approved.") }
    @Test fun `insurance offer ignored`() { ignored("Get insured today! Exclusive offer on health insurance. Apply now.") }
    @Test fun `marketing with amount ignored`() { ignored("Spend AED 500 and win rewards! Limited time offer. Don't miss.") }
    @Test fun `pure marketing ignored`() { ignored("Exclusive offer! Upgrade your card today and enjoy premium benefits.") }
    @Test fun `otp ignored`() { ignored("Your OTP is 445566. Do not share.") }
    @Test fun `statement ignored`() { ignored("Your monthly statement is ready. Total due AED 500.") }

    // ---- Confirmations (must not double-count) ----

    @Test fun `payment received is confirmation`() { confirmed("Payment of AED 200 received. Outstanding balance AED 3450. Thank you.") }
    @Test fun `card payment event is transfer not confirmation`() { assertEquals(TransactionType.TRANSFER, persists("AED 200 debited from your account towards FAB Credit Card.").candidate.transactionType) }
}
