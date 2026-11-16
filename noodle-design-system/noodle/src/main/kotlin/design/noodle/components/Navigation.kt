package design.noodle.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import design.noodle.foundation.noodleClickable
import design.noodle.theme.NoodleTheme

/**
 * Навигационная панель iOS: заголовок по центру, слева/справа — действия.
 * [backgroundAlpha] и [titleAlpha] позволяют плавно проявлять панель при скролле.
 */
@Composable
fun NoodleNavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    titleAlpha: Float = 1f,
    backgroundAlpha: Float = 1f,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = NoodleTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(colors.background.copy(alpha = backgroundAlpha))
                val h = 2.dp.toPx()
                drawRect(colors.outline.copy(alpha = backgroundAlpha), Offset(0f, size.height - h), Size(size.width, h))
            }
            .statusBarsPadding()
            .height(44.dp)
            .padding(horizontal = 8.dp),
    ) {
        Row(Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically, content = leading)
        NoodleText(
            title,
            Modifier.align(Alignment.Center).graphicsLayer { alpha = titleAlpha; translationY = (1f - titleAlpha) * 6.dp.toPx() },
            style = NoodleTheme.typography.title3.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
            maxLines = 1,
        )
        Row(
            Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = trailing,
        )
    }
}

/** Кнопка «‹ Назад» в стиле iOS. */
@Composable
fun NoodleBackButton(onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = "Назад") {
    val colors = NoodleTheme.colors
    Row(
        modifier.noodleClickable(pressedScale = 0.94f, onClick = onClick).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoodleIcon(Icons.Rounded.KeyboardArrowLeft, tint = colors.label, iconSize = 30.dp)
        if (label != null) NoodleText(label, style = NoodleTheme.typography.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold), color = colors.label)
    }
}

/**
 * Экран с «большим заголовком» (Large Title): при прокрутке заголовок уезжает,
 * а компактный плавно проявляется в панели с полупрозрачным фоном.
 */
@Composable
fun NoodleLargeTitleList(
    title: String,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val colors = NoodleTheme.colors
    val density = LocalDensity.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 44.dp
    val thresholdPx = with(density) { 36.dp.toPx() }
    val collapse by remember(state) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / thresholdPx).coerceIn(0f, 1f)
        }
    }
    val smooth by animateFloatAsState(collapse, tween(90), label = "collapse")

    Box(modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = top, bottom = bottomPadding),
        ) {
            item(key = "__noodle_large_title") {
                Column(
                    Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 18.dp)
                        .graphicsLayer { alpha = 1f - smooth * 0.9f },
                ) {
                    NoodleText(title, style = NoodleTheme.typography.largeTitle, maxLines = 1)
                    NoodleSquiggle(Modifier.padding(top = 6.dp), color = colors.primary, width = 92.dp, thickness = 5.dp, amplitude = 4.dp, wavelength = 22.dp)
                }
            }
            content()
        }
        NoodleNavigationBar(
            title = title,
            modifier = Modifier.align(Alignment.TopCenter),
            titleAlpha = smooth,
            backgroundAlpha = smooth,
            leading = leading,
            trailing = trailing,
        )
    }
}
