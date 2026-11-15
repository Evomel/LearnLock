package com.learnlock.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learnlock.app.data.ChildProfileEntity
import com.learnlock.app.data.OutsideWindowMode
import com.learnlock.app.ui.components.LlBackground
import com.learnlock.app.ui.components.LlGhostButton
import com.learnlock.app.ui.components.LlPanel
import com.learnlock.app.ui.components.LlPrimaryButton
import com.learnlock.app.ui.theme.LlColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profile: ChildProfileEntity,
    onSave: (ChildProfileEntity) -> Unit,
    onBack: () -> Unit
) {
    var draft by remember(profile) { mutableStateOf(profile) }
    val sessionOptions = listOf(5, 15, 30, 60)
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = LlColors.MintDeep,
        selectedLabelColor = LlColors.TextOnAccent
    )

    LlBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Настройки", style = MaterialTheme.typography.displayMedium, color = LlColors.MintDeep)

            LlPanel {
                OutlinedTextField(
                    value = draft.displayName,
                    onValueChange = { draft = draft.copy(displayName = it.take(24)) },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LlColors.MintDeep)
                )

                Text("Возраст: ${draft.ageYears}", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = draft.ageYears.toFloat(),
                    onValueChange = {
                        draft = draft.copy(ageYears = it.roundToInt().coerceIn(5, 16))
                    },
                    valueRange = 5f..16f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = LlColors.MintDeep, activeTrackColor = LlColors.Mint)
                )

                Text("Сложность: ${difficultyLabel(draft.difficultyOffset)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-1, 0, 1).forEach { offset ->
                        FilterChip(
                            selected = draft.difficultyOffset == offset,
                            onClick = { draft = draft.copy(difficultyOffset = offset) },
                            label = { Text(difficultyLabel(offset)) },
                            colors = chipColors
                        )
                    }
                }

                Text("Сессия")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sessionOptions.forEach { minutes ->
                        FilterChip(
                            selected = draft.sessionMinutes == minutes,
                            onClick = { draft = draft.copy(sessionMinutes = minutes) },
                            label = { Text("$minutes мин") },
                            colors = chipColors
                        )
                    }
                }

                Text("Окно: ${draft.activeWindowStartHour}:00–${draft.activeWindowEndHour}:00")
                Slider(
                    value = draft.activeWindowStartHour.toFloat(),
                    onValueChange = {
                        draft = draft.copy(activeWindowStartHour = it.roundToInt().coerceIn(0, 23))
                    },
                    valueRange = 0f..23f,
                    steps = 22,
                    colors = SliderDefaults.colors(thumbColor = LlColors.Coral, activeTrackColor = LlColors.CoralSoft)
                )
                Slider(
                    value = draft.activeWindowEndHour.toFloat(),
                    onValueChange = {
                        draft = draft.copy(activeWindowEndHour = it.roundToInt().coerceIn(0, 23))
                    },
                    valueRange = 0f..23f,
                    steps = 22,
                    colors = SliderDefaults.colors(thumbColor = LlColors.Coral, activeTrackColor = LlColors.CoralSoft)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.outsideWindowMode == OutsideWindowMode.NO_TASKS.name,
                        onClick = {
                            draft = draft.copy(outsideWindowMode = OutsideWindowMode.NO_TASKS.name)
                        },
                        label = { Text("Вне окна: без задач") },
                        colors = chipColors
                    )
                    FilterChip(
                        selected = draft.outsideWindowMode == OutsideWindowMode.HARD_BLOCK.name,
                        onClick = {
                            draft = draft.copy(outsideWindowMode = OutsideWindowMode.HARD_BLOCK.name)
                        },
                        label = { Text("Полный запрет") },
                        colors = chipColors
                    )
                }

                CategoryToggle("Математика", draft.mathEnabled) {
                    draft = draft.copy(mathEnabled = it)
                }
                CategoryToggle("Чтение", draft.readingEnabled) {
                    draft = draft.copy(readingEnabled = it)
                }
                CategoryToggle("Логика", draft.logicEnabled) {
                    draft = draft.copy(logicEnabled = it)
                }
                CategoryToggle("Подсказки", draft.hintsEnabled) {
                    draft = draft.copy(hintsEnabled = it)
                }

                Text("Ошибок до упрощения: ${draft.maxWrongBeforeEasier}")
                Slider(
                    value = draft.maxWrongBeforeEasier.toFloat(),
                    onValueChange = {
                        draft = draft.copy(maxWrongBeforeEasier = it.roundToInt().coerceIn(2, 5))
                    },
                    valueRange = 2f..5f,
                    steps = 2,
                    colors = SliderDefaults.colors(thumbColor = LlColors.MintDeep, activeTrackColor = LlColors.Mint)
                )
            }

            LlPrimaryButton("Сохранить", onClick = { onSave(draft) }, coral = true)
            LlGhostButton("Назад", onClick = onBack)
        }
    }
}

@Composable
private fun CategoryToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = LlColors.MintDeep,
                checkedThumbColor = LlColors.SurfaceElevated
            )
        )
    }
}

private fun difficultyLabel(offset: Int): String = when (offset) {
    -1 -> "Легче"
    1 -> "Сложнее"
    else -> "Норма"
}
