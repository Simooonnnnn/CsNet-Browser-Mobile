package com.csnet.browser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun CsNetLogo(
    modifier: Modifier = Modifier,
    size: Float = 75f
) {
    val color = MaterialTheme.colorScheme.onBackground // This will adapt to light/dark mode

    Canvas(
        modifier = modifier.size(size.dp)
    ) {
        val barHeight = size * 0.29f // 22/76 of total height
        val barWidth = size * 0.787f // 59/75 of total width
        val cornerRadius = size * 0.147f // 11/75 of total width
        val middleBarOffset = size * 0.213f // 16/75 of total width
        val verticalSpacing = size * 0.066f // 5/76 of total height

        // Top bar
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Middle bar
        drawRoundRect(
            color = color,
            topLeft = Offset(middleBarOffset, size * 0.355f), // 27/76 of height
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )

        // Bottom bar
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, size * 0.71f), // 54/76 of height
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
        )
    }
}