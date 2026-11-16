package design.noodle.showcase.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.components.NoodleBarChart
import design.noodle.components.NoodleButton
import design.noodle.components.NoodleButtonStyle
import design.noodle.components.NoodleCard
import design.noodle.components.NoodleGroup
import design.noodle.components.NoodleIndeterminateBar
import design.noodle.components.NoodleLargeTitleList
import design.noodle.components.NoodleLoader
import design.noodle.components.NoodleProgressBar
import design.noodle.components.NoodleProgressRing
import design.noodle.components.NoodleRow
import design.noodle.components.NoodleSegmentedControl
import design.noodle.components.NoodleSlider
import design.noodle.components.NoodleSpinner
import design.noodle.components.NoodleSwitch
import design.noodle.components.NoodleText
import design.noodle.components.NoodleToastTone
import design.noodle.components.skeleton
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.showcase.ShowcaseState
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme
import kotlin.math.roundToInt

@Composable
fun MotionScreen(state: LazyListState, ui: ShowcaseState, bottomPad: Dp) {
    val colors = NoodleTheme.colors

    var progress by remember { mutableFloatStateOf(0.64f) }
    var chartSel by remember { mutableIntStateOf(3) }
    var springKind by remember { mutableIntStateOf(2) }
    var ballRight by remember { mutableStateOf(false) }
    var skeletonOn by remember { mutableStateOf(true) }

    NoodleLargeTitleList(title = "Движение", state = state, bottomPadding = bottomPad) {
        // ─── Оверлеи
        item {
            NoodleGroup(header = "Оверлеи", footer = "Шторка, алерт и action sheet — со своей физикой появления и жестами.") {
                row {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            NoodleButton("Шторка", { ui.sheet = true }, Modifier.weight(1f), style = NoodleButtonStyle.Tinted)
                            NoodleButton("Алерт", { ui.alert = true }, Modifier.weight(1f), style = NoodleButtonStyle.Tinted)
                        }
                        NoodleButton("Action Sheet", { ui.actionSheet = true }, Modifier.fillMaxWidth(), style = NoodleButtonStyle.Gray)
                    }
                }
            }
        }

        // ─── Тосты
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Тосты", footer = "Выпрыгивают сверху, как Dynamic Island.") {
                row { NoodleRow("Успех", icon = Icons.Rounded.Favorite, iconTint = colors.success, showChevron = true, onClick = { ui.toast.show("Всё получилось", Icons.Rounded.Favorite, NoodleToastTone.Success) }) }
                row { NoodleRow("Предупреждение", icon = Icons.Rounded.Warning, iconTint = colors.warning, showChevron = true, onClick = { ui.toast.show("Слабый сигнал сети", Icons.Rounded.Warning, NoodleToastTone.Warning) }) }
                row { NoodleRow("Ошибка", icon = Icons.Rounded.Info, iconTint = colors.danger, showChevron = true, onClick = { ui.toast.show("Не удалось сохранить", Icons.Rounded.Info, NoodleToastTone.Danger) }) }
            }
        }

        // ─── Прогресс
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Прогресс и загрузка") {
                row {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NoodleProgressRing(progress, diameter = 120.dp, strokeWidth = 12.dp) {
                            NoodleText("${(progress * 100).roundToInt()}%", style = NoodleTheme.typography.title1)
                        }
                        NoodleSlider(progress, { progress = it })
                        NoodleProgressBar(progress)
                        NoodleIndeterminateBar()
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                            NoodleSpinner(diameter = 28.dp, color = colors.labelSecondary)
                            NoodleSpinner(diameter = 28.dp, color = colors.primary)
                            NoodleLoader()
                        }
                    }
                }
            }
        }

        // ─── Диаграмма
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Диаграмма") {
                row {
                    Column(Modifier.padding(16.dp)) {
                        NoodleText("Порции за неделю", style = NoodleTheme.typography.headline)
                        NoodleText("Нажмите на столбец", style = NoodleTheme.typography.footnote, color = colors.labelSecondary)
                        Spacer(Modifier.height(12.dp))
                        NoodleBarChart(
                            values = listOf(24f, 42f, 31f, 58f, 47f, 70f, 36f),
                            labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"),
                            selectedIndex = chartSel,
                            onSelect = { chartSel = it },
                        )
                    }
                }
            }
        }

        // ─── Пружины
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Физика пружин", footer = "Три характера движения: smooth, snappy, bouncy и gentle.") {
                row {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NoodleSegmentedControl(listOf("Smooth", "Snappy", "Bouncy", "Gentle"), springKind, { springKind = it })
                        val x by animateDpAsState(
                            targetValue = if (ballRight) 232.dp else 0.dp,
                            animationSpec = when (springKind) {
                                0 -> NoodleMotion.smooth()
                                1 -> NoodleMotion.snappy()
                                2 -> NoodleMotion.bouncy()
                                else -> NoodleMotion.gentle()
                            },
                            label = "ball",
                        )
                        Box(Modifier.fillMaxWidth().height(56.dp).clip(SquircleShape(16.dp)).background(colors.fillTertiary), contentAlignment = Alignment.CenterStart) {
                            Box(Modifier.padding(start = 8.dp).offset(x = x).size(40.dp).clip(CircleShape).background(colors.gradient))
                        }
                        NoodleButton("Запустить", { ballRight = !ballRight }, Modifier.fillMaxWidth(), style = NoodleButtonStyle.Tinted)
                    }
                }
            }
        }

        // ─── Скелетон
        item { Spacer(Modifier.height(24.dp)) }
        item {
            NoodleGroup(header = "Скелетон") {
                row { NoodleRow("Показывать загрузку", trailing = { NoodleSwitch(skeletonOn, { skeletonOn = it }) }) }
                row {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (skeletonOn) {
                            Box(Modifier.size(48.dp).skeleton(NoodleShapes.Capsule))
                            Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.fillMaxWidth(0.7f).height(14.dp).skeleton())
                                Box(Modifier.fillMaxWidth(0.45f).height(12.dp).skeleton())
                            }
                        } else {
                            design.noodle.components.NoodleAvatar("Анна Смирнова", diameter = 48.dp)
                            Column(Modifier.padding(start = 14.dp)) {
                                NoodleText("Анна Смирнова", style = NoodleTheme.typography.headline)
                                NoodleText("Дизайнер продукта", style = NoodleTheme.typography.footnote, color = colors.labelSecondary)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
