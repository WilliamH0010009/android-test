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

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.ResolvableFuture
import androidx.test.core.internal.os.HandlerExecutor
import androidx.test.platform.graphics.HardwareRendererCompat
import com.google.common.util.concurrent.ListenableFuture

/**
 * Internal implementation for capturing an image from a Window.
 *
 * TOOD: redo implementation and signature to use co-routines
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun captureWindowRegionToImage(
  window: Window,
  boundsInWindow: Rect? = null
): ListenableFuture<Bitmap> {
  val bitmapFuture: ResolvableFuture<Bitmap> = ResolvableFuture.create()
  val drawingWasEnabled = HardwareRendererCompat.enableDrawingIfNecessary()
  val mainExecutor = HandlerExecutor(Handler(Looper.getMainLooper()))

  // enable drawing again if necessary once work is complete
  if (!drawingWasEnabled) {
    bitmapFuture.addListener({ HardwareRendererCompat.setDrawingEnabled(false) }, mainExecutor)
  }

  mainExecutor.execute {
    forceRedraw(window.decorView, Runnable { window.generateBitmap(boundsInWindow, bitmapFuture) })
  }

  return bitmapFuture
}

internal fun Window.generateBitmap(
  boundsInWindow: Rect? = null,
  bitmapFuture: ResolvableFuture<Bitmap>
) {
  val destBitmap =
    Bitmap.createBitmap(
      boundsInWindow?.width() ?: decorView.width,
      boundsInWindow?.height() ?: decorView.height,
      Bitmap.Config.ARGB_8888
    )
  if (Build.VERSION.SDK_INT < 26) {
    // TODO: handle boundsInWindow
    decorView.generateBitmapFromDraw(destBitmap, bitmapFuture)
  } else {
    generateBitmapFromPixelCopy(boundsInWindow, destBitmap, bitmapFuture)
  }
}

@SuppressWarnings("NewApi")
internal fun Window.generateBitmapFromPixelCopy(
  boundsInWindow: Rect? = null,
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
  PixelCopy.request(
    this,
    boundsInWindow,
    destBitmap,
    onCopyFinished,
    Handler(Looper.getMainLooper())
  )
}
