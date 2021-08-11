/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.test.core.internal.view

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi

/**
 * Trigger a redraw of the given view.
 *
 * Should only be called on main thread.
 *
 * @param view the view to trigger a redraw of
 * @param onCompleteCallback the runnable to execute once the draw is complete. Will be called on
 * main thread
 */
// NoClassDefFoundError occurs on API 15
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
fun forceRedraw(view: View, onCompleteCallback: Runnable) {
  if (Build.VERSION.SDK_INT >= 29 && view.isHardwareAccelerated) {
    view.viewTreeObserver.registerFrameCommitCallback() {
      // frame commit callbacks occur on main thread, so no need to post
      onCompleteCallback.run()
    }
  } else {
    view.viewTreeObserver.addOnDrawListener(
      object : ViewTreeObserver.OnDrawListener {
        var handled = false
        override fun onDraw() {
          if (!handled) {
            handled = true
            Handler(Looper.getMainLooper()).post {
              onCompleteCallback.run()
              view.viewTreeObserver.removeOnDrawListener(this)
            }
          }
        }
      }
    )
  }
  view.invalidate()
}
