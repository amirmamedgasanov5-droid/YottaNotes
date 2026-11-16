package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.noodleClickable
import design.noodle.foundation.softShadow
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodlePalette
import design.noodle.theme.NoodleTheme
import kotlinx.coroutines.delay

/** Карточка-поверхность с мягкой тенью. Если задан onClick — пружинит при нажатии. */
@Composable
fun NoodleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = SquircleShape(24.dp),
    background: Color = NoodleTheme.colors.surface,
    content: @Composable () -> Unit,
) {
    val colors = NoodleTheme.colors
    Box(
        modifier
            .then(if (onClick != null) Modifier.noodleClickable(pressedScale = 0.97f, onClick = onClick) else Modifier)
            .softShadow(if (colors.isDark) 0.dp else 6.dp, shape)
            .clip(shape)
            .background(background)
            .then(if (colors.isDark) Modifier.border(0.5.dp, colors.glassBorder, shape) else Modifier),
    ) { content() }
}

/** Бейдж-счётчик. */
@Composable
fun NoodleBadge(count: Int, modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.danger) {
    val text = if (count > 99) "99+" else count.toString()
    NoodleText(
        text,
        modifier
            .height(18.dp)
            .width(if (text.length == 1) 18.dp else (10 + text.length * 7).dp)
            .clip(NoodleShapes.Capsule)
            .background(color)
            .padding(top = 1.dp),
        style = NoodleTheme.typography.caption2.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
        color = Color.White,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        maxLines = 1,
    )
}

private val avatarPalette = listOf(
    NoodlePalette.Chili to NoodlePalette.Yolk,
    NoodlePalette.Naruto to Color(0xFFFFAA6B),
    NoodlePalette.Scallion to Color(0xFFA6E36B),
    NoodlePalette.Sky to Color(0xFF5ED6FF),
    NoodlePalette.Plum to Color(0xFFFF8AD8),
)

/** Аватар с градиентом, зависящим от имени, и необязательным статусом. */
@Composable
fun NoodleAvatar(
    name: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    online: Boolean? = null,
) {
    val colors = NoodleTheme.colors
    val (a, b) = avatarPalette[(name.hashCode() and 0x7fffffff) % avatarPalette.size]
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(modifier.size(diameter)) {
        Box(
            Modifier.size(diameter).clip(CircleShape).background(Brush.linearGradient(listOf(b, a))),
            contentAlignment = Alignment.Center,
        ) {
            NoodleText(
                initials,
                style = NoodleTheme.typography.headline.copy(fontSize = (diameter.value * 0.36f).sp, fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
        if (online != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(diameter * 0.3f)
                    .border(2.dp, colors.surface, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(if (online) colors.success else colors.labelTertiary),
            )
        }
    }
}

/** Индикатор страниц: активная точка «растягивается» в капсулу. */
@Composable
fun NoodlePageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = NoodleTheme.colors.primary,
    inactiveColor: Color = NoodleTheme.colors.fill,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { i ->
            val selected = i == currentPage
            val w by animateDpAsState(if (selected) 22.dp else 7.dp, NoodleMotion.snappy(), label = "dotW")
            val c by animateColorAsState(if (selected) activeColor else inactiveColor, tween(220), label = "dotC")
            Box(Modifier.height(7.dp).width(w).clip(CircleShape).background(c))
        }
    }
}

/** Анимированная столбчатая диаграмма. Нажмите на столбец — он подсвечивается. */
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
        modifier.fillMaxWidth().height(chartHeight + 44.dp),
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
            Column(
                Modifier.weight(1f).fillMaxHeight().noodleClickable(pressedScale = 0.94f) { onSelect(i) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                NoodleText(
                    valueFormatter(v),
                    Modifier.graphicsLayer { alpha = if (selected) 1f else 0f }.padding(bottom = 4.dp),
                    style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight * (v / maxV))
                        .graphicsLayer {
                            scaleY = grow.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(SquircleShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 4.dp, bottomStart = 4.dp))
                        .background(if (selected) colors.gradient else Brush.linearGradient(listOf(colors.fillSecondary, colors.fillSecondary))),
                )
                NoodleText(
                    labels.getOrElse(i) { "" },
                    Modifier.padding(top = 8.dp),
                    style = NoodleTheme.typography.caption1,
                    color = if (selected) colors.label else colors.labelSecondary,
                )
            }
        }
    }
}
