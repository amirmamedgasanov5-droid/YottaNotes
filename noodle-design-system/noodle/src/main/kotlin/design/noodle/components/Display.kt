package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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
import design.noodle.theme.NoodleTheme
import kotlinx.coroutines.delay

/** Карточка: чернильный контур + жёсткая тень. Если задан onClick — вдавливается при нажатии. */
@Composable
fun NoodleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = SquircleShape(20.dp),
    background: Color = NoodleTheme.colors.surface,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .then(
                if (onClick != null) Modifier.inkPressable(shape, background, role = null, onClick = onClick)
                else Modifier.ink(shape, background),
            ),
    ) { content() }
}

/** Бейдж-счётчик. */
@Composable
fun NoodleBadge(count: Int, modifier: Modifier = Modifier, color: Color = NoodleTheme.colors.label) {
    val text = if (count > 99) "99+" else count.toString()
    NoodleText(
        text,
        modifier
            .height(18.dp)
            .width(if (text.length == 1) 18.dp else (10 + text.length * 7).dp)
            .ink(NoodleShapes.Capsule, color, border = 0.dp)
            .padding(top = 1.dp),
        style = NoodleTheme.typography.caption2.copy(fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
        color = NoodleTheme.colors.background,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/** Аватар: монохромный круг с инициалами и тонким индикатором статуса. */
@Composable
fun NoodleAvatar(
    name: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 44.dp,
    online: Boolean? = null,
) {
    val colors = NoodleTheme.colors
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
    Box(modifier.size(diameter)) {
        Box(Modifier.size(diameter).ink(CircleShape, colors.fillSecondary, border = 0.dp), contentAlignment = Alignment.Center) {
            NoodleText(
                initials,
                style = NoodleTheme.typography.headline.copy(fontSize = (diameter.value * 0.34f).sp, fontWeight = FontWeight.Medium),
                color = colors.labelSecondary,
            )
        }
        if (online != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(diameter * 0.3f)
                    .ink(CircleShape, colors.surface, border = 0.dp)
                    .padding(2.dp)
                    .ink(CircleShape, if (online) colors.success else colors.labelQuaternary, border = 0.dp),
            )
        }
    }
}

/** Индикатор страниц: тонкие чёрточки, активная вытягивается. */
@Composable
fun NoodlePageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = NoodleTheme.colors.label,
    inactiveColor: Color = NoodleTheme.colors.labelQuaternary,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { i ->
            val selected = i == currentPage
            val w by animateDpAsState(if (selected) 22.dp else 12.dp, spring(0.8f, 500f), label = "dotW")
            val c by animateColorAsState(if (selected) activeColor else inactiveColor, tween(180), label = "dotC")
            Box(Modifier.height(2.dp).width(w).clip(NoodleShapes.Capsule).background(c))
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
                    style = NoodleTheme.typography.caption1.copy(fontWeight = FontWeight.Medium),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight * (v / maxV))
                        .graphicsLayer {
                            scaleY = grow.value
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .inkPressable(
                            SquircleShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 3.dp, bottomStart = 3.dp),
                            if (selected) colors.label else colors.fillSecondary,
                            border = 0.dp, haptic = true, role = null,
                        ) { onSelect(i) },
                )
                NoodleText(
                    labels.getOrElse(i) { "" },
                    Modifier.padding(top = 8.dp),
                    style = NoodleTheme.typography.caption1.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
                    color = if (selected) colors.label else colors.labelSecondary,
                )
            }
        }
    }
}
