package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import androidx.compose.material3.Text
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

/** A section of legal / informational copy: an optional heading + body paragraph. */
private data class LegalSection(val heading: String?, val body: String)

@Composable
private fun LegalScaffold(
    title: String,
    onBackClick: () -> Unit,
    intro: String,
    sections: List<LegalSection>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceBase),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen, bottom = ledgerScreenBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
    ) {
        item("nav") {
            LedgerScreenHeader(title = title, onBackClick = onBackClick)
        }

        item("intro") {
            Text(intro, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
        }

        items(sections.size) { i ->
            val section = sections[i]
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny)) {
                if (section.heading != null) {
                    Text(
                        section.heading,
                        style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                        color = LedgerTheme.colors.textPrimary,
                    )
                }
                Text(section.body, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
            }
        }

        item("tail") { Spacer(Modifier.height(LedgerSpacing.XLarge)) }
    }
}

/**
 * Privacy Policy — reflects Ledger's actual, offline-first architecture: financial
 * data is read and stored on-device; nothing leaves the device except the optional,
 * opt-in AI calls the user explicitly configures with their own API key.
 *
 * This is the product's honest description of its data handling. Before a public
 * store listing the owner should finalize the contact address and, ideally, have it
 * reviewed — the effective date below marks the current version.
 */
@Composable
fun PrivacyPolicyScreen(onBackClick: () -> Unit = {}) {
    LegalScaffold(
        title = "Privacy Policy",
        onBackClick = onBackClick,
        intro = "Effective 23 July 2026. Ledger is an offline-first personal finance app. " +
            "Your financial data stays on your device — there is no Ledger account and no Ledger server.",
        sections = listOf(
            LegalSection(
                "What Ledger accesses",
                "• Bank/payment SMS and notifications on your device, read locally to detect transactions.\n" +
                    "• A local profile (a name and email you optionally enter), stored only on your device.\n" +
                    "Ledger has no analytics, no advertising, and no third-party tracking.",
            ),
            LegalSection(
                "How SMS and notifications are used",
                "Transaction messages are parsed on your device to build your ledger. Their content is " +
                    "not transmitted anywhere by Ledger. Messages that aren't financial are ignored.",
            ),
            LegalSection(
                "Optional AI features",
                "AI categorization is OFF by default. If you turn it on and provide your own API key, the " +
                    "text of transactions you choose to process is sent to the AI provider you selected " +
                    "(for example Google Gemini, Groq, or OpenAI) to suggest a category. That processing is " +
                    "governed by your chosen provider's privacy policy. No AI calls are made while the feature " +
                    "is off, and you can disable it at any time.",
            ),
            LegalSection(
                "Storage and security",
                "Your data is stored in an app-private database on your device. API keys are held in Android's " +
                    "encrypted key storage. Nothing is backed up to a Ledger server because none exists.",
            ),
            LegalSection(
                "Data sharing",
                "Ledger does not sell or share your data. The only outbound data is the optional AI request " +
                    "described above, which you explicitly enable and control.",
            ),
            LegalSection(
                "Retention and deletion",
                "Your data remains on your device until you delete it in the app or uninstall Ledger. " +
                    "Uninstalling removes all local Ledger data.",
            ),
            LegalSection(
                "Permissions",
                "• SMS (read/receive): to detect transactions from bank messages.\n" +
                    "• Notification access: to read transaction notifications from banking apps.\n" +
                    "• Post notifications: to show you capture confirmations you can act on.",
            ),
            LegalSection(
                "Children",
                "Ledger is not directed to children and does not knowingly collect data from them.",
            ),
            LegalSection(
                "Changes and contact",
                "We may update this policy; the effective date above marks the current version. " +
                    "Questions: [set a contact address before publishing].",
            ),
        ),
    )
}

/**
 * Open-source licenses / attribution. Every third-party library Ledger bundles is
 * distributed under the Apache License 2.0, whose notice requirement is satisfied
 * here. Keep this list in step with app/build.gradle.kts.
 */
@Composable
fun LicensesScreen(onBackClick: () -> Unit = {}) {
    val apache = "Apache License 2.0"
    LegalScaffold(
        title = "Open Source Licenses",
        onBackClick = onBackClick,
        intro = "Ledger is built with open-source software. We're grateful to the maintainers of the " +
            "following libraries. Each is used under its license, reproduced in summary below.",
        sections = listOf(
            LegalSection("Jetpack Compose & AndroidX", "© The Android Open Source Project — $apache"),
            LegalSection("Kotlin, Coroutines & Serialization", "© JetBrains s.r.o. and contributors — $apache"),
            LegalSection("Dagger Hilt", "© Google LLC — $apache"),
            LegalSection("Room & DataStore", "© The Android Open Source Project — $apache"),
            LegalSection("AndroidX Security (Crypto)", "© The Android Open Source Project — $apache"),
            LegalSection("OkHttp", "© Square, Inc. — $apache"),
            LegalSection("Haze", "© Chris Banes — $apache"),
            LegalSection(
                "Apache License 2.0",
                "Licensed under the Apache License, Version 2.0 (the \"License\"); you may not use these " +
                    "files except in compliance with the License. Unless required by applicable law or agreed " +
                    "to in writing, software distributed under the License is distributed on an \"AS IS\" BASIS, " +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND. A full copy is available at " +
                    "https://www.apache.org/licenses/LICENSE-2.0.",
            ),
        ),
    )
}
