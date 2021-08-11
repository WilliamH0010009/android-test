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
package androidx.test.core.view;

import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import androidx.annotation.RequiresApi;
import androidx.test.annotation.ExperimentalScreenshot;
import androidx.test.core.internal.view.ViewCaptureImplKt;
import androidx.test.platform.graphics.HardwareRendererCompat;
import com.google.common.util.concurrent.ListenableFuture;

/** Utilities for capturing a view to a image. */
public class ViewCapture {

  private ViewCapture() {}

  /**
   * Asynchronously captures an image of the underlying view into a {@link Bitmap}.
   *
   * For devices below {@link Build.VERSION_CODES#O} (or if the view's window cannot be determined),
   * the image is obtained using {@link View#draw). Otherwise, {@link PixelCopy } is used.
   *
   * This method will also enable {@link HardwareRendererCompat#setDrawingEnabled} if required.
   *
   * This API is primarily intended for use in lower layer libraries or frameworks.
   * For test authors, its recommended to use espresso or compose's captureToImage.
   *
   * This API is currently experimental and subject to change or removal.
   */
  @ExperimentalScreenshot
  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
  public static ListenableFuture<Bitmap> captureViewToImage(View view) {
    return ViewCaptureImplKt.captureViewToImage(view);
  }
}
