package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerElevation
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing

/**
 * Ledger Design System Card.
 *
 * @deprecated Superseded by [LedgerSurface], which expresses hierarchy through
 * semantic surface levels instead of elevation. Retained for exactly one commit
 * so the repository compiles during migration, then removed once every call site
 * has moved to [LedgerSurface].
 */
@Deprecated(
    message = "Use LedgerSurface. Surfaces express hierarchy through semantic " +
        "surface levels, not elevation. Removed in the follow-up migration commit.",
)
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Medium),

    containerColor: Color = MaterialTheme.colorScheme.surface,

    border: BorderStroke? = BorderStroke(
        width = LedgerElevation.None,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
    ),

    content: @Composable ColumnScope.() -> Unit,
) {

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = LedgerElevation.Raised,
        ),
    ) {

        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )

    }

}
