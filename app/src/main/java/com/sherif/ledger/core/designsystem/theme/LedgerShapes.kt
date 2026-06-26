package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object LedgerCornerRadius {
    val Small = 12.dp
    val Medium = 20.dp
    val Large = 28.dp
}

val LedgerShapes = Shapes(
    small = RoundedCornerShape(LedgerCornerRadius.Small),
    medium = RoundedCornerShape(LedgerCornerRadius.Medium),
    large = RoundedCornerShape(LedgerCornerRadius.Large),
)
