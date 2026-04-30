package com.airsaid.toolkit.demo.toolkit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberAppLogoBytes(): ByteArray? {
  val context = LocalContext.current
  return remember {
    val iconRes = context.applicationInfo.icon
    if (iconRes == 0) return@remember null
    val drawable = context.getDrawable(iconRes) ?: return@remember null
    drawable.toPngBytes()
  }
}

private fun Drawable.toPngBytes(): ByteArray? {
  val bitmap = when (this) {
    is BitmapDrawable -> this.bitmap
    else -> createBitmapFromDrawable(this)
  } ?: return null
  val output = ByteArrayOutputStream()
  val success = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
  return if (success) output.toByteArray() else null
}

private fun createBitmapFromDrawable(drawable: Drawable): Bitmap? {
  val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: return null
  val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: return null
  val bitmap = createBitmap(width, height)
  val canvas = Canvas(bitmap)
  drawable.setBounds(0, 0, canvas.width, canvas.height)
  drawable.draw(canvas)
  return bitmap
}
