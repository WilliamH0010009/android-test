/*
 * Copyright (C) 2014 The Android Open Source Project
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
package androidx.test.espresso;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;
import androidx.annotation.RequiresApi;
import androidx.test.annotation.ExperimentalScreenshot;
import androidx.test.core.internal.view.CaptureUtilKt;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.internal.util.Checks;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.platform.graphics.HardwareRendererCompat;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.hamcrest.Matcher;

/**
 * API surface for performing device-centric operations.
 *
 * <p>This API is experimental and subject to change
 */
public class DeviceInteraction {

  DeviceInteraction() {}

  /**
   * Captures an image of the device's screen into a {@link Bitmap}.
   *
   * <p>Essentially a wrapper for {@link UIAutomation#takeScreenshot} that also handles cases where
   * hardware renderer drawing is disabled. See {@link HardwareRendererCompat}.
   *
   * <p>This API is intended for use cases like debugging where an image of the entire screen is
   * needed. For use cases where the image will be used for validation, its recommended to take a
   * more isolated, tsrgeted screenshot of a specific view or compose node. See {@link
   * ViewInteractionCapture#captureToImage} and compose's captureToImage.
   *
   * <p>This API is currently experimental and subject to change or removal.
   */
  @ExperimentalScreenshot
  @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  public Bitmap takeScreenshot() {
    Checks.checkArgument(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2,
        "takeScreenshot is only supported on Android SDKs >= 18");
    boolean drawingEnabled = HardwareRendererCompat.enableDrawingIfNecessary();

    if (!drawingEnabled) {
      SettableFuture<Boolean> listeningFuture = SettableFuture.create();

      // TODO: support cases where there are multiple root vies
      Espresso.onView(ViewMatchers.isRoot())
          .perform(new RedrawViewThenScreenshotAction(listeningFuture));
      // wait for redraw
      try {
        listeningFuture.get(10, SECONDS);
      } catch (InterruptedException | TimeoutException | ExecutionException e) {
        throw new RuntimeException(e);
      }
      Bitmap b = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
      HardwareRendererCompat.setDrawingEnabled(false);
      return b;
    }

    // TODO: should Espresso.onIdle be performed here? Better for synchronization, but probably
    // worse for error handling use cases
    return InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
  }

  private static class RedrawViewThenScreenshotAction implements ViewAction {

    private final SettableFuture<Boolean> listeningFuture;

    RedrawViewThenScreenshotAction(SettableFuture<Boolean> listeningFuture) {
      this.listeningFuture = listeningFuture;
    }

    @Override
    public Matcher<View> getConstraints() {
      return ViewMatchers.isDisplayingAtLeast(90);
    }

    @Override
    public String getDescription() {
      return String.format(Locale.ROOT, "capture view to image");
    }

    @Override
    public void perform(UiController uiController, View view) {
      CaptureUtilKt.forceRedraw(view, () -> listeningFuture.set(true));
    }
  }
}
