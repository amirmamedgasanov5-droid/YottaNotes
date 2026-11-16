package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.ink
import design.noodle.foundation.noRippleClickable
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.theme.NoodleTheme
import kotlin.math.PI
import kotlin.math.roundToInt

@Immutable
class NoodleTabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badge: Int? = null,
)

/** Движущаяся «нить»: индикатор активной вкладки — тонкая волна, которая скользит пружиной. */
@Composable
private fun ThreadIndicator(modifier: Modifier = Modifier, color: Color) {
    val t = rememberInfiniteTransition(label = "thread")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.width(22.dp).height(6.dp)) {
        squiggle(color, 1.8.dp.toPx(), 1.8.dp.toPx(), 11.dp.toPx(), -phase, x0 = 1.dp.toPx(), x1 = size.width - 1.dp.toPx())
    }
}

/**
 * Плавающий таб-бар: тонкая капсула, иконка и подпись. Активная вкладка
 * отмечена скользящей нитью лапши — фирменная деталь Noodle.
 */
@Composable
fun NoodleFloatingTabBar(
    items: List<NoodleTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(64.dp)
                .shadow(16.dp, NoodleShapes.Capsule, ambientColor = Color.Black.copy(if (colors.isDark) 0.5f else 0.10f), spotColor = Color.Black.copy(if (colors.isDark) 0.5f else 0.14f))
                .ink(NoodleShapes.Capsule, colors.glass, border = 1.dp, outline = colors.outline)
                .padding(horizontal = 6.dp),
        ) {
            val tabW = maxWidth / items.size
            val x by androidx.compose.animation.core.animateDpAsState(
                tabW * selectedIndex + (tabW - 22.dp) / 2, spring(dampingRatio = 0.62f, stiffness = 420f), label = "threadX",
            )
            ThreadIndicator(
                Modifier.align(Alignment.BottomStart).offset { IntOffset(x.roundToPx(), -4.dp.roundToPx()) },
                colors.primary,
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, item ->
                    TabCell(
                        item = item,
                        selected = i == selectedIndex,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .noRippleClickable {
                                if (i != selectedIndex) {
                                    haptics.tick()
                                    onSelect(i)
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCell(item: NoodleTabItem, selected: Boolean, modifier: Modifier) {
    val colors = NoodleTheme.colors
    val tint by animateColorAsState(if (selected) colors.label else colors.labelTertiary, tween(180), label = "tabTint")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.86f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
    }
    Column(modifier.padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box {
            NoodleIcon(
                if (selected) item.selectedIcon else item.icon,
                Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
                tint = tint,
                iconSize = 23.dp,
            )
            if (item.badge != null && item.badge > 0) {
                NoodleBadge(item.badge, Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-5).dp))
            }
        }
        NoodleText(
            item.label,
            Modifier.padding(top = 3.dp),
            style = NoodleTheme.typography.caption2.copy(fontSize = 10.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
            color = tint,
            maxLines = 1,
        )
    }
}

/** Классический таб-бар на всю ширину: тонкая линия сверху, иконка + подпись, нить под активной. */
@Composable
fun NoodleTabBar(
    items: List<NoodleTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.background)
            .drawBehind { drawRect(colors.separator, Offset.Zero, Size(size.width, 1.dp.toPx())) }
            .navigationBarsPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(60.dp)) {
            val tabW = maxWidth / items.size
            val x by androidx.compose.animation.core.animateDpAsState(
                tabW * selectedIndex + (tabW - 22.dp) / 2, spring(dampingRatio = 0.62f, stiffness = 420f), label = "threadX",
            )
            ThreadIndicator(Modifier.align(Alignment.TopStart).offset { IntOffset(x.roundToPx(), 0) }, colors.primary)
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { i, item ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .noRippleClickable { if (i != selectedIndex) { haptics.tick(); onSelect(i) } },
                        contentAlignment = Alignment.Center,
                    ) { TabCell(item, i == selectedIndex, Modifier.fillMaxSize().padding(top = 8.dp)) }
                }
            }
        }
    }
}

/** Верхние прокручиваемые вкладки: активная отмечена бегущей нитью. */
@Composable
fun NoodleScrollableTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val target = bounds[selectedIndex]
    val x by animateFloatAsState(target?.first ?: 0f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "underlineX")
    val w by animateFloatAsState(target?.second ?: 0f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "underlineW")
    val t = rememberInfiniteTransition(label = "tabThread")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "phase",
    )

    Box(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .drawBehind {
                if (w > 0f) {
                    squiggle(
                        color = colors.primary,
                        stroke = 1.8.dp.toPx(),
                        amplitude = 1.6.dp.toPx(),
                        wavelength = 10.dp.toPx(),
                        phase = -phase,
                        y = size.height - 4.dp.toPx(),
                        x0 = x + 14.dp.toPx(),
                        x1 = x + w - 14.dp.toPx(),
                    )
                }
            },
    ) {
        Row {
            titles.forEachIndexed { i, title ->
                val selected = i == selectedIndex
                val c by animateColorAsState(if (selected) colors.label else colors.labelTertiary, tween(180), label = "tabText")
                NoodleText(
                    title,
                    Modifier
                        .onGloballyPositioned { bounds[i] = it.positionInParent().x to it.size.width.toFloat() }
                        .noRippleClickable { if (!selected) { haptics.tick(); onSelect(i) } }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    style = NoodleTheme.typography.callout.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
                    color = c,
                    maxLines = 1,
                )
            }
        }
    }
}
