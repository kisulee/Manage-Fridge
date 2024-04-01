/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kslee.managefridge.vision.kotlin.labeldetector

import android.content.Context
import android.icu.text.SimpleDateFormat
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.kotlin.VisionProcessorBase
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabelerOptionsBase
import com.google.mlkit.vision.label.ImageLabeling
import com.kslee.managefridge.vision.EntryChoiceActivity
import com.kslee.managefridge.vision.GraphicOverlay
import com.kslee.managefridge.vision.MyData
import java.io.IOException
import java.util.ArrayList
import java.util.Date

/** Custom InputImage Classifier   */
class LabelDetectorProcessor(context: Context, options: ImageLabelerOptionsBase) :
  VisionProcessorBase<List<ImageLabel>>(context) {

  private val imageLabeler: ImageLabeler = ImageLabeling.getClient(options)

  override fun stop() {
    super.stop()
    try {
      imageLabeler.close()
    } catch (e: IOException) {
      Log.e(
        TAG,
        "Exception thrown while trying to close ImageLabelerClient: $e"
      )
    }
  }

  override fun detectInImage(image: InputImage): Task<List<ImageLabel>> {
    return imageLabeler.process(image)
  }

  override fun onSuccess(labels: List<ImageLabel>, graphicOverlay: GraphicOverlay) {
//    graphicOverlay.add(LabelGraphic(graphicOverlay, labels))
    logExtrasForTesting(labels)
  }

  override fun onFailure(e: Exception) {
    Log.w(TAG, "Label detection failed.$e")
  }

  companion object {
    private const val TAG = "LabelDetectorProcessor"

    private fun logExtrasForTesting(labels: List<ImageLabel>?) {
      if (labels == null) {
        Log.v(MANUAL_TESTING_LOG, "No labels detected")
      } else {
        for (label in labels) {
          Log.v(
            MANUAL_TESTING_LOG,
            String.format("Label %s, confidence %f", label.text, label.confidence)
          )
          val myData = MyData()
          myData.name = label.text
          myData.date = SimpleDateFormat("yyyy.MM.dd hh:mm:ss").format(Date())
          EntryChoiceActivity.data.add(myData)
        }
      }
    }
  }
}
