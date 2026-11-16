package design.noodle.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import design.noodle.foundation.ink
import design.noodle.foundation.NoodleShapes
import design.noodle.foundation.noRippleClickable
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
class NoodleTabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badge: Int? = null,
)

/**
 * Плавающий «стеклянный» таб-бар: капсула над контентом, под активной вкладкой
 * пружинит и растягивается подсветка. Узнаваемая подпись Noodle.
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
            .padding(start = 16.dp, end = 21.dp, top = 8.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .height(68.dp)
                .ink(NoodleShapes.Capsule, colors.surface, elevation = 5.dp, border = 2.5.dp)
                .padding(6.dp),
        ) {
            val tabW = maxWidth / items.size
            val targetX = tabW * selectedIndex
            val x by animateDpAsState(targetX, spring(dampingRatio = 0.58f, stiffness = 380f), label = "pillX")
            val stretch = (abs((targetX - x).value) / tabW.value).coerceIn(0f, 1f)

            Box(
                Modifier
                    .offset { IntOffset(x.roundToPx(), 0) }
                    .width(tabW)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleX = 1f + stretch * 0.22f
                        scaleY = 1f - stretch * 0.08f
                    }
                    .ink(NoodleShapes.Capsule, colors.primarySecondary, elevation = 0.dp, border = 2.dp),
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
    val tint by animateColorAsState(if (selected) colors.onPrimary else colors.labelSecondary, tween(160), label = "tabTint")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 560f))
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box {
            NoodleIcon(
                if (selected) item.selectedIcon else item.icon,
                Modifier.graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                },
                tint = tint,
                iconSize = 26.dp,
            )
            if (item.badge != null && item.badge > 0) {
                NoodleBadge(item.badge, Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-5).dp))
            }
        }
        NoodleText(
            item.label,
            Modifier.padding(top = 2.dp),
            style = NoodleTheme.typography.caption2.copy(fontSize = 9.5.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Bold),
            color = tint,
            maxLines = 1,
        )
    }
}

/** Классический таб-бар iOS: на всю ширину, тонкая линия сверху, иконка + подпись. */
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
            .background(colors.surface)
            .drawBehind {
                drawRect(colors.outline, Offset.Zero, Size(size.width, 2.dp.toPx()))
            }
            .navigationBarsPadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp)) {
            items.forEachIndexed { i, item ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .noRippleClickable {
                            if (i != selectedIndex) {
                                haptics.tick()
                                onSelect(i)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    TabCell(item, i == selectedIndex, Modifier.fillMaxSize())
                }
            }
        }
    }
}

/** Верхние прокручиваемые вкладки со скользящим подчёркиванием. */
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

    Box(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .drawBehind {
                if (w > 0f) {
                    squiggle(
                        color = colors.primary,
                        stroke = 4.dp.toPx(),
                        amplitude = 2.5.dp.toPx(),
                        wavelength = 13.dp.toPx(),
                        phase = 0f,
                        y = size.height - 5.dp.toPx(),
                        x0 = x + 10.dp.toPx(),
                        x1 = x + w - 10.dp.toPx(),
                    )
                }
            },
    ) {
        Row {
            titles.forEachIndexed { i, title ->
                val selected = i == selectedIndex
                val c by animateColorAsState(if (selected) colors.label else colors.labelSecondary, tween(180), label = "tabText")
                NoodleText(
                    title,
                    Modifier
                        .onGloballyPositioned { bounds[i] = it.positionInParent().x to it.size.width.toFloat() }
                        .noRippleClickable {
                            if (!selected) {
                                haptics.tick()
                                onSelect(i)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    style = NoodleTheme.typography.callout.copy(fontWeight = if (selected) FontWeight.Black else FontWeight.Bold),
                    color = c,
                    maxLines = 1,
                )
            }
        }
    }
}
