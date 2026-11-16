package design.noodle.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import design.noodle.foundation.rememberNoodleHaptics
import design.noodle.theme.NoodleTheme
import kotlin.math.abs

/**
 * Барабан-пикер как в iOS: магнитится к строке, соседние элементы
 * уменьшаются и тускнеют, на каждой смене — лёгкий «тик».
 */
@Composable
fun NoodleWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 40.dp,
) {
    val colors = NoodleTheme.colors
    val haptics = rememberNoodleHaptics()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(state)
    val sidePad = itemHeight * (visibleCount / 2)

    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index ?: selectedIndex
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { centered }.collect { idx ->
            if (idx != selectedIndex) {
                haptics.tick()
                onSelectedChange(idx)
            }
        }
    }

    Box(modifier.fillMaxWidth().height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(itemHeight)
                .ink(SquircleShape(12.dp), colors.fillTertiary, border = 0.dp),
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = sidePad),
            modifier = Modifier.fillMaxWidth().height(itemHeight * visibleCount),
        ) {
            itemsIndexed(items) { index, text ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            val info = state.layoutInfo
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            val dist = if (item == null) 3f else abs(item.offset + item.size / 2f - center) / item.size
                            val t = (1f - dist / 2.5f).coerceIn(0f, 1f)
                            val s = 0.78f + 0.22f * t
                            scaleX = s
                            scaleY = s
                            alpha = 0.25f + 0.75f * t
                            rotationX = (if (item != null && item.offset + item.size / 2f < center) 1f else -1f) * dist * 14f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    NoodleText(
                        text,
                        style = NoodleTheme.typography.title3.copy(fontWeight = if (index == centered) FontWeight.Medium else FontWeight.Normal),
                        color = if (index == centered) colors.label else colors.labelTertiary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
