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
package androidx.test.core.view

import android.graphics.Bitmap
import android.view.View
import androidx.test.annotation.ExperimentalScreenshot
import com.google.common.util.concurrent.ListenableFuture

/**
 * Captures an image of the underlying view into a {@link Bitmap}.
 *
 * Essentially provides a kotlin friendly extension function for {@link
 * ViewCapture#captureViewToImage}
 *
 * TODO: make this return something coroutine friendly This API is currently experimental and
 * subject to change or removal.
 */
@ExperimentalScreenshot
fun View.captureToImage(): ListenableFuture<Bitmap> {
  return ViewCapture.captureViewToImage(this)
}
