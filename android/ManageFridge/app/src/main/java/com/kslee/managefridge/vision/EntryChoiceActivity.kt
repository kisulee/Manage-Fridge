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
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import com.kslee.managefridge.R
import com.kslee.managefridge.vision.kotlin.CameraXLivePreviewActivity
import com.kslee.managefridge.vision.preference.PreferenceUtils.getSavedHashMap
import com.kslee.managefridge.vision.preference.PreferenceUtils.saveHashMap
import kotlinx.coroutines.launch
import java.util.Date

class EntryChoiceActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {
    var dataMap = HashMap<String, MyData>()
    val SAVED_DATA = "SAVED_DATA"

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
            intent.putExtra(
                CameraXLivePreviewActivity.STATE_SELECTED_MODEL,
                CameraXLivePreviewActivity.IMAGE_LABELING
            )
//      startActivity(intent)
            resultLauncher.launch(intent)
        }

        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }

        getSavedData()
        importCompose(dataMap)
    }

    override fun onDestroy() {
        saveData()
        super.onDestroy()
    }

    private fun getSavedData() {
        dataMap = getSavedHashMap(this, SAVED_DATA)
    }

    private fun saveData() {
        saveHashMap(this, SAVED_DATA, dataMap)
    }

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private fun getDataFromIntent() {
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    dataMap = result.data?.getSerializableExtra(DATA_MAP) as HashMap<String, MyData>
                    saveData()
                    importCompose(dataMap)
                }
            }
    }

    @OptIn(ExperimentalUnitApi::class, ExperimentalMaterial3Api::class)
    private fun importCompose(dataMap: HashMap<String, MyData>) {
        Log.d(TAG, "importCompose")
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
//            LazyColumn(Modifier.fillMaxSize(),
//                    contentPadding = PaddingValues(all = 6.dp),) {
//                items(dataMap.toList()) {
//                    ListItem(it)
//                }
//            }
            SetupList(dataMap)
        }
    }

    private fun checkExistInvalid(input: String): Boolean {
        if (dataMap.get(input) != null || input.isEmpty())
            return true
        return false
    }

    @OptIn(ExperimentalWearMaterialApi::class)
    @ExperimentalUnitApi
    @ExperimentalMaterial3Api
    @Composable
    fun SetupList(inputData: HashMap<String, MyData>) {
        val itemList = remember {
            inputData.values.toMutableStateList()
        }

        val inputValue = remember { mutableStateOf(TextFieldValue()) }
        val isError = remember { mutableStateOf(false) }
        var temp = ""

        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                TextField(
                    value = inputValue.value,
                    onValueChange = {
                        inputValue.value = it
                        isError.value = checkExistInvalid(it.text)
                    },
                    modifier = Modifier.weight(0.8f),
                    placeholder = { Text(text = resources.getString(R.string.add_item_hint_main)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrect = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),

                    textStyle = TextStyle(
                        color = Color.Black, fontSize = TextUnit.Unspecified,
                        fontFamily = FontFamily.SansSerif
                    ),
                    maxLines = 1,
                    singleLine = true,
                    isError = isError.value,
                    supportingText = {
                        if (isError.value) {
                            Icon(
                                painterResource(id = R.drawable.baseline_error_24),
                                "error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = resources.getString(R.string.aleady_error_main),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    trailingIcon = {
                        if (!isError.value) {
                            IconButton(onClick = { inputValue.value = TextFieldValue("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    "error",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                )
                Button(
                    onClick = {
                        val myData = MyData(
                            name = inputValue.value.text,
                            date = SimpleDateFormat("yyyy.MM.dd hh:mm:ss").format(Date()),
                            amount = ""
                        )
                        if (!checkExistInvalid(inputValue.value.text)) {
                            dataMap.put(myData.name, myData)
                            itemList.add(myData)
                            temp = myData.name
                        } else {
                            isError.value = true
                        }
                    },
                    modifier = Modifier
                        .weight(0.2f)
                        .wrapContentHeight()
                ) {
                    Text(text = resources.getString(R.string.add_item_main))
                }
            }

            Spacer(modifier = Modifier.height(Dp(1f)))

            LazyColumn {
                items(items = itemList,
                    key = { item: MyData -> item.name }
                ) {
                    val dismissState = rememberDismissState()

                    if (dismissState.isDismissed(DismissDirection.EndToStart)) {
                        if (temp.contentEquals(it.name)) {
                            LaunchedEffect(Unit) {
                                dismissState.reset()
                            }
                            temp = ""
                            return@items
                        }

                        if (it.name.contentEquals(inputValue.value.text))
                            isError.value = false
                        itemList.remove(it)
                        dataMap.remove(it.name)
                    }
                    SwipeToDismiss(
                        state = dismissState,
                        modifier = Modifier
                            .padding(vertical = Dp(1f)),
                        directions = setOf(
                            DismissDirection.EndToStart
                        ),
//                        positionalThreshold = { direction ->
//                            FractionalThreshold(if (direction == DismissDirection.EndToStart) 0.1f else 0.05f)
//                        },
                        background = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    DismissValue.Default -> Color.White
                                    else -> Color.Red
                                }
                            )
                            val alignment = Alignment.CenterEnd
                            val icon = Icons.Default.Delete
                            val scale by animateFloatAsState(
                                if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
                            )

                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = Dp(20f)),
                                contentAlignment = alignment
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = "Delete Icon",
                                    modifier = Modifier.scale(scale)
                                )
                            }
                        },
                        dismissContent = {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimensionResource(id = R.dimen.item_height_main))
                                    .align(alignment = Alignment.CenterVertically)
                            ) {
//                                setUpRow(item = item)
                                ListItem(it)
                            }
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun ListItem(item: MyData) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(start = 10.dp, end = 10.dp),
            shape = RoundedCornerShape(6.dp),
//            border = BorderStroke(width = 1.dp, color = Color.Black),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
            )
            {
                Image(
                    painterResource(R.drawable.sample1),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(40.dp)
                        .height(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(text = item.name, fontWeight = FontWeight.Bold)
                    Text(text = item.date)
                }
            }
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
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }

    companion object {
        private const val TAG = "EntryChoiceActivity"
        const val DATA_MAP = "DATA_MAP"
        private const val PERMISSION_REQUESTS = 1
        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
    }
}
