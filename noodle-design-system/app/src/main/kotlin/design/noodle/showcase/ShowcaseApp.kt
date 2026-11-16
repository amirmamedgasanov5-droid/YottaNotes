package design.noodle.showcase

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import design.noodle.components.NoodleActionSheet
import design.noodle.components.NoodleAlert
import design.noodle.components.NoodleAlertAction
import design.noodle.components.NoodleAlertActionStyle
import design.noodle.components.NoodleBottomSheet
import design.noodle.components.NoodleButton
import design.noodle.components.NoodleButtonStyle
import design.noodle.components.NoodleFloatingTabBar
import design.noodle.components.NoodleGroup
import design.noodle.components.NoodleRow
import design.noodle.components.NoodleTabItem
import design.noodle.components.NoodleText
import design.noodle.components.NoodleToastHost
import design.noodle.components.NoodleToastState
import design.noodle.components.NoodleToastTone
import design.noodle.theme.NoodleAccent
import design.noodle.theme.NoodleTheme
import design.noodle.showcase.screens.ControlsScreen
import design.noodle.showcase.screens.HomeScreen
import design.noodle.showcase.screens.ListsScreen
import design.noodle.showcase.screens.MotionScreen
import design.noodle.showcase.screens.StyleScreen

/** Общее состояние демо: оверлеи, тосты, настройки внешнего вида. */
@Stable
class ShowcaseState {
    val toast = NoodleToastState()
    var sheet by mutableStateOf(false)
    var alert by mutableStateOf(false)
    var actionSheet by mutableStateOf(false)

    var themeMode by mutableIntStateOf(0) // 0 системная, 1 светлая, 2 тёмная
    var accent by mutableStateOf(NoodleAccent.Chili)
    var haptics by mutableStateOf(true)
}

@Composable
fun ShowcaseApp() {
    val ui = remember { ShowcaseState() }
    val systemDark = isSystemInDarkTheme()
    val dark = when (ui.themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val activity = LocalContext.current as ComponentActivity
    LaunchedEffect(dark) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    NoodleTheme(darkTheme = dark, accent = ui.accent, hapticsEnabled = ui.haptics) {
        ShowcaseScaffold(ui)
    }
}

@Composable
internal fun ShowcaseScaffold(ui: ShowcaseState, initialTab: Int = 0) {
    val colors = NoodleTheme.colors
    var tab by remember { mutableIntStateOf(initialTab) }
    val listStates = List(5) { rememberLazyListState() }
    val bottomPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 108.dp

    val tabs = remember {
        listOf(
            NoodleTabItem("Главная", Icons.Outlined.Home),
            NoodleTabItem("Контролы", Icons.Outlined.Build),
            NoodleTabItem("Списки", Icons.Outlined.FavoriteBorder, badge = 3),
            NoodleTabItem("Движение", Icons.Outlined.PlayArrow),
            NoodleTabItem("Стиль", Icons.Outlined.Settings),
        )
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(260, delayMillis = 60)) + scaleIn(tween(320), initialScale = 0.97f)) togetherWith fadeOut(tween(120))
            },
            label = "tabs",
        ) { page ->
            when (page) {
                0 -> HomeScreen(listStates[0], ui, bottomPad) { tab = it }
                1 -> ControlsScreen(listStates[1], ui, bottomPad)
                2 -> ListsScreen(listStates[2], ui, bottomPad)
                3 -> MotionScreen(listStates[3], ui, bottomPad)
                else -> StyleScreen(listStates[4], ui, bottomPad)
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(bottomPad)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, colors.background, colors.background))),
        )
        NoodleFloatingTabBar(
            items = tabs,
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Оверлеи
        NoodleBottomSheet(visible = ui.sheet, onDismissRequest = { ui.sheet = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                NoodleText(
                    "Шторка Noodle",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = NoodleTheme.typography.title2,
                )
                NoodleText(
                    "Потяните вниз, чтобы закрыть. Закрытие учитывает скорость жеста — как в iOS.",
                    Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
                    style = NoodleTheme.typography.subhead,
                    color = colors.labelSecondary,
                )
                NoodleGroup {
                    row { NoodleRow("Поделиться", icon = Icons.Outlined.Star, iconTint = colors.info, showChevron = true, onClick = {}) }
                    row { NoodleRow("В избранное", icon = Icons.Outlined.Favorite, iconTint = colors.danger, showChevron = true, onClick = {}) }
                }
                NoodleButton(
                    "Готово",
                    onClick = {
                        ui.sheet = false
                        ui.toast.show("Сохранено", Icons.Outlined.Star, NoodleToastTone.Success)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 20.dp),
                )
                NoodleButton(
                    "Закрыть",
                    onClick = { ui.sheet = false },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                    style = NoodleButtonStyle.Plain,
                )
            }
        }

        NoodleAlert(
            visible = ui.alert,
            title = "Удалить заметку?",
            message = "Это действие нельзя отменить. Заметка исчезнет на всех устройствах.",
            onDismissRequest = { ui.alert = false },
            actions = listOf(
                NoodleAlertAction("Отмена", NoodleAlertActionStyle.Cancel),
                NoodleAlertAction("Удалить", NoodleAlertActionStyle.Destructive) {
                    ui.toast.show("Заметка удалена", Icons.Outlined.Star, NoodleToastTone.Danger)
                },
            ),
        )

        NoodleActionSheet(
            visible = ui.actionSheet,
            onDismissRequest = { ui.actionSheet = false },
            title = "Что сделать с файлом?",
            actions = listOf(
                NoodleAlertAction("Переименовать") { ui.toast.show("Переименовано", Icons.Outlined.Star, NoodleToastTone.Success) },
                NoodleAlertAction("Дублировать") { ui.toast.show("Копия создана", Icons.Outlined.Star) },
                NoodleAlertAction("Удалить", NoodleAlertActionStyle.Destructive) {
                    ui.toast.show("Файл удалён", Icons.Outlined.Star, NoodleToastTone.Danger)
                },
            ),
        )

        NoodleToastHost(ui.toast, Modifier.align(Alignment.TopCenter))
    }
}
