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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
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
                Icons.Rounded.Notifications,
                onClick = { ui.toast.show("Новых уведомлений нет", Icons.Rounded.Notifications) },
                container = androidx.compose.ui.graphics.Color.Transparent,
            )
        },
    ) {
        item {
            Hero(
                onTry = { onNavigate(1) },
                onToast = { ui.toast.show("Добро пожаловать в Noodle", Icons.Rounded.Favorite, NoodleToastTone.Success) },
            )
        }

        item { Spacer(Modifier.height(28.dp)) }
        item { NoodleSectionTitle("Палитра") }
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
    val shape = SquircleShape(30.dp)
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.gradient),
    ) {
        NoodleWaves(Modifier.matchParentSize().padding(top = 70.dp), amplitude = 14.dp, strands = 5)
        Column(Modifier.padding(22.dp)) {
            NoodleLogo(diameter = 64.dp, onClick = onToast)
            Spacer(Modifier.height(18.dp))
            NoodleText("Noodle\nDesign System", style = type.title1.copy(fontWeight = FontWeight.ExtraBold), color = Color.White)
            Spacer(Modifier.height(8.dp))
            NoodleText(
                "Тёплый, упругий, живой. Компоненты в духе Apple с собственным характером.",
                style = type.callout,
                color = Color.White.copy(alpha = 0.92f),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).background(Color.White)) {
                    NoodleButton(
                        "Смотреть",
                        onClick = onTry,
                        style = NoodleButtonStyle.Plain,
                        size = NoodleButtonSize.Medium,
                        icon = Icons.Rounded.PlayArrow,
                    )
                }
                NoodleTag("40+ компонентов", color = Color.White)
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

private class Swatch(val name: String, val a: Color, val b: Color)

@Composable
private fun Palette() {
    val swatches = listOf(
        Swatch("Chili", NoodlePalette.Chili, NoodlePalette.Yolk),
        Swatch("Naruto", NoodlePalette.Naruto, Color(0xFFFFAA6B)),
        Swatch("Scallion", NoodlePalette.Scallion, Color(0xFFA6E36B)),
        Swatch("Sky", NoodlePalette.Sky, Color(0xFF5ED6FF)),
        Swatch("Plum", NoodlePalette.Plum, Color(0xFFFF8AD8)),
        Swatch("Soy", NoodlePalette.Soy, Color(0xFF5A4034)),
    )
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(swatches) { s ->
            NoodleCard(shape = SquircleShape(22.dp)) {
                Column(Modifier.width(112.dp)) {
                    Box(Modifier.fillMaxWidth().height(72.dp).background(Brush.linearGradient(listOf(s.b, s.a))))
                    NoodleText(s.name, Modifier.padding(12.dp), style = NoodleTheme.typography.subhead.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun FeatureCarousel() {
    val colors = NoodleTheme.colors
    val pager = rememberPagerState(pageCount = { 3 })
    val cards = listOf(
        Triple("Упругая физика", "Каждое нажатие и переход построены на пружинах, а не на линейных кривых.", Icons.Rounded.Favorite),
        Triple("Живая тема", "Смена светлой/тёмной темы и акцента плавно перекрашивает весь интерфейс.", Icons.Rounded.Star),
        Triple("Тактильность", "Лёгкие хаптики на переключениях, табах, колесе и слайдере.", Icons.Rounded.PlayArrow),
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 28.dp),
            pageSpacing = 12.dp,
        ) { page ->
            val (title, body, icon) = cards[page]
            NoodleCard(
                Modifier.graphicsLayer {
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
                        Modifier.size(44.dp).clip(SquircleShape(13.dp)).background(colors.gradient),
                        contentAlignment = Alignment.Center,
                    ) { design.noodle.components.NoodleIcon(icon, tint = Color.White, iconSize = 24.dp) }
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
