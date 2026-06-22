package com.momcare.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MOMCARE CLAY DESIGN SYSTEM - Jetpack Compose Implementation
 */

object ClayColors {
    val Primary = Color(0xFFC98C7B)
    val PrimaryHover = Color(0xFFB67868)
    val Background = Color(0xFFF6F1EC)
    val Surface = Color(0xFFF2EAE4)
    val Card = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF5A463F)
    val TextSecondary = Color(0xFF9C857C)
}

@Composable
fun ClayCard(
    modifier: Modifier = Modifier,
    floating: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (floating) 24.dp else 12.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = ClayColors.TextPrimary.copy(alpha = 0.1f),
                spotColor = ClayColors.TextPrimary.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(32.dp),
        color = ClayColors.Card,
        content = content
    )
}

@Composable
fun ClayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ClayColors.Primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp
        ),
        content = content
    )
}
