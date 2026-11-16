package design.noodle.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import design.noodle.theme.LocalNoodleContentColor
import design.noodle.theme.NoodleTheme

@Composable
fun NoodleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = NoodleTheme.typography.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val contentColor = LocalNoodleContentColor.current
    val resolved = when {
        color != Color.Unspecified -> color
        contentColor != Color.Unspecified -> contentColor
        else -> NoodleTheme.colors.label
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = resolved, textAlign = textAlign)),
        overflow = overflow,
        maxLines = maxLines,
    )
}

@Composable
fun NoodleIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = NoodleTheme.colors.label,
    iconSize: Dp = 24.dp,
) {
    Image(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier.size(iconSize),
        colorFilter = ColorFilter.tint(tint),
    )
}
