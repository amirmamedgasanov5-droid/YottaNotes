package design.noodle.foundation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

private const val K = 1.52866483f

/**
 * «Непрерывные» скругления как у iOS (squircle): кривизна плавно нарастает,
 * поэтому углы выглядят мягче обычного RoundedCornerShape.
 */
@Immutable
class SquircleShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
) : Shape {
    constructor(radius: Dp) : this(radius, radius, radius, radius)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val limit = min(w, h) / 2f / K
        fun r(dp: Dp): Float = with(density) { dp.toPx() }.coerceIn(0f, limit)

        val ltr = layoutDirection == LayoutDirection.Ltr
        val rTL = r(if (ltr) topStart else topEnd)
        val rTR = r(if (ltr) topEnd else topStart)
        val rBR = r(if (ltr) bottomEnd else bottomStart)
        val rBL = r(if (ltr) bottomStart else bottomEnd)

        val p = Path()
        p.moveTo(w / 2f, 0f)
        corner(p, w, 0f, -1f, 1f, true, rTR)
        corner(p, w, h, -1f, -1f, false, rBR)
        corner(p, 0f, h, 1f, -1f, true, rBL)
        corner(p, 0f, 0f, 1f, 1f, false, rTL)
        p.close()
        return Outline.Generic(p)
    }

    private fun corner(p: Path, cx: Float, cy: Float, sx: Float, sy: Float, horizontalFirst: Boolean, r: Float) {
        if (r <= 0f) {
            p.lineTo(cx, cy)
            return
        }
        fun pt(a: Float, b: Float): Offset =
            if (horizontalFirst) Offset(cx + sx * a * r, cy + sy * b * r)
            else Offset(cx + sx * b * r, cy + sy * a * r)

        fun cubic(c1: Offset, c2: Offset, e: Offset) = p.cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)

        val s = pt(K, 0f)
        p.lineTo(s.x, s.y)
        cubic(pt(1.08849296f, 0f), pt(0.86840694f, 0f), pt(0.63149379f, 0.07491139f))
        cubic(pt(0.37282383f, 0.16905956f), pt(0.16905956f, 0.37282383f), pt(0.07491139f, 0.63149379f))
        cubic(pt(0f, 0.86840694f), pt(0f, 1.08849296f), pt(0f, K))
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
    val Sheet = SquircleShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
}
