package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val Trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color(0xFF888888)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 6f)
            lineTo(21f, 6f)
        }.path(
            stroke = SolidColor(Color(0xFF888888)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(19f, 6f)
            lineTo(19f, 20f)
            curveTo(19f, 21.1f, 18.1f, 22f, 17f, 22f)
            lineTo(7f, 22f)
            curveTo(5.9f, 22f, 5f, 21.1f, 5f, 20f)
            lineTo(5f, 6f)
            
            moveTo(8f, 6f)
            lineTo(8f, 4f)
            curveTo(8f, 2.9f, 8.9f, 2f, 10f, 2f)
            lineTo(14f, 2f)
            curveTo(15.1f, 2f, 16f, 2.9f, 16f, 4f)
            lineTo(16f, 6f)
        }.path(
            stroke = SolidColor(Color(0xFF888888)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10f, 11f)
            lineTo(10f, 17f)
        }.path(
            stroke = SolidColor(Color(0xFF888888)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(14f, 11f)
            lineTo(14f, 17f)
        }.build()
    }

    val DeleteTab: ImageVector by lazy {
        ImageVector.Builder(
            name = "DeleteTab",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color(0xFFF07041)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
            curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
            curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
            curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
        }.path(
            stroke = SolidColor(Color(0xFFF07041)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 12f)
            lineTo(16f, 12f)
        }.build()
    }

    val Backspace: ImageVector by lazy {
        ImageVector.Builder(
            name = "Backspace",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color(0xFFF07041)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(21f, 4f)
            lineTo(8f, 4f)
            lineTo(1f, 12f)
            lineTo(8f, 20f)
            lineTo(21f, 20f)
            curveTo(22.1f, 20f, 23f, 19.1f, 23f, 18f)
            lineTo(23f, 6f)
            curveTo(23f, 4.9f, 22.1f, 4f, 21f, 4f)
            close()
        }.path(
            stroke = SolidColor(Color(0xFFF07041)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 9f)
            lineTo(12f, 15f)
        }.path(
            stroke = SolidColor(Color(0xFFF07041)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 9f)
            lineTo(18f, 15f)
        }.build()
    }
}
