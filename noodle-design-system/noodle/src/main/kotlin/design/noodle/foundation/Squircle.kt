package design.noodle.foundation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Фирменные «пухлые» скругления Noodle (с независимыми радиусами углов). */
@Immutable
class SquircleShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
) : Shape {
    constructor(radius: Dp) : this(radius, radius, radius, radius)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val limit = min(size.width, size.height) / 2f
        fun r(dp: Dp): Float = with(density) { dp.toPx() }.coerceIn(0f, limit)
        val ltr = layoutDirection == LayoutDirection.Ltr
        val tl = r(if (ltr) topStart else topEnd)
        val tr = r(if (ltr) topEnd else topStart)
        val br = r(if (ltr) bottomEnd else bottomStart)
        val bl = r(if (ltr) bottomStart else bottomEnd)
        return Outline.Rounded(
            RoundRect(0f, 0f, size.width, size.height, CornerRadius(tl), CornerRadius(tr), CornerRadius(br), CornerRadius(bl)),
        )
    }

    override fun equals(other: Any?): Boolean =
        other is SquircleShape && topStart == other.topStart && topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd && bottomStart == other.bottomStart

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        return result
    }
}

object NoodleShapes {
    val Capsule = RoundedCornerShape(percent = 50)
    val Small = SquircleShape(8.dp)
    val Medium = SquircleShape(12.dp)
    val Large = SquircleShape(16.dp)
    val XLarge = SquircleShape(22.dp)
    val Sheet = SquircleShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
}
