/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.test.core.internal.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.ResolvableFuture
import androidx.test.core.internal.os.HandlerExecutor
import androidx.test.platform.graphics.HardwareRendererCompat
import com.google.common.util.concurrent.ListenableFuture

/**
 * Internal implementation for capturing an image from a View.
 *
 * TOOD: redo implementation and signature to use co-routines
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun captureViewToImage(view: View): ListenableFuture<Bitmap> {
  val bitmapFuture: ResolvableFuture<Bitmap> = ResolvableFuture.create()
  val drawingWasEnabled = HardwareRendererCompat.enableDrawingIfNecessary()
  val mainExecutor = HandlerExecutor(Handler(Looper.getMainLooper()))

  // enable drawing again if necessary once work is complete
  if (!drawingWasEnabled) {
    bitmapFuture.addListener({ HardwareRendererCompat.setDrawingEnabled(false) }, mainExecutor)
  }

  mainExecutor.execute { forceRedraw(view, Runnable { view.generateBitmap(bitmapFuture) }) }

  return bitmapFuture
}

private fun View.generateBitmap(bitmapFuture: ResolvableFuture<Bitmap>) {
  if (bitmapFuture.isCancelled) {
    return
  }
  val destBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  if (Build.VERSION.SDK_INT < 26) {
    generateBitmapFromDraw(destBitmap, bitmapFuture)
  } else {
    if (this is SurfaceView) {
      generateBitmapFromSurfaceViewPixelCopy(destBitmap, bitmapFuture)
    } else {
      val window = getActivity()!!.window
      if (window != null) {
        generateBitmapFromPixelCopy(window, destBitmap, bitmapFuture)
      } else {
        Log.i(
          "View.captureToImage",
          "Could not find window for view. Falling back to View#draw instead of PixelCopy"
        )
        generateBitmapFromDraw(destBitmap, bitmapFuture)
      }
    }
  }
}

@SuppressWarnings("NewApi")
fun SurfaceView.generateBitmapFromSurfaceViewPixelCopy(
  destBitmap: Bitmap,
  bitmapFuture: ResolvableFuture<Bitmap>
) {
  val onCopyFinished =
    PixelCopy.OnPixelCopyFinishedListener { result ->
      if (result == PixelCopy.SUCCESS) {
        bitmapFuture.set(destBitmap)
      } else {
        bitmapFuture.setException(RuntimeException(String.format("PixelCopy failed: %d", result)))
      }
    }
  PixelCopy.request(this, null, destBitmap, onCopyFinished, handler)
}

fun View.generateBitmapFromDraw(destBitmap: Bitmap, bitmapFuture: ResolvableFuture<Bitmap>) {
  destBitmap.density = resources.displayMetrics.densityDpi
  computeScroll()
  val canvas = Canvas(destBitmap)
  canvas.translate((-scrollX).toFloat(), (-scrollY).toFloat())
  draw(canvas)
  bitmapFuture.set(destBitmap)
}

fun View.getActivity(): Activity? {
  fun Context.getActivity(): Activity? {
    return when (this) {
      is Activity -> this
      is ContextWrapper -> this.baseContext.getActivity()
      else -> null
    }
  }
  return context.getActivity()
}

fun View.generateBitmapFromPixelCopy(
  window: Window,
  destBitmap: Bitmap,
  bitmapFuture: ResolvableFuture<Bitmap>
) {
  val locationInWindow = intArrayOf(0, 0)
  getLocationInWindow(locationInWindow)
  val x = locationInWindow[0]
  val y = locationInWindow[1]
  val boundsInWindow = Rect(x, y, x + width, y + height)

  return window.generateBitmapFromPixelCopy(boundsInWindow, destBitmap, bitmapFuture)
}
