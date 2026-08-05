package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material3's Shapes() requires CornerBasedShape specifically (SquircleShape
// only implements the plain Shape interface), and this object only ever backs
// default Material component fallbacks the app's own components don't use —
// every real surface in the app draws its corner from LedgerRadius, which IS
// the squircle. Not worth widening SquircleShape's contract for this.
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
