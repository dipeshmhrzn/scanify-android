package com.scanify.app.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.scanify.app.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    val partialPath = remember { Path() }

    Box(
        modifier = modifier
            .size(120.dp)
            .rotate(-90f)
            .drawWithCache {
                val radius = size.minDimension / 2 - 20.dp.toPx()
                val amplitude = 5.dp.toPx()
                val peaks = 8

                val fullWavePath = Path()
                for (i in 0..360) {
                    val angle = Math.toRadians(i.toDouble())
                    val r = radius + amplitude * sin(peaks * angle)

                    val x = size.width / 2 + (r * cos(angle)).toFloat()
                    val y = size.height / 2 + (r * sin(angle)).toFloat()

                    if (i == 0) fullWavePath.moveTo(x, y) else fullWavePath.lineTo(x, y)
                }
                fullWavePath.close()

                val pathMeasure = PathMeasure().apply {
                    setPath(fullWavePath, forceClosed = true)
                }

                onDrawWithContent {
                    val strokeWidth = 3.5.dp.toPx()

                    drawPath(
                        path = fullWavePath,
                        color = Color(0xFFFF3B30).copy(alpha = 0.15f),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    partialPath.reset()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathMeasure.length * progress,
                        destination = partialPath,
                        startWithMoveTo = true
                    )

                    drawPath(
                        path = partialPath,
                        color = Color(0xFFFF3B30),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    drawContent()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(shape = CircleShape)
                .rotate(90f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Loading Logo"
            )
        }
    }
}