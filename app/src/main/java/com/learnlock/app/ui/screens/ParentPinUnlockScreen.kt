package com.learnlock.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnlock.app.ui.components.LlBackground
import com.learnlock.app.ui.components.LlGhostButton
import com.learnlock.app.ui.components.LlPinField
import com.learnlock.app.ui.components.LlPrimaryButton
import com.learnlock.app.ui.theme.LlColors

@Composable
fun ParentPinUnlockScreen(
    backgroundColor: Int = 0,
    onVerify: (String) -> Unit,
    onBack: () -> Unit,
    error: String? = null
) {
    var pin by remember { mutableStateOf("") }

    LlBackground(backgroundColor = backgroundColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Введите PIN родителя", style = MaterialTheme.typography.displayMedium, color = LlColors.MintDeep)
            Text("PIN нужен для открытия настроек.", style = MaterialTheme.typography.bodyMedium, color = LlColors.TextSecondary)
            LlPinField(value = pin, onValueChange = { pin = it }, label = "PIN родителя")
            if (error != null) Text(error, color = LlColors.Coral)
            LlPrimaryButton("Открыть настройки", onClick = { onVerify(pin) }, enabled = pin.length >= 4, coral = true)
            LlGhostButton("Назад", onClick = onBack, contentColor = LlColors.TextSecondary)
        }
    }
}
