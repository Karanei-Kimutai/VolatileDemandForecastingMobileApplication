package com.example.volatiledemandforecasting.core.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** How a series is drawn. The legend and line style differ so colour is never the only cue (spec §16). */
enum class LineStyle { Solid, Dashed, Dotted, Dots }

/**
 * One line. [values] is aligned with the chart's x positions: index 0 is the first date.
 * A null value is a gap in the data (not recorded), so the line breaks there instead of dropping to 0.
 */
data class ChartSeries(
    val label: String,
    val values: List<Double?>,
    val color: Color,
    val style: LineStyle = LineStyle.Solid,
    /** Shade the area under the line: used for the one series a chart is mainly about. */
    val fill: Boolean = false,
)

/**
 * A simple multi-series line chart drawn on a Canvas. It only draws numbers it is given.
 *
 * @param xLabels labels for evenly spread positions along the axis, as `index to text`.
 * @param markerIndex x position of a vertical marker (the forecast start), or null.
 * @param description spoken summary for screen readers.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    pointCount: Int,
    xLabels: List<Pair<Int, String>>,
    description: String,
    modifier: Modifier = Modifier,
    markerIndex: Int? = null,
    markerLabel: String? = null,
    height: androidx.compose.ui.unit.Dp = 200.dp,
    yFormat: (Double) -> String = ::defaultYLabel,
    showLegend: Boolean = series.size > 1,
) {
    val measurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisText = MaterialTheme.colorScheme.onSurfaceVariant
    val markerColor = MaterialTheme.colorScheme.outline
    // The region after the marker is the future: a faint wash marks it as forecast, not history.
    val futureWash = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, color = axisText)
    val density = LocalDensity.current

    // The legend comes first, so the reader knows what the lines are before reading them.
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (showLegend) Legend(series)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = description },
        ) {
            if (pointCount < 1) return@Canvas
            val maxValue = series.flatMap { it.values }.filterNotNull().maxOrNull() ?: 0.0
            val top = niceCeiling(maxValue)
            val ticks = gridSteps(top)
            val tickLabels = (0..ticks).map { yFormat(top * it / ticks) }
            val leftPad = tickLabels.maxOf { measurer.measure(it, labelStyle).size.width } + with(density) { 8.dp.toPx() }
            val bottomPad = with(density) { 22.dp.toPx() }
            val topPad = with(density) { 6.dp.toPx() }
            val rightPad = with(density) { 6.dp.toPx() }
            val plotW = size.width - leftPad - rightPad
            val plotH = size.height - bottomPad - topPad

            fun x(i: Int) = leftPad + if (pointCount == 1) plotW / 2 else plotW * i / (pointCount - 1)
            fun y(v: Double) = topPad + plotH - (plotH * (v / top)).toFloat()

            if (markerIndex != null && markerIndex in 0 until pointCount) {
                val mx = x(markerIndex)
                drawRect(futureWash, topLeft = Offset(mx, topPad), size = androidx.compose.ui.geometry.Size(leftPad + plotW - mx, plotH))
            }
            for (t in 0..ticks) {
                val yy = topPad + plotH - plotH * t / ticks
                drawLine(
                    gridColor, Offset(leftPad, yy), Offset(leftPad + plotW, yy), strokeWidth = 1f,
                    pathEffect = if (t == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
                val layout = measurer.measure(tickLabels[t], labelStyle)
                drawText(layout, topLeft = Offset(leftPad - layout.size.width - with(density) { 6.dp.toPx() }, yy - layout.size.height / 2f))
            }
            xLabels.forEach { (index, text) ->
                if (index !in 0 until pointCount) return@forEach
                val layout = measurer.measure(text, labelStyle)
                val xx = (x(index) - layout.size.width / 2f).coerceIn(leftPad - 4f, size.width - layout.size.width.toFloat())
                drawText(layout, topLeft = Offset(xx, topPad + plotH + with(density) { 4.dp.toPx() }))
            }
            if (markerIndex != null && markerIndex in 0 until pointCount) {
                val mx = x(markerIndex)
                drawLine(
                    markerColor, Offset(mx, topPad), Offset(mx, topPad + plotH), strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
                if (markerLabel != null) drawMarkerLabel(measurer, markerLabel, mx, topPad, leftPad + plotW, labelStyle)
            }
            val baseline = topPad + plotH
            series.forEach { drawSeries(it, ::x, ::y, baseline, with(density) { 2.dp.toPx() }) }
        }
    }
}

private fun DrawScope.drawMarkerLabel(
    measurer: TextMeasurer,
    text: String,
    markerX: Float,
    topPad: Float,
    rightEdge: Float,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style)
    val fitsRight = markerX + 4f + layout.size.width <= rightEdge
    val xx = if (fitsRight) markerX + 4f else markerX - 4f - layout.size.width
    drawText(layout, topLeft = Offset(xx, topPad))
}

private fun DrawScope.drawSeries(series: ChartSeries, x: (Int) -> Float, y: (Double) -> Float, baseline: Float, strokeWidth: Float) {
    if (series.style == LineStyle.Dots) {
        series.values.forEachIndexed { i, v -> if (v != null) drawCircle(series.color, radius = strokeWidth * 2f, center = Offset(x(i), y(v))) }
        return
    }
    val effect = when (series.style) {
        LineStyle.Dashed -> PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
        LineStyle.Dotted -> PathEffect.dashPathEffect(floatArrayOf(2f, 10f))
        else -> null
    }
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, pathEffect = effect)
    // Break the line at every null so a gap is drawn as a gap.
    var run = mutableListOf<Int>()
    fun flush() {
        if (run.size >= 2) {
            val path = Path()
            run.forEachIndexed { n, i ->
                val v = series.values[i]!!
                if (n == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v))
            }
            if (series.fill) {
                val area = Path().apply {
                    addPath(path)
                    lineTo(x(run.last()), baseline)
                    lineTo(x(run.first()), baseline)
                    close()
                }
                drawPath(
                    area,
                    Brush.verticalGradient(listOf(series.color.copy(alpha = 0.22f), series.color.copy(alpha = 0.02f)), endY = baseline),
                )
            }
            drawPath(path, series.color, style = stroke)
        } else if (run.size == 1) {
            drawCircle(series.color, radius = 4f, center = Offset(x(run[0]), y(series.values[run[0]]!!)))
        }
        run = mutableListOf()
    }
    series.values.forEachIndexed { i, v -> if (v == null) flush() else run.add(i) }
    flush()
}

@Composable
private fun Legend(series: List<ChartSeries>) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        series.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                LegendSwatch(s)
                Text(s.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun LegendSwatch(series: ChartSeries) {
    Canvas(Modifier.size(width = 28.dp, height = 10.dp)) {
        val cy = size.height / 2
        if (series.style == LineStyle.Dots) {
            drawCircle(series.color, radius = 5f, center = Offset(size.width / 2, cy))
        } else {
            val effect = when (series.style) {
                LineStyle.Dashed -> PathEffect.dashPathEffect(floatArrayOf(14f, 9f))
                LineStyle.Dotted -> PathEffect.dashPathEffect(floatArrayOf(2f, 8f))
                else -> null
            }
            drawLine(series.color, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 4f, cap = StrokeCap.Round, pathEffect = effect)
        }
    }
}

/** Rounds the top of the axis up to 1, 2, 2.5, 5 or 10 times a power of ten so the grid labels stay tidy. */
internal fun niceCeiling(max: Double): Double {
    if (max <= 0.0) return 1.0
    val exponent = floor(log10(max))
    val base = 10.0.pow(exponent)
    val fraction = max / base
    val nice = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it >= fraction - 1e-9 }
    return nice * base
}

/** Four grid steps when that gives whole, tidy values (0, 25, 50…), otherwise five (0, 10, 20…). */
internal fun gridSteps(top: Double): Int {
    val step = top / 4
    return if (kotlin.math.abs(step - kotlin.math.round(step)) < 1e-9 && step >= 1) 4 else 5
}

internal fun defaultYLabel(value: Double): String =
    if (value == floor(value) || value >= 10) ceil(value - 1e-9).toInt().toString() else "%.1f".format(value)
