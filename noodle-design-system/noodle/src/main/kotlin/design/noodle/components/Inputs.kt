package design.noodle.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import design.noodle.foundation.SquircleShape
import design.noodle.foundation.ink
import design.noodle.foundation.noRippleClickable
import design.noodle.foundation.noodleClickable
import design.noodle.theme.NoodleMotion
import design.noodle.theme.NoodleTheme

/** Поле ввода: чернильный контур; при фокусе тень «выезжает» цветом акцента. */
@Composable
fun NoodleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: ImageVector? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val edge by animateColorAsState(
        when { isError -> colors.danger; focused -> colors.primary; else -> Color.Transparent },
        tween(180), label = "fieldEdge",
    )

    Column(modifier) {
        if (label != null) {
            NoodleText(label, Modifier.padding(start = 2.dp, bottom = 8.dp), style = type.footnote, color = colors.labelSecondary)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .ink(SquircleShape(14.dp), colors.fillTertiary, border = 1.dp, outline = edge, strokeWidth = 1.2.dp)
                .heightIn(min = 50.dp)
                .noRippleClickable(enabled) { focusRequester.requestFocus() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                NoodleIcon(leadingIcon, Modifier.padding(end = 10.dp), tint = colors.label, iconSize = 20.dp)
            }
            Box(Modifier.weight(1f).padding(vertical = 13.dp)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = type.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = keyboardOptions,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) NoodleText(placeholder, color = colors.labelTertiary)
                            inner()
                        }
                    },
                )
            }
            AnimatedVisibility(
                visible = focused && value.isNotEmpty(),
                enter = scaleIn(NoodleMotion.bouncy(), initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(tween(120), targetScale = 0.4f) + fadeOut(tween(120)),
            ) {
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                        .ink(CircleShape, colors.labelQuaternary, border = 0.dp)
                        .noRippleClickable { onValueChange("") },
                    contentAlignment = Alignment.Center,
                ) { NoodleIcon(Icons.Outlined.Close, tint = colors.background, iconSize = 13.dp) }
            }
        }
        AnimatedVisibility(
            visible = supportingText != null,
            enter = fadeIn() + expandVertically(NoodleMotion.snappy()),
            exit = fadeOut() + shrinkVertically(NoodleMotion.snappy()),
        ) {
            NoodleText(
                supportingText ?: "",
                Modifier.padding(start = 2.dp, top = 4.dp),
                style = type.caption1,
                color = if (isError) colors.danger else colors.labelSecondary,
            )
        }
    }
}

/** Поиск: при фокусе справа выезжает «Отмена». */
@Composable
fun NoodleSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Поиск",
) {
    val colors = NoodleTheme.colors
    val type = NoodleTheme.typography
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(42.dp)
                .ink(androidx.compose.foundation.shape.RoundedCornerShape(50), colors.fillTertiary, border = 1.dp, outline = if (focused) colors.primary else Color.Transparent, strokeWidth = 1.2.dp)
                .noRippleClickable { focusRequester.requestFocus() }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoodleIcon(Icons.Outlined.Search, Modifier.padding(end = 8.dp), tint = colors.labelSecondary, iconSize = 19.dp)
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
                    singleLine = true,
                    textStyle = type.body.copy(color = colors.label),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) NoodleText(placeholder, color = colors.labelTertiary)
                            inner()
                        }
                    },
                )
            }
            AnimatedVisibility(visible = value.isNotEmpty(), enter = scaleIn(NoodleMotion.bouncy()) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(
                    Modifier.size(18.dp).ink(CircleShape, colors.labelQuaternary, border = 0.dp).noRippleClickable { onValueChange("") },
                    contentAlignment = Alignment.Center,
                ) { NoodleIcon(Icons.Outlined.Close, tint = colors.background, iconSize = 11.dp) }
            }
        }
        AnimatedVisibility(
            visible = focused,
            enter = expandHorizontally(NoodleMotion.snappy()) + fadeIn(),
            exit = shrinkHorizontally(NoodleMotion.snappy()) + fadeOut(),
        ) {
            NoodleText(
                "Отмена",
                Modifier.noodleClickable(haptic = false, pressedScale = 0.94f) {
                    onValueChange("")
                    focusManager.clearFocus()
                }.padding(start = 12.dp),
                style = type.body,
                color = colors.label,
                maxLines = 1,
            )
        }
    }
}
