package com.learnlock.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learnlock.app.ui.theme.LlColors

private val CardShape = RoundedCornerShape(24.dp)
private val ButtonShape = RoundedCornerShape(18.dp)

@Composable
fun LlBackground(
    modifier: Modifier = Modifier,
    energetic: Boolean = false,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "bg")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )
    val brush = if (energetic) {
        Brush.verticalGradient(
            colors = listOf(
                LlColors.Ink,
                LlColors.InkSoft,
                Color(0xFF12353A),
                LlColors.MintDeep.copy(alpha = 0.55f + 0.2f * shift)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LlColors.Mist,
                Color(0xFFDFF3F1).copy(alpha = 0.9f + 0.05f * shift),
                LlColors.MistDeep.copy(alpha = 0.65f)
            )
        )
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}

@Composable
fun LlPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(LlColors.SurfaceElevated.copy(alpha = 0.92f))
            .border(1.dp, LlColors.MistDeep.copy(alpha = 0.7f), CardShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
fun LlPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    coral: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (coral) LlColors.Coral else LlColors.MintDeep,
            contentColor = LlColors.TextOnAccent,
            disabledContainerColor = LlColors.MistDeep,
            disabledContentColor = LlColors.TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LlGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LlColors.TextPrimary
        )
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LlChoiceTile(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = if (selected) 1.03f else 1f
    Box(
        modifier = modifier
            .height(64.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) LlColors.MintDeep else LlColors.SurfaceElevated.copy(alpha = 0.9f)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) LlColors.MintBright else LlColors.MistDeep,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleLarge,
            color = if (selected) LlColors.TextOnAccent else LlColors.TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LlBrandMark(
    light: Boolean = true,
    subtitle: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "LearnLock",
            style = MaterialTheme.typography.headlineLarge,
            color = if (light) LlColors.TextOnAccent else LlColors.MintDeep
        )
        subtitle?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (light) LlColors.MistDeep else LlColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
