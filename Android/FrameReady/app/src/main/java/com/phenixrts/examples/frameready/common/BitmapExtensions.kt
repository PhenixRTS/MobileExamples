/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.examples.frameready.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import kotlin.math.roundToInt

private const val SCALE_FACTOR = 4

fun Bitmap.mixWith(overlay: Bitmap): Bitmap {
    val finalBitmap = Bitmap.createBitmap(width, height, this.config)
    val canvas = Canvas(finalBitmap)
    val aspectRatio: Float = overlay.width.toFloat() / overlay.height.toFloat()
    val targetWidth = width / SCALE_FACTOR
    val targetHeight = (targetWidth / aspectRatio).roundToInt()
    val scaledOverlay = Bitmap.createScaledBitmap(overlay, targetWidth, targetHeight, false)
    canvas.drawBitmap(this, Matrix(), null)
    canvas.drawBitmap(scaledOverlay, width - targetWidth.toFloat(), height - targetHeight.toFloat(), null)
    overlay.recycle()
    scaledOverlay.recycle()
    recycle()
    return finalBitmap
}
