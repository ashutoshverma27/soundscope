package com.soundscope.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundscope.app.ui.theme.DeepSpaceCard
import com.soundscope.app.ui.theme.GlassBorder
import com.soundscope.app.ui.theme.GlassBorderGlow

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    containerColor: Color = DeepSpaceCard,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    glowEffect: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val borderBrush = if (glowEffect) {
        Brush.linearGradient(
            colors = listOf(
                GlassBorderGlow,
                GlassBorder,
                Color.Transparent,
                GlassBorderGlow
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = 0.2f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        containerColor.copy(alpha = 0.95f),
                        containerColor.copy(alpha = 0.80f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = borderBrush,
                shape = shape
            )
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(16.dp),
        content = content
    )
}
