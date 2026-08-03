package com.sherif.ledger.core.domain.service.account

import javax.inject.Inject

/**
 * What kind of thing sent a message, for the single purpose of deciding whether it
 * could ever be an account of the user's.
 *
 * Deliberately NOT a judgement about whether the message is financial — that is
 * [com.sherif.ledger.feature.capture.notification.NotificationFilter]'s and the
 * extractor's job, and this must never become a second, competing content gate.
 */
enum class SenderKind {
    /**
     * A messaging app that merely displayed someone else's message: Google
     * Messages, Truecaller, Samsung Messages. The bank is named inside the text;
     * the app around it is transport and carries no financial identity at all.
     */
    TRANSPORT,

    /**
     * A sender known not to be a financial institution — telecom operators,
     * loyalty programmes, marketing shortcodes. Their messages can still mention
     * money ("your bill is AED 250"), but there is no account here to hold.
     */
    NON_FINANCIAL,

    /** Nothing is known about it. It may well be a real bank Ledger hasn't met yet. */
    UNKNOWN,
}

/**
 * Whether a notification package name or SMS sender ID could name an account.
 *
 * Exists because of what real captured data did without it: a fresh import of the
 * owner's inbox created twelve accounts, ten of them named after things that are
 * not banks — `Unrecognized Institution (Smiles)`, `(eandINF)`, `(eandUAE)`,
 * `(com.truecaller)`, `(com.google.android.apps.messaging)`. Two failure modes,
 * distinguished here because they deserve different reasoning rather than one
 * catch-all denylist:
 *
 *  - A **transport** app is structurally incapable of being an institution. This
 *    is a rule, not a list of nuisances: whoever the message is from, it is not
 *    from the messaging app that rendered it. Worse than merely untidy, an account
 *    named after a messenger competes with the real bank's account for the same
 *    transactions — the owner's database had one card's spending split between
 *    "Mashreq Credit Card ···1959" and an account named after Google Messages
 *    holding the same tail.
 *  - A **non-financial** sender is a maintained list, and is only ever consulted
 *    to REFUSE an account. It never admits anything, so a wrong entry can lose an
 *    account name but can never invent one.
 *
 * Anything else stays [SenderKind.UNKNOWN] and keeps the existing RC7 Phase B
 * behaviour of parking a reviewable, promotable Candidate Account per institution
 * — an unrecognised bank is a gap to surface, not junk to suppress.
 */
class SenderClassifier @Inject constructor() {

    /**
     * Full package names, matched exactly. A messaging app is identified by being
     * that app, never by a substring — "com.truecaller" must not be reachable by
     * accident from an unrelated identifier that happens to contain it.
     */
    private val transportPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.truecaller",
        "com.whatsapp",
        "com.microsoft.android.smsorganizer",
        "org.thoughtcrime.securesms",
        "org.telegram.messenger",
        "com.moez.qksms",
        "com.textra",
    )

    /**
     * Sender IDs, compared against [coreIdentifier] so the same operator is
     * recognised however a carrier decorated the header. Every entry here was
     * observed creating an account on the owner's device, or is an immediate
     * sibling of one.
     *
     * Note `EANDMONEY` is deliberately absent: e& money is a payment wallet with
     * real balances, unlike the telecom and loyalty senders that share its prefix.
     * That is exactly why this matches whole identifiers rather than a prefix.
     */
    private val nonFinancialSenders = setOf(
        "SMILES",
        "EAND",
        "EANDINF",
        "EANDUAE",
        "ETISALAT",
        "DU",
        "DUUAE",
    )

    fun classify(identifier: String?): SenderKind {
        if (identifier.isNullOrBlank()) return SenderKind.UNKNOWN
        if (identifier.lowercase() in transportPackages) return SenderKind.TRANSPORT
        if (coreIdentifier(identifier) in nonFinancialSenders) return SenderKind.NON_FINANCIAL
        return SenderKind.UNKNOWN
    }

    /** True when an account may be named after this sender. */
    fun canOwnAnAccount(identifier: String?): Boolean = classify(identifier) == SenderKind.UNKNOWN

    /**
     * Strips the routing decoration Indian DLT and UAE aggregator headers wrap
     * around an operator's own code — a leading two-letter carrier prefix
     * (`AD-eand`) and a trailing one-letter route marker (`JK-SBIUPI-S`) — leaving
     * the identifier the sender actually chose. Comparison is then whole-string, so
     * lengthening an entry can never widen what it matches.
     */
    private fun coreIdentifier(identifier: String): String =
        identifier.uppercase()
            .removePrefix("+")
            .replace(Regex("^[A-Z]{2}-"), "")
            .replace(Regex("-[A-Z]$"), "")
}
