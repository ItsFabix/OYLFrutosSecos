package com.example.oylfrutossecos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Composición propia de hojas y un cuenco; no utiliza fotografías ni recursos externos. */
@Composable
fun OylArtwork(modifier: Modifier = Modifier, bowl: Boolean = false, onDark: Boolean = false,
    bowlClearance: Dp = 0.dp) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(if (onDark) Color(0xFF285943) else Color(0xFFFFF8EE))
        fun leaf(x: Float, y: Float, angle: Float, color: Color) {
            val center = Offset(w * x, h * y)
            withTransform({ rotate(angle, center) }) {
                drawPath(Path().apply {
                    moveTo(center.x - w * .09f, center.y + h * .035f)
                    cubicTo(center.x - w * .13f, center.y - h * .09f,
                        center.x + w * .07f, center.y - h * .1f, center.x + w * .1f, center.y - h * .04f)
                    cubicTo(center.x + w * .06f, center.y + h * .045f,
                        center.x - w * .02f, center.y + h * .08f, center.x - w * .09f, center.y + h * .035f)
                    close()
                }, color)
                drawLine(Color(0xFF83A168), center - Offset(w * .07f, -h * .025f),
                    center + Offset(w * .07f, -h * .025f), strokeWidth = 2f)
            }
        }
        leaf(.07f, .08f, -20f, Color(0xFF527544))
        leaf(.1f, .22f, 25f, Color(0xFFA6BD91))
        leaf(.94f, .09f, 145f, Color(0xFFA6BD91))
        leaf(.98f, .35f, 100f, Color(0xFF527544))
        if (bowl) {
            val bowlScale = ((h - bowlClearance.toPx()) / (h * .35f)).coerceIn(0f, 1f)
            withTransform({ scale(1f, bowlScale, Offset(0f, h)) }) {
                drawOval(Color(0xFFAE7045), Offset(w * .18f, h * .65f), Size(w * .64f, h * .25f))
                drawOval(Color(0xFFF1D5AD), Offset(w * .21f, h * .67f), Size(w * .58f, h * .18f))
                val colors = listOf(Color(0xFFD99C61), Color(0xFF84502D), Color(0xFFB7AA6C))
                for (row in 0..2) for (column in 0..4) {
                    val x = w * (.29f + column * .083f + if (row == 1) .025f else 0f)
                    val y = h * (.68f + row * .044f)
                    withTransform({ rotate((column * 21 - row * 17).toFloat(), Offset(x, y)) }) {
                        drawOval(colors[(row + column) % colors.size], Offset(x - w * .033f, y), Size(w * .07f, h * .045f))
                    }
                }
                drawPath(Path().apply {
                    moveTo(w * .19f, h * .77f)
                    cubicTo(w * .27f, h * 1.01f, w * .73f, h * 1.01f, w * .81f, h * .77f)
                    quadraticTo(w * .5f, h * .9f, w * .19f, h * .77f)
                    close()
                }, Color(0xFFCB9865))
                drawArc(Color(0xFF84502D), 0f, 180f, false,
                    Offset(w * .19f, h * .69f), Size(w * .62f, h * .17f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .012f))
            }
        }
    }
}
