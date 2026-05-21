package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkCardBorder

@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    borderColor: Color = DarkCardBorder,
    glowColor: Color? = null,
    borderWidth: Dp = 1.dp,
    contentPadding: Dp = 16.dp,
    containerColor: Color = DarkCardBg,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val roundedShape = RoundedCornerShape(28.dp)
    
    // Choose border brush
    val borderBrush = if (glowColor != null) {
        Brush.linearGradient(listOf(glowColor, borderColor))
    } else {
        Brush.linearGradient(listOf(borderColor, borderColor))
    }

    Card(
        modifier = modifier
            .clip(roundedShape)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = roundedShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(borderWidth, borderBrush),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (glowColor != null) 3.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .background(containerColor)
                .padding(contentPadding)
        ) {
            content()
        }
    }
}
