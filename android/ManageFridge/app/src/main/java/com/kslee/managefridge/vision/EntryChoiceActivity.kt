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

package com.kslee.managefridge.vision

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kslee.managefridge.R
import com.kslee.managefridge.vision.kotlin.CameraXLivePreviewActivity
import java.util.ArrayList

class EntryChoiceActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
  var dataMap = HashMap<String, MyData>()
  var array = dataMap.keys.toTypedArray()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_vision_entry_choice)
    getDataFromIntent()
    findViewById<TextView>(R.id.kotlin_entry_point).setOnClickListener {
      val intent =
        Intent(
          this@EntryChoiceActivity,
          com.kslee.managefridge.vision.kotlin.CameraXLivePreviewActivity::class.java
        )
      intent.putExtra(CameraXLivePreviewActivity.STATE_SELECTED_MODEL, CameraXLivePreviewActivity.IMAGE_LABELING)
//      startActivity(intent)
      resultLauncher.launch(intent)
    }

    if (!allRuntimePermissionsGranted()) {
      getRuntimePermissions()
    }

    importCompose()
  }

  private lateinit var resultLauncher: ActivityResultLauncher<Intent>

  private fun getDataFromIntent(){
    resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
      if (result.resultCode == Activity.RESULT_OK){
        dataMap = result.data?.getSerializableExtra(DATAMAP) as HashMap<String, MyData>
        importCompose()
      }
    }
  }

  private fun importCompose() {
    Log.d(TAG, "importCompose")
    val composeView = findViewById<ComposeView>(R.id.compose_view)
    dataMap.put("1", MyData())
    dataMap.put("2",MyData())
    dataMap.put("3",MyData())
    dataMap.put("4",MyData())
    array = dataMap.keys.toTypedArray()
    composeView.setContent {
      LazyColumn(Modifier.fillMaxSize()) {
        items(array.size) {
          ListItem(it)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()

  }

  @Composable
  fun ListItem(index: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
      dataMap.get(array.get(index))?.let { Text(text = it.name) }
    }
  }

  private fun allRuntimePermissionsGranted(): Boolean {
    for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
      permission?.let {
        if (!isPermissionGranted(this, it)) {
          return false
        }
      }
    }
    return true
  }

  private fun getRuntimePermissions() {
    val permissionsToRequest = ArrayList<String>()
    for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
      permission?.let {
        if (!isPermissionGranted(this, it)) {
          permissionsToRequest.add(permission)
        }
      }
    }

    if (permissionsToRequest.isNotEmpty()) {
      ActivityCompat.requestPermissions(
        this,
        permissionsToRequest.toTypedArray(),
        PERMISSION_REQUESTS
      )
    }
  }

  private fun isPermissionGranted(context: Context, permission: String): Boolean {
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    ) {
      Log.i(TAG, "Permission granted: $permission")
      return true
    }
    Log.i(TAG, "Permission NOT granted: $permission")
    return false
  }

  companion object {
    private const val TAG = "EntryChoiceActivity"
    val DATAMAP = "DATAMAP"
    private const val PERMISSION_REQUESTS = 1
    private val REQUIRED_RUNTIME_PERMISSIONS =
      arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
  }
}
