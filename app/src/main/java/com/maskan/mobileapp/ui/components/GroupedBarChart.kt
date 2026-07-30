package com.maskan.mobileapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maskan.mobileapp.ui.theme.MaskanTheme
import com.maskan.mobileapp.ui.theme.MaskanType
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow

/** Grouped bar chart: two bars per category, y-axis gridlines + value labels. */
@Composable
fun GroupedBarChart(
    labels: List<String>,
    seriesA: List<Double>,
    seriesB: List<Double>,
    colorA: Color,
    colorB: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaskanTheme.colors
    val niceMax = niceMax(max(seriesA.maxOrNull() ?: 0.0, seriesB.maxOrNull() ?: 0.0))
    val gridSteps = 3 // -> 4 labels: 0, 1/3, 2/3, max

    Row(modifier = modifier.fillMaxWidth().height(220.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            for (i in gridSteps downTo 0) {
                val value = niceMax * i / gridSteps
                Text(text = formatCompact(value), style = MaskanType.caption, color = colors.textSecondary)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val stepY = size.height / gridSteps
                for (i in 0..gridSteps) {
                    val y = stepY * i
                    drawLine(
                        color = colors.border,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                if (labels.isEmpty()) return@Canvas
                val groupWidth = size.width / labels.size
                val barWidth = groupWidth * 0.24f
                val gap = groupWidth * 0.06f

                labels.indices.forEach { index ->
                    val groupStart = groupWidth * index
                    val centerX = groupStart + groupWidth / 2f

                    val aHeightRatio = if (niceMax > 0) (seriesA.getOrElse(index) { 0.0 } / niceMax).toFloat() else 0f
                    val bHeightRatio = if (niceMax > 0) (seriesB.getOrElse(index) { 0.0 } / niceMax).toFloat() else 0f

                    val aHeight = size.height * aHeightRatio
                    val bHeight = size.height * bHeightRatio

                    drawRoundRect(
                        color = colorA,
                        topLeft = Offset(centerX - gap - barWidth, size.height - aHeight),
                        size = Size(barWidth, aHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                    drawRoundRect(
                        color = colorB,
                        topLeft = Offset(centerX + gap, size.height - bHeight),
                        size = Size(barWidth, bHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaskanType.caption,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegend(entries: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    val colors = MaskanTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.width(8.dp).height(8.dp).background(color, CircleShape))
                Text(text = label, style = MaskanType.secondary, color = colors.textSecondary)
            }
        }
    }
}

private fun niceMax(value: Double): Double {
    if (value <= 0.0) return 1.0
    val magnitude = 10.0.pow(kotlin.math.floor(kotlin.math.log10(value)))
    val normalized = value / magnitude
    val niceNormalized = when {
        normalized <= 1 -> 1.0
        normalized <= 2 -> 2.0
        normalized <= 5 -> 5.0
        else -> 10.0
    }
    return ceil(niceNormalized * magnitude)
}

private fun formatCompact(value: Double): String {
    if (value <= 0.0) return "0"
    return java.text.NumberFormat.getNumberInstance().format(value.toLong())
}
