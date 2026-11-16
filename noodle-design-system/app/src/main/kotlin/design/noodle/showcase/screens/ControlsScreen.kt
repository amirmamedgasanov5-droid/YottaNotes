package design.noodle.showcase.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.components.NoodleButton
import design.noodle.components.NoodleButtonSize
import design.noodle.components.NoodleButtonStyle
import design.noodle.components.NoodleCheckbox
import design.noodle.components.NoodleChip
import design.noodle.components.NoodleGroup
import design.noodle.components.NoodleIconButton
import design.noodle.components.NoodleLargeTitleList
import design.noodle.components.NoodleRadio
import design.noodle.components.NoodleRow
import design.noodle.components.NoodleSearchBar
import design.noodle.components.NoodleSegmentedControl
import design.noodle.components.NoodleSlider
import design.noodle.components.NoodleStepper
import design.noodle.components.NoodleSwitch
import design.noodle.components.NoodleText
import design.noodle.components.NoodleTextField
import design.noodle.components.NoodleToastTone
import design.noodle.components.NoodleWheelPicker
import design.noodle.showcase.ShowcaseState
import design.noodle.theme.NoodleTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ControlsScreen(state: LazyListState, ui: ShowcaseState, bottomPad: Dp) {
    val colors = NoodleTheme.colors
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var faceId by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(true) }
    var check1 by remember { mutableStateOf(true) }
    var check2 by remember { mutableStateOf(false) }
    var check3 by remember { mutableStateOf(false) }
    var radio by remember { mutableIntStateOf(1) }
    var segment by remember { mutableIntStateOf(0) }
    var volume by remember { mutableFloatStateOf(0.6f) }
    var steps by remember { mutableFloatStateOf(3f) }
    var qty by remember { mutableIntStateOf(2) }
    var chips by remember { mutableStateOf(setOf("Суп")) }
    var query by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("noodle@") }
    var hour by remember { mutableIntStateOf(8) }
    var minute by remember { mutableIntStateOf(30) }

    NoodleLargeTitleList(title = "Контролы", state = state, bottomPadding = bottomPad) {
        // ─── Кнопки
        item {
            NoodleGroup(header = "Кнопки") {
                row {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NoodleButton("Продолжить", onClick = { ui.toast.show("Нажато", Icons.Rounded.Star) }, Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NoodleButton("Tinted", {}, Modifier.weight(1f), style = NoodleButtonStyle.Tinted)
                            NoodleButton("Gray", {}, Modifier.weight(1f), style = NoodleButtonStyle.Gray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            NoodleButton("Medium", {}, size = NoodleButtonSize.Medium, icon = Icons.Rounded.Add)
                            NoodleButton("Small", {}, size = NoodleButtonSize.Small, style = NoodleButtonStyle.Tinted)
                            NoodleButton("Удалить", {}, size = NoodleButtonSize.Small, style = NoodleButtonStyle.Destructive)
                        }
                        NoodleButton(
                            if (loading) "Отправляем…" else "Нажми — загрузка",
                            onClick = {
                                loading = true
                                scope.launch {
                                    delay(1800)
                                    loading = false
                                    ui.toast.show("Готово!", Icons.Rounded.Favorite, NoodleToastTone.Success)
                                }
                            },
                            Modifier.fillMaxWidth(),
                            style = NoodleButtonStyle.Tinted,
                            loading = loading,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NoodleIconButton(Icons.Rounded.Share, {})
                            NoodleIconButton(Icons.Rounded.Favorite, {}, tint = colors.danger)
                            NoodleIconButton(Icons.Rounded.Notifications, {})
                            NoodleIconButton(Icons.Rounded.Add, {}, tint = colors.onPrimary, container = colors.primary)
                        }
                    }
                }
            }
        }

        // ─── Переключатели
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Переключатели", dividerInset = 58.dp, footer = "Бегунок растягивается при нажатии и пружинит — попробуйте потянуть его.") {
                row { NoodleRow("Уведомления", icon = Icons.Rounded.Notifications, iconTint = colors.danger, trailing = { NoodleSwitch(notifications, { notifications = it }) }) }
                row { NoodleRow("Face ID", icon = Icons.Rounded.Lock, iconTint = colors.success, trailing = { NoodleSwitch(faceId, { faceId = it }) }) }
                row { NoodleRow("Избранное", icon = Icons.Rounded.Star, iconTint = colors.warning, trailing = { NoodleSwitch(favorites, { favorites = it }) }) }
            }
        }

        // ─── Выбор
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Чекбоксы") {
                row { NoodleRow("Лапша удон", trailing = { NoodleCheckbox(check1, { check1 = it }) }, onClick = { check1 = !check1 }) }
                row { NoodleRow("Соба", trailing = { NoodleCheckbox(check2, { check2 = it }) }, onClick = { check2 = !check2 }) }
                row { NoodleRow("Рамен", trailing = { NoodleCheckbox(check3, { check3 = it }) }, onClick = { check3 = !check3 }) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Радио") {
                listOf("Острый", "Средний", "Не острый").forEachIndexed { i, label ->
                    row { NoodleRow(label, trailing = { NoodleRadio(radio == i, { radio = i }) }, onClick = { radio = i }) }
                }
            }
        }

        // ─── Чипы
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Чипы") {
                row {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("Суп", "Wok", "Острое", "Веган", "Десерты", "Напитки").forEach { c ->
                            NoodleChip(c, chips.contains(c), { chips = if (chips.contains(c)) chips - c else chips + c })
                        }
                    }
                }
            }
        }

        // ─── Значения
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Значения") {
                row {
                    Column(Modifier.padding(16.dp)) {
                        NoodleSegmentedControl(listOf("День", "Неделя", "Месяц"), segment, { segment = it })
                    }
                }
                row {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NoodleText("Громкость", Modifier.weight(1f))
                            NoodleText("${(volume * 100).roundToInt()}%", color = colors.labelSecondary)
                        }
                        NoodleSlider(volume, { volume = it })
                    }
                }
                row {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NoodleText("Шаги", Modifier.weight(1f))
                            NoodleText("${steps.roundToInt()}", color = colors.labelSecondary)
                        }
                        NoodleSlider(steps, { steps = it }, valueRange = 0f..6f, steps = 5)
                    }
                }
                row {
                    NoodleRow("Порций", subtitle = "Нажмите − или +", trailing = { NoodleStepper(qty, { qty = it }, range = 0..9) }, value = "$qty")
                }
            }
        }

        // ─── Поля ввода
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NoodleSearchBar(query, { query = it })
                NoodleTextField(name, { name = it }, label = "Имя", placeholder = "Как вас зовут?", leadingIcon = Icons.Rounded.Person)
                NoodleTextField(
                    email, { email = it },
                    label = "Почта",
                    placeholder = "you@noodle.design",
                    leadingIcon = Icons.Rounded.Email,
                    isError = email.isNotBlank() && !email.contains(Regex("^\\S+@\\S+\\.\\S+$")),
                    supportingText = if (email.isNotBlank() && !email.contains(Regex("^\\S+@\\S+\\.\\S+$"))) "Введите корректный адрес" else null,
                )
            }
        }

        // ─── Колесо
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Будильник", footer = "Колесо магнитится к значению, соседние строки уменьшаются и тускнеют.") {
                row {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            NoodleWheelPicker(
                                List(24) { it.toString().padStart(2, '0') },
                                hour, { hour = it }, Modifier.weight(1f), visibleCount = 5,
                            )
                            NoodleText(":", style = NoodleTheme.typography.title2)
                            NoodleWheelPicker(
                                List(60) { it.toString().padStart(2, '0') },
                                minute, { minute = it }, Modifier.weight(1f), visibleCount = 5,
                            )
                        }
                        NoodleText(
                            "Разбудить в ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                            style = NoodleTheme.typography.footnote,
                            color = colors.labelSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
