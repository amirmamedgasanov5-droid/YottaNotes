package design.noodle.showcase.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import design.noodle.components.NoodleButton
import design.noodle.components.NoodleButtonSize
import design.noodle.components.NoodleButtonStyle
import design.noodle.components.NoodleCard
import design.noodle.components.NoodleGroup
import design.noodle.components.NoodleIconButton
import design.noodle.components.NoodleLargeTitleList
import design.noodle.components.NoodleLogo
import design.noodle.components.NoodlePageIndicator
import design.noodle.components.NoodleSectionTitle
import design.noodle.components.NoodleTag
import design.noodle.components.NoodleText
import design.noodle.components.NoodleToastTone
import design.noodle.components.NoodleWaves
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import androidx.compose.ui.unit.sp
import design.noodle.showcase.ShowcaseState
import design.noodle.theme.NoodlePalette
import design.noodle.theme.NoodleTheme
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(state: LazyListState, ui: ShowcaseState, bottomPad: Dp, onNavigate: (Int) -> Unit) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography

    NoodleLargeTitleList(
        title = "Noodle",
        state = state,
        bottomPadding = bottomPad,
        trailing = {
            NoodleIconButton(
                Icons.Outlined.Notifications,
                onClick = { ui.toast.show("Новых уведомлений нет", Icons.Outlined.Notifications) },
                container = androidx.compose.ui.graphics.Color.Transparent,
            )
        },
    ) {
        item {
            Hero(
                onTry = { onNavigate(1) },
                onToast = { ui.toast.show("Добро пожаловать в Noodle", Icons.Outlined.Favorite, NoodleToastTone.Success) },
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
        item { NoodleSectionTitle("Цвет") }
        item { Spacer(Modifier.height(12.dp)) }
        item { Palette() }

        item { Spacer(Modifier.height(28.dp)) }
        item { NoodleSectionTitle("Что внутри") }
        item { Spacer(Modifier.height(12.dp)) }
        item { FeatureCarousel() }

        item { Spacer(Modifier.height(28.dp)) }
        item {
            NoodleGroup(header = "Типографика", footer = "Шкала от Large Title до Caption — как в системных приложениях iOS.") {
                row { Sample("Large Title", type.largeTitle) }
                row { Sample("Title 1", type.title1) }
                row { Sample("Title 2", type.title2) }
                row { Sample("Headline", type.headline) }
                row { Sample("Body — основной текст", type.body) }
                row { Sample("Subhead — вспомогательный", type.subhead, colors.labelSecondary) }
                row { Sample("Footnote · Caption", type.footnote, colors.labelSecondary) }
            }
        }
    }
}

@Composable
private fun Sample(text: String, style: androidx.compose.ui.text.TextStyle, color: Color = Color.Unspecified) {
    NoodleText(text, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), style = style, color = color, maxLines = 1)
}

@Composable
private fun Hero(onTry: () -> Unit, onToast: () -> Unit) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        NoodleLogo(diameter = 48.dp, onClick = onToast)
        Spacer(Modifier.height(28.dp))
        NoodleText("Тихий интерфейс.\nОдна нить.", style = type.title1.copy(fontSize = 30.sp, lineHeight = 36.sp))
        Spacer(Modifier.height(12.dp))
        NoodleText(
            "Noodle — спокойная дизайн-система: монохром, воздух и тонкая волна, которая показывает, где вы сейчас.",
            style = type.callout,
            color = colors.labelSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            NoodleButton("Компоненты", onClick = onTry, size = NoodleButtonSize.Medium)
            NoodleButton("Привет", onClick = onToast, style = NoodleButtonStyle.Tinted, size = NoodleButtonSize.Medium)
        }
    }
}

private class Swatch(val name: String, val color: Color, val bordered: Boolean = false)

@Composable
private fun Palette() {
    val colors = NoodleTheme.colors
    val swatches = listOf(
        Swatch("Ink", colors.label),
        Swatch("Fill", colors.fill),
        Swatch("Paper", colors.background, true),
        Swatch("Amber", NoodlePalette.Amber),
        Swatch("Clay", NoodlePalette.Clay),
        Swatch("Sage", NoodlePalette.Sage),
        Swatch("Sky", NoodlePalette.Sky),
        Swatch("Rose", NoodlePalette.Rose),
    )
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        items(swatches) { s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).ink(CircleShape, s.color, border = if (s.bordered) 1.dp else 0.dp))
                NoodleText(s.name, Modifier.padding(top = 8.dp), style = NoodleTheme.typography.caption1, color = colors.labelSecondary)
            }
        }
    }
}

@Composable
private fun FeatureCarousel() {
    val colors = NoodleTheme.colors
    val pager = rememberPagerState(pageCount = { 3 })
    val cards = listOf(
        Triple("Упругая физика", "Каждое нажатие и переход построены на пружинах, а не на линейных кривых.", Icons.Outlined.Favorite),
        Triple("Живая тема", "Смена светлой/тёмной темы и акцента плавно перекрашивает весь интерфейс.", Icons.Outlined.Star),
        Triple("Тактильность", "Лёгкие хаптики на переключениях, табах, колесе и слайдере.", Icons.Outlined.PlayArrow),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val (title, body, icon) = cards[page]
            NoodleCard(
                Modifier.padding(bottom = 2.dp).graphicsLayer {
                    val off = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                    val s = lerp(1f, 0.92f, off)
                    scaleX = s
                    scaleY = s
                    alpha = lerp(1f, 0.6f, off)
                },
                shape = SquircleShape(26.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Box(
                        Modifier.size(40.dp).ink(CircleShape, colors.fillTertiary, border = 0.dp),
                        contentAlignment = Alignment.Center,
                    ) { design.noodle.components.NoodleIcon(icon, tint = colors.label, iconSize = 20.dp) }
                    Spacer(Modifier.height(14.dp))
                    NoodleText(title, style = NoodleTheme.typography.title3)
                    Spacer(Modifier.height(4.dp))
                    FixedLinesText(body, style = NoodleTheme.typography.subhead, color = colors.labelSecondary, lines = 3)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        NoodlePageIndicator(pageCount = 3, currentPage = pager.currentPage)
    }
}

/** Резервирует высоту под N строк, чтобы карточки в карусели были одинаковыми. */
@Composable
private fun FixedLinesText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    lines: Int,
) {
    val lineH = style.lineHeight.value.dp
    Box(Modifier.height(lineH * lines)) {
        NoodleText(text, style = style, color = color)
    }
}
