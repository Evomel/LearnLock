package com.learnlock.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.learnlock.app.ui.theme.LlColors

@Composable
internal fun LlPinField(value: String, onValueChange: (String) -> Unit, label: String) {
    // This field uses a light container even when the system theme is dark.
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (next.length <= 12 && next.all(Char::isDigit)) onValueChange(next)
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = LlColors.Surface,
            unfocusedContainerColor = LlColors.Surface,
            focusedTextColor = LlColors.TextPrimary,
            unfocusedTextColor = LlColors.TextPrimary,
            focusedLabelColor = LlColors.MintDeep,
            unfocusedLabelColor = LlColors.TextSecondary,
            focusedBorderColor = LlColors.MintDeep,
            unfocusedBorderColor = LlColors.TextSecondary,
            cursorColor = LlColors.MintDeep
        )
    )
}
