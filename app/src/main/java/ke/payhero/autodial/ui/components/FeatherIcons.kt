package ke.payhero.autodial.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FeatherIcons {

    val Home: ImageVector
        get() = ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 9f)
                lineTo(12f, 2f)
                lineTo(21f, 9f)
                verticalLineTo(20f)
                curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
                horizontalLineTo(15f)
                verticalLineTo(14f)
                horizontalLineTo(9f)
                verticalLineTo(21f)
                horizontalLineTo(4f)
                curveTo(3.45f, 21f, 3f, 20.55f, 3f, 20f)
                close()
            }
        }.build()

    val Globe: ImageVector
        get() = ImageVector.Builder(
            name = "Globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                close()
                moveTo(2f, 12f)
                horizontalLineTo(22f)
                moveTo(12f, 2f)
                curveTo(14.5f, 4.8f, 16f, 8.3f, 16f, 12f)
                curveTo(16f, 15.7f, 14.5f, 19.2f, 12f, 22f)
                curveTo(9.5f, 19.2f, 8f, 15.7f, 8f, 12f)
                curveTo(8f, 8.3f, 9.5f, 4.8f, 12f, 2f)
                close()
            }
        }.build()

    val MessageSquare: ImageVector
        get() = ImageVector.Builder(
            name = "MessageSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 15f)
                curveTo(21f, 15.55f, 20.55f, 16f, 20f, 16f)
                horizontalLineTo(7f)
                lineTo(3f, 20f)
                verticalLineTo(5f)
                curveTo(3f, 4.45f, 3.45f, 4f, 4f, 4f)
                horizontalLineTo(20f)
                curveTo(20.55f, 4f, 21f, 4.45f, 21f, 5f)
                verticalLineTo(15f)
                close()
            }
        }.build()

    val Shield: ImageVector
        get() = ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(12f, 22f, 20f, 18f, 20f, 12f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(12f)
                curveTo(4f, 18f, 12f, 22f, 12f, 22f)
                close()
            }
        }.build()

    val Phone: ImageVector
        get() = ImageVector.Builder(
            name = "Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 16.92f)
                verticalLineTo(19.92f)
                curveTo(22f, 20.48f, 21.54f, 20.93f, 20.98f, 20.92f)
                curveTo(10.15f, 20.15f, 3.85f, 10.15f, 3.08f, 3.02f)
                curveTo(3.07f, 2.46f, 3.52f, 2f, 4.08f, 2f)
                horizontalLineTo(7.08f)
                curveTo(7.57f, 2f, 7.99f, 2.36f, 8.06f, 2.85f)
                curveTo(8.21f, 3.91f, 8.48f, 4.94f, 8.86f, 5.92f)
                curveTo(8.98f, 6.23f, 8.89f, 6.58f, 8.65f, 6.82f)
                lineTo(7.38f, 8.09f)
                curveTo(9.03f, 11f, 11.91f, 12.97f, 14.91f, 14.62f)
                lineTo(16.18f, 13.35f)
                curveTo(16.42f, 13.11f, 16.77f, 13.02f, 17.08f, 13.14f)
                curveTo(18.06f, 13.52f, 19.09f, 13.79f, 20.15f, 13.94f)
                curveTo(20.64f, 14.01f, 21f, 14.43f, 21f, 14.92f)
                close()
            }
        }.build()

    val Bell: ImageVector
        get() = ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
                curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                horizontalLineTo(21f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                close()
                moveTo(13.73f, 21f)
                curveTo(13.36f, 21.61f, 12.72f, 22f, 12f, 22f)
                curveTo(11.28f, 22f, 10.64f, 21.61f, 10.27f, 21f)
            }
        }.build()

    val Zap: ImageVector
        get() = ImageVector.Builder(
            name = "Zap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 2f)
                lineTo(3f, 14f)
                horizontalLineTo(12f)
                lineTo(11f, 22f)
                lineTo(21f, 10f)
                horizontalLineTo(12f)
                lineTo(13f, 2f)
                close()
            }
        }.build()

    val Plus: ImageVector
        get() = ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }.build()

    val Play: ImageVector
        get() = ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 3f)
                lineTo(19f, 12f)
                lineTo(5f, 21f)
                verticalLineTo(3f)
                close()
            }
        }.build()

    val Edit: ImageVector
        get() = ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 4f)
                horizontalLineTo(4f)
                curveTo(3.45f, 4f, 3f, 4.45f, 3f, 5f)
                verticalLineTo(20f)
                curveTo(3f, 20.55f, 3.45f, 21f, 4f, 21f)
                horizontalLineTo(19f)
                curveTo(19.55f, 21f, 20f, 20.55f, 20f, 20f)
                verticalLineTo(13f)
                moveTo(18.5f, 2.5f)
                curveTo(19.33f, 1.67f, 20.67f, 1.67f, 21.5f, 2.5f)
                curveTo(22.33f, 3.33f, 22.33f, 4.67f, 21.5f, 5.5f)
                lineTo(12f, 15f)
                lineTo(8f, 16f)
                lineTo(9f, 12f)
                lineTo(18.5f, 2.5f)
                close()
            }
        }.build()

    val Trash: ImageVector
        get() = ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                moveTo(19f, 6f)
                verticalLineTo(20f)
                curveTo(19f, 20.55f, 18.55f, 21f, 18f, 21f)
                horizontalLineTo(6f)
                curveTo(5.45f, 21f, 5f, 20.55f, 5f, 20f)
                verticalLineTo(6f)
                moveTo(8f, 6f)
                verticalLineTo(4f)
                curveTo(8f, 3.45f, 8.45f, 3f, 9f, 3f)
                horizontalLineTo(15f)
                curveTo(15.55f, 3f, 16f, 3.45f, 16f, 4f)
                verticalLineTo(6f)
            }
        }.build()

    val Copy: ImageVector
        get() = ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                close()
                moveTo(5f, 15f)
                horizontalLineTo(4f)
                curveTo(3.45f, 15f, 3f, 14.55f, 3f, 14f)
                verticalLineTo(4f)
                curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
                horizontalLineTo(14f)
                curveTo(14.55f, 3f, 15f, 3.45f, 15f, 4f)
                verticalLineTo(5f)
            }
        }.build()

    val Refresh: ImageVector
        get() = ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(17f)
                moveTo(1f, 20f)
                verticalLineTo(14f)
                horizontalLineTo(7f)
                moveTo(3.51f, 9f)
                curveTo(4.7f, 6.02f, 7.6f, 3.88f, 11f, 3.36f)
                curveTo(15.42f, 2.68f, 19.64f, 5.34f, 21f, 9.5f)
                moveTo(20.49f, 15f)
                curveTo(19.3f, 17.98f, 16.4f, 20.12f, 13f, 20.64f)
                curveTo(8.58f, 21.32f, 4.36f, 18.66f, 3f, 14.5f)
            }
        }.build()

    val ChevronRight: ImageVector
        get() = ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 6f)
            }
        }.build()
}
