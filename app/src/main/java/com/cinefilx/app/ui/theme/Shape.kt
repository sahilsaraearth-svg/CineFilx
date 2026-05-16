package com.cinefilx.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 shape tokens
val CineFilxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chips, snackbars
    small = RoundedCornerShape(8.dp),         // text fields, menus
    medium = RoundedCornerShape(12.dp),       // cards
    large = RoundedCornerShape(16.dp),        // FABs, nav drawer
    extraLarge = RoundedCornerShape(28.dp)    // dialogs, bottom sheets
)
