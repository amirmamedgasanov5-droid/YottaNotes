package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import design.noodle.foundation.inkPressable
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodlePalette
import design.noodle.theme.NoodleTheme
import kotlinx.coroutines.delay

/** Карточка: чернильный контур + жёсткая тень. Если задан onClick — вдавливается при нажатии. */
@Composable
fun NoodleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = SquircleShape(22.dp),
    background: Color = NoodleTheme.colors.surface,
    elevation: Dp = 5.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .padding(end = elevation, bottom = elevation)
            .then(
                if (onClick != null) Modifier.inkPressable(shape, background, elevation = elevation, role = null, onClick = onClick)
                else Modifier.ink(shape, background, elevation = elevation),
            ),
    ) { content() }
}

/** Бейдж-счётчик. */
@Composable
fun NoodleBadge(count: Int, modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.danger) {
    val text = if (count > 99) "99+" else count.toString()
    NoodleText(
        text,
        modifier
            .height(20.dp)
            .width(if (text.length == 1) 20.dp else (12 + text.length * 7).dp)
            .ink(NoodleShapes.Capsule, color, elevation = 0.dp, border = 1.8.dp)
            .padding(top = 1.dp),
        style = NoodleTheme.typography.caption2.copy(fontWeight = FontWeight.Black, fontSize = 11.sp),
        color = NoodleTheme.colors.outline,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

private val avatarPalette = listOf(
    NoodlePalette.Chili, NoodlePalette.Yolk, NoodlePalette.Mint, NoodlePalette.Sky, NoodlePalette.Bubble, NoodlePalette.Grape,
)

/** Аватар: плоский цвет по имени, контур, статус. */
@Composable
fun NoodleAvatar(
    name: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    online: Boolean? = null,
) {
    val colors = NoodleTheme.colors
    val c = avatarPalette[(name.hashCode() and 0x7fffffff) % avatarPalette.size]
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(modifier.size(diameter)) {
        Box(Modifier.size(diameter).ink(CircleShape, c, elevation = 0.dp, border = 2.dp), contentAlignment = Alignment.Center) {
            NoodleText(
                initials,
                style = NoodleTheme.typography.headline.copy(fontSize = (diameter.value * 0.36f).sp, fontWeight = FontWeight.Black),
                color = NoodlePalette.Ink,
            )
        }
        if (online != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(diameter * 0.32f)
                    .ink(CircleShape, if (online) colors.success else colors.fill, elevation = 0.dp, border = 2.dp),
            )
        }
    }
}

/** Индикатор страниц: активная точка вытягивается в цветную капсулу с контуром. */
@Composable
fun NoodlePageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = NoodleTheme.colors.primary,
    inactiveColor: Color = NoodleTheme.colors.surface,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { i ->
            val selected = i == currentPage
            val w by animateDpAsState(if (selected) 28.dp else 10.dp, spring(0.55f, 500f), label = "dotW")
            val c by animateColorAsState(if (selected) activeColor else inactiveColor, tween(160), label = "dotC")
            Box(Modifier.height(10.dp).width(w).ink(NoodleShapes.Capsule, c, elevation = 0.dp, border = 2.dp))
        }
    }
}

/** Столбчатая диаграмма: плоские столбики с контуром, выбранный — акцентный и «подпрыгивает». */
@Composable
fun NoodleBarChart(
    values: List<Float>,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
) {
    val colors = NoodleTheme.colors
    val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(1e-3f)
    Row(
        modifier.fillMaxWidth().height(chartHeight + 52.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            val grow = remember { Animatable(0f) }
            LaunchedEffect(v) {
                delay(i * 45L)
                grow.animateTo(1f, NoodleMotion.bouncy())
            }
            val selected = i == selectedIndex
            val lift = remember { Animatable(0f) }
            LaunchedEffect(selected) { lift.animateTo(if (selected) 1f else 0f, spring(0.4f, 600f)) }
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                NoodleText(
                    valueFormatter(v),
                    Modifier.graphicsLayer { alpha = lift.value.coerceIn(0f, 1f) }.padding(bottom = 6.dp),
                    style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.Black),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(end = 3.dp, bottom = 3.dp)
                        .height(chartHeight * (v / maxV))
                        .graphicsLayer {
                            scaleY = grow.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            translationY = -lift.value * 6.dp.toPx()
                        }
                        .inkPressable(
                            SquircleShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 4.dp, bottomStart = 4.dp),
                            if (selected) colors.primary else colors.surface,
                            elevation = 3.dp, haptic = true, role = null,
                        ) { onSelect(i) },
                )
                NoodleText(
                    labels.getOrElse(i) { "" },
                    Modifier.padding(top = 8.dp),
                    style = NoodleTheme.typography.caption1.copy(fontWeight = if (selected) FontWeight.Black else FontWeight.Bold),
                    color = if (selected) colors.label else colors.labelSecondary,
                )
            }
        }
    }
}
