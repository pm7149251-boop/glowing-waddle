package com.example.blockgallery

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun IsometricBlockCanvas(block: BlockItem, modifier: Modifier = Modifier) {
    val topBmp = remember(block.topBase64) { BlockRepository.decodeBase64ToBitmap(block.topBase64) }
    val sideBmp = remember(block.sideBase64) { BlockRepository.decodeBase64ToBitmap(block.sideBase64) }
    val faceBmp = remember(block.faceBase64) { BlockRepository.decodeBase64ToBitmap(block.faceBase64) }

    val pixelPaint = remember {
        Paint().apply {
            isFilterBitmap = false // Forces crisp nearest-neighbor pixel rendering
            isAntiAlias = false
            isDither = false
        }
    }

    val shadePaint = remember {
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = false
        }
    }

    val matrix = remember { Matrix() }
    val dstRect = remember { RectF() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Exact isometric proportion math
        val s = canvasWidth * 0.462f
        val h = s * 1.155f
        val cx = canvasWidth / 2f
        val cy = (canvasHeight + h - s) / 2f

        dstRect.set(0f, 0f, s, s)

        fun drawIsoFace(
            bitmap: Bitmap?,
            scaleX: Float, skewY: Float,
            skewX: Float, scaleY: Float,
            transX: Float, transY: Float,
            shadeAlpha: Int
        ) {
            if (bitmap == null) return
            
            // Android 3x3 Affine Matrix array order:
            // [ MSCALE_X, MSKEW_X,  MTRANS_X ]
            // [ MSKEW_Y,  MSCALE_Y, MTRANS_Y ]
            // [ MPERSP_0, MPERSP_1, MPERSP_2 ]
            matrix.setValues(
                floatArrayOf(
                    scaleX, skewX, transX,
                    skewY, scaleY, transY,
                    0f, 0f, 1f
                )
            )

            drawContext.canvas.nativeCanvas.apply {
                save()
                concat(matrix)
                drawBitmap(bitmap, null, dstRect, pixelPaint)
                if (shadeAlpha > 0) {
                    shadePaint.color = android.graphics.Color.argb(shadeAlpha, 0, 0, 0)
                    drawRect(dstRect, shadePaint)
                }
                restore()
            }
        }

        // 1. Top Face
        drawIsoFace(topBmp, 1f, 0.5f, -1f, 0.5f, cx, cy - h, shadeAlpha = 0)
        // 2. Face (Isometric Left) - 15% dark shade (38 / 255)
        drawIsoFace(faceBmp, 1f, 0.5f, 0f, h / s, cx - s, cy - h + 0.5f * s, shadeAlpha = 38)
        // 3. Side (Isometric Right) - 40% dark shade (102 / 255)
        drawIsoFace(sideBmp, 1f, -0.5f, 0f, h / s, cx, cy - h + s, shadeAlpha = 102)
    }
}