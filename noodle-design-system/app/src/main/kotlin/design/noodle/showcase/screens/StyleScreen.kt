package design.noodle.showcase.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import design.noodle.components.NoodleGroup
import design.noodle.components.NoodleIcon
import design.noodle.components.NoodleLargeTitleList
import design.noodle.components.NoodleLogo
import design.noodle.components.NoodleRow
import design.noodle.components.NoodleSegmentedControl
import design.noodle.components.NoodleSwitch
import design.noodle.components.NoodleText
import design.noodle.foundation.noodleClickable
import design.noodle.showcase.ShowcaseState
import design.noodle.theme.NoodleAccent
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

@Composable
fun StyleScreen(state: LazyListState, ui: ShowcaseState, bottomPad: Dp) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography

    NoodleLargeTitleList(title = "Стиль", state = state, bottomPadding = bottomPad) {
        item {
            NoodleGroup(header = "Тема", footer = "Переключение перекрашивает весь интерфейс плавным переходом.") {
                row {
                    Column(Modifier.padding(16.dp)) {
                        NoodleSegmentedControl(listOf("Системная", "Светлая", "Тёмная"), ui.themeMode, { ui.themeMode = it })
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Акцентный цвет", footer = "Градиент, кнопки, переключатели и таб-бар подстраиваются под выбранный акцент.") {
                row {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        NoodleAccent.all.forEach { a ->
                            AccentDot(a, selected = ui.accent.name == a.name) { ui.accent = a }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Отклик", dividerInset = 58.dp) {
                row {
                    NoodleRow(
                        "Тактильный отклик",
                        subtitle = "Лёгкие «тики» на переключателях, табах и колесе",
                        icon = Icons.Rounded.Favorite,
                        iconTint = colors.danger,
                        trailing = { NoodleSwitch(ui.haptics, { ui.haptics = it }) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NoodleLogo(diameter = 88.dp, onClick = { ui.toast.show("Noodle — это про вкус", Icons.Rounded.Star) })
                NoodleText("Noodle Design System", Modifier.padding(top = 14.dp), style = type.headline)
                NoodleText("Версия 1.0.0", Modifier.padding(top = 2.dp), style = type.footnote, color = colors.labelSecondary)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun AccentDot(accent: NoodleAccent, selected: Boolean, onClick: () -> Unit) {
    val colors = NoodleTheme.colors
    val ring by animateFloatAsState(if (selected) 1f else 0f, NoodleMotion.bouncy(), label = "ring")
    Box(
        Modifier
            .size(48.dp)
            .noodleClickable(pressedScale = 0.88f, onClick = onClick)
            .graphicsLayer {
                scaleX = 1f + 0.08f * ring
                scaleY = 1f + 0.08f * ring
            }
            .border(2.5.dp, accent.primary.copy(alpha = ring), CircleShape)
            .padding(5.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent.secondary, accent.primary))),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) NoodleIcon(Icons.Rounded.Check, tint = Color.White, iconSize = 22.dp)
    }
}
