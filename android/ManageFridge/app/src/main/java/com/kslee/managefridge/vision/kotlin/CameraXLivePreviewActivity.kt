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

package com.kslee.managefridge.vision.kotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.widget.ToggleButton
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.annotation.KeepName
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.barcode.ZoomSuggestionOptions.ZoomCallback
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kslee.managefridge.vision.kotlin.barcodescanner.BarcodeScannerProcessor
import com.kslee.managefridge.vision.kotlin.facedetector.FaceDetectorProcessor
import com.kslee.managefridge.vision.kotlin.facemeshdetector.FaceMeshDetectorProcessor
import com.kslee.managefridge.vision.kotlin.labeldetector.LabelDetectorProcessor
import com.kslee.managefridge.vision.kotlin.objectdetector.ObjectDetectorProcessor
import com.kslee.managefridge.vision.kotlin.posedetector.PoseDetectorProcessor
import com.kslee.managefridge.vision.kotlin.segmenter.SegmenterProcessor
import com.kslee.managefridge.vision.kotlin.textdetector.TextRecognitionProcessor
import com.kslee.managefridge.R
import com.kslee.managefridge.vision.CameraXViewModel
import com.kslee.managefridge.vision.EntryChoiceActivity.Companion.DATAMAP
import com.kslee.managefridge.vision.GraphicOverlay
import com.kslee.managefridge.vision.MyData
import com.kslee.managefridge.vision.VisionImageProcessor
import com.kslee.managefridge.vision.preference.PreferenceUtils
import com.kslee.managefridge.vision.preference.SettingsActivity

/** Live preview demo app for ML Kit APIs using CameraX. */
@KeepName
class CameraXLivePreviewActivity :
    AppCompatActivity(), OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    var dataMap = HashMap<String, MyData>()
    var array = dataMap.toList()

    private var previewView: PreviewView? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    private var selectedModel = OBJECT_DETECTION
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION)
        }

        if (!intent.extras?.getString(STATE_SELECTED_MODEL, "")?.isEmpty()!!) {
            selectedModel = intent.extras?.getString(STATE_SELECTED_MODEL, OBJECT_DETECTION)!!
        }

        Log.d(TAG, "selectedModel = " + selectedModel)

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        setContentView(R.layout.activity_vision_camerax_live_preview)
        previewView = findViewById(R.id.preview_view)
        if (previewView == null) {
            Log.d(TAG, "previewView is null")
        }
        graphicOverlay = findViewById(R.id.graphic_overlay)
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null")
        }
        val spinner = findViewById<Spinner>(R.id.spinner)
        val options: MutableList<String> = ArrayList()
        options.add(OBJECT_DETECTION)
        options.add(OBJECT_DETECTION_CUSTOM)
        options.add(CUSTOM_AUTOML_OBJECT_DETECTION)
        options.add(FACE_DETECTION)
        options.add(BARCODE_SCANNING)
        options.add(IMAGE_LABELING)
        options.add(IMAGE_LABELING_CUSTOM)
        options.add(CUSTOM_AUTOML_LABELING)
        options.add(POSE_DETECTION)
        options.add(SELFIE_SEGMENTATION)
        options.add(TEXT_RECOGNITION_LATIN)
        options.add(TEXT_RECOGNITION_CHINESE)
        options.add(TEXT_RECOGNITION_DEVANAGARI)
        options.add(TEXT_RECOGNITION_JAPANESE)
        options.add(TEXT_RECOGNITION_KOREAN)
        options.add(FACE_MESH_DETECTION)

        // Creating adapter for spinner
        val dataAdapter = ArrayAdapter(this, R.layout.spinner_style, options)
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // attaching data adapter to spinner
        spinner.adapter = dataAdapter
        spinner.onItemSelectedListener = this
        val facingSwitch = findViewById<ToggleButton>(R.id.facing_switch)
        facingSwitch.setOnCheckedChangeListener(this)
        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(
                this,
                Observer { provider: ProcessCameraProvider? ->
                    cameraProvider = provider
                    bindAllCameraUseCases()
                }
            )

        val settingsButton = findViewById<ImageView>(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(applicationContext, SettingsActivity::class.java)
            intent.putExtra(
                SettingsActivity.EXTRA_LAUNCH_SOURCE,
                SettingsActivity.LaunchSource.CAMERAX_LIVE_PREVIEW
            )
            startActivity(intent)
        }
        val okButton = findViewById<Button>(R.id.ok_btn)
        okButton.setOnClickListener {
            val intent =
                Intent(this@CameraXLivePreviewActivity, CameraXLivePreviewActivity::class.java)
            intent.putExtra(DATAMAP, dataMap)
            setResult(RESULT_OK, intent)
            finish()
        }
        // compose 추가
        val composeView = findViewById<ComposeView>(R.id.compose_bottom_sheet)
        composeView.setContent {
            importCompose()
        }
    }

    @SuppressLint("UnrememberedMutableState")
    @Composable
    private fun importCompose() {
        Log.d(TAG, "importCompose")
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        array = dataMap.toList()
        var mutableList = mutableStateListOf<Pair<String, MyData>>()

        composeView.setContent {
            LazyColumn(Modifier.fillMaxSize()) {
                items(mutableList) {
                    ListItem(it)
                }
            }
        }
    }

    @Composable
    fun ListItem(item: Pair<String, MyData>, modifier: Modifier = Modifier) {
        Row(
            modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp)
                .padding(6.dp)
        ) {
            Text(text = item.first)
            Text(text = item.second.date)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun setBottomSheet() {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        var showBottomSheet by remember { mutableStateOf(false) }
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Show bottom sheet") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = "") },
                    onClick = {
                        showBottomSheet = true
                    }
                )
            }
        ) { contentPadding ->
            // Screen content
            Box(modifier = Modifier.padding(contentPadding)) { 
            Text(text = "contents Box")
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showBottomSheet = false
                    }
                    ,
                    sheetState = sheetState
                ) {
                    // Sheet content
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
                    Text("test text!!!!")
//                    Button(onClick = {
//                        scope.launch { sheetState.hide() }.invokeOnCompletion {
//                            if (!sheetState.isVisible) {
//                                showBottomSheet = false
//                            }
//                        }
//                    })
//                    {
//                        Text("Hide bottom sheet")
//                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(STATE_SELECTED_MODEL, selectedModel)
    }

    @Synchronized
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
//        selectedModel = parent?.getItemAtPosition(pos).toString()
//        Log.d(TAG, "Selected model: $selectedModel")
//        bindAnalysisUseCase()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing.
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (cameraProvider == null) {
            return
        }
        val newLensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        val newCameraSelector = CameraSelector.Builder().requireLensFacing(newLensFacing).build()
        try {
            if (cameraProvider!!.hasCamera(newCameraSelector)) {
                Log.d(TAG, "Set facing to " + newLensFacing)
                lensFacing = newLensFacing
                cameraSelector = newCameraSelector
                bindAllCameraUseCases()
                return
            }
        } catch (e: CameraInfoUnavailableException) {
            // Falls through
        }
        Toast.makeText(
            applicationContext,
            "This device does not have lens with facing: $newLensFacing",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    public override fun onResume() {
        super.onResume()
        bindAllCameraUseCases()
    }

    override fun onPause() {
        super.onPause()

        imageProcessor?.run { this.stop() }
    }

    public override fun onDestroy() {
        super.onDestroy()
        imageProcessor?.run { this.stop() }
    }

    private fun bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider!!.unbindAll()
            bindPreviewUseCase()
            bindAnalysisUseCase()
        }
    }

    private fun bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return
        }
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        val builder = Preview.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        previewUseCase = builder.build()
        previewUseCase!!.setSurfaceProvider(previewView!!.getSurfaceProvider())
        camera =
            cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this,
                cameraSelector!!,
                previewUseCase
            )
    }

    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        if (imageProcessor != null) {
            imageProcessor!!.stop()
        }
        imageProcessor =
            try {
                when (selectedModel) {
                    OBJECT_DETECTION -> {
                        Log.i(TAG, "Using Object Detector Processor")
                        val objectDetectorOptions =
                            PreferenceUtils.getObjectDetectorOptionsForLivePreview(this)
                        ObjectDetectorProcessor(this, objectDetectorOptions)
                    }

                    OBJECT_DETECTION_CUSTOM -> {
                        Log.i(TAG, "Using Custom Object Detector (with object labeler) Processor")
                        val localModel =
                            LocalModel.Builder()
                                .setAssetFilePath("custom_models/object_labeler.tflite").build()
                        val customObjectDetectorOptions =
                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                                this,
                                localModel
                            )
                        ObjectDetectorProcessor(this, customObjectDetectorOptions)
                    }

                    CUSTOM_AUTOML_OBJECT_DETECTION -> {
                        Log.i(TAG, "Using Custom AutoML Object Detector Processor")
                        val customAutoMLODTLocalModel =
                            LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json")
                                .build()
                        val customAutoMLODTOptions =
                            PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                                this,
                                customAutoMLODTLocalModel
                            )
                        ObjectDetectorProcessor(this, customAutoMLODTOptions)
                    }

                    TEXT_RECOGNITION_LATIN -> {
                        Log.i(TAG, "Using on-device Text recognition Processor for Latin")
                        TextRecognitionProcessor(this, TextRecognizerOptions.Builder().build())
                    }

                    TEXT_RECOGNITION_CHINESE -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Chinese"
                        )
                        TextRecognitionProcessor(
                            this,
                            ChineseTextRecognizerOptions.Builder().build()
                        )
                    }

                    TEXT_RECOGNITION_DEVANAGARI -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Devanagari"
                        )
                        TextRecognitionProcessor(
                            this,
                            DevanagariTextRecognizerOptions.Builder().build()
                        )
                    }

                    TEXT_RECOGNITION_JAPANESE -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Japanese"
                        )
                        TextRecognitionProcessor(
                            this,
                            JapaneseTextRecognizerOptions.Builder().build()
                        )
                    }

                    TEXT_RECOGNITION_KOREAN -> {
                        Log.i(
                            TAG,
                            "Using on-device Text recognition Processor for Latin and Korean"
                        )
                        TextRecognitionProcessor(
                            this,
                            KoreanTextRecognizerOptions.Builder().build()
                        )
                    }

                    FACE_DETECTION -> {
                        Log.i(TAG, "Using Face Detector Processor")
                        val faceDetectorOptions = PreferenceUtils.getFaceDetectorOptions(this)
                        FaceDetectorProcessor(this, faceDetectorOptions)
                    }

                    BARCODE_SCANNING -> {
                        Log.i(TAG, "Using Barcode Detector Processor")
                        var zoomCallback: ZoomCallback? = null
                        if (PreferenceUtils.shouldEnableAutoZoom(this)) {
                            zoomCallback = ZoomCallback { zoomLevel: Float ->
                                Log.i(TAG, "Set zoom ratio $zoomLevel")
                                val ignored = camera!!.cameraControl.setZoomRatio(zoomLevel)
                                true
                            }
                        }
                        BarcodeScannerProcessor(this, zoomCallback)
                    }

                    IMAGE_LABELING -> {
                        Log.i(TAG, "Using Image Label Detector Processor")
                        LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS)
                    }

                    IMAGE_LABELING_CUSTOM -> {
                        Log.i(TAG, "Using Custom Image Label (Birds) Detector Processor")
                        val localClassifier =
                            LocalModel.Builder()
                                .setAssetFilePath("custom_models/bird_classifier.tflite").build()
                        val customImageLabelerOptions =
                            CustomImageLabelerOptions.Builder(localClassifier).build()
                        LabelDetectorProcessor(this, customImageLabelerOptions)
                    }

                    CUSTOM_AUTOML_LABELING -> {
                        Log.i(TAG, "Using Custom AutoML Image Label Detector Processor")
                        val customAutoMLLabelLocalModel =
                            LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json")
                                .build()
                        val customAutoMLLabelOptions =
                            CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
                                .setConfidenceThreshold(0f)
                                .build()
                        LabelDetectorProcessor(this, customAutoMLLabelOptions)
                    }

                    POSE_DETECTION -> {
                        val poseDetectorOptions =
                            PreferenceUtils.getPoseDetectorOptionsForLivePreview(this)
                        val shouldShowInFrameLikelihood =
                            PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this)
                        val visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this)
                        val rescaleZ =
                            PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this)
                        val runClassification =
                            PreferenceUtils.shouldPoseDetectionRunClassification(this)
                        PoseDetectorProcessor(
                            this,
                            poseDetectorOptions,
                            shouldShowInFrameLikelihood,
                            visualizeZ,
                            rescaleZ,
                            runClassification,
                            /* isStreamMode = */ true
                        )
                    }

                    SELFIE_SEGMENTATION -> SegmenterProcessor(this)
                    FACE_MESH_DETECTION -> FaceMeshDetectorProcessor(this)
                    else -> throw IllegalStateException("Invalid model name")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Can not create image processor: $selectedModel", e)
                Toast.makeText(
                    applicationContext,
                    "Can not create image processor: " + e.localizedMessage,
                    Toast.LENGTH_LONG
                )
                    .show()
                return
            }

        val builder = ImageAnalysis.Builder()
        val targetResolution = PreferenceUtils.getCameraXTargetResolution(this, lensFacing)
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution)
        }
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            // imageProcessor.processImageProxy will use another thread to run the detection underneath,
            // thus we can just runs the analyzer itself on main thread.
            ContextCompat.getMainExecutor(this),
            ImageAnalysis.Analyzer { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.width,
                            imageProxy.height,
                            isImageFlipped
                        )
                    } else {
                        graphicOverlay!!.setImageSourceInfo(
                            imageProxy.height,
                            imageProxy.width,
                            isImageFlipped
                        )
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                } catch (e: MlKitException) {
                    Log.e(TAG, "Failed to process image. Error: " + e.localizedMessage)
                    Toast.makeText(applicationContext, e.localizedMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )
        cameraProvider!!.bindToLifecycle(/* lifecycleOwner= */ this,
            cameraSelector!!,
            analysisUseCase
        )
    }

    companion object {
        const val TAG = "CameraXLivePreview"
        const val OBJECT_DETECTION = "Object Detection"
        const val OBJECT_DETECTION_CUSTOM = "Custom Object Detection"
        const val CUSTOM_AUTOML_OBJECT_DETECTION = "Custom AutoML Object Detection (Flower)"
        const val FACE_DETECTION = "Face Detection"
        const val TEXT_RECOGNITION_LATIN = "Text Recognition Latin"
        const val TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese"
        const val TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari"
        const val TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese"
        const val TEXT_RECOGNITION_KOREAN = "Text Recognition Korean"
        const val BARCODE_SCANNING = "Barcode Scanning"
        const val IMAGE_LABELING = "Image Labeling"
        const val IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)"
        const val CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)"
        const val POSE_DETECTION = "Pose Detection"
        const val SELFIE_SEGMENTATION = "Selfie Segmentation"
        const val FACE_MESH_DETECTION = "Face Mesh Detection (Beta)"

        const val STATE_SELECTED_MODEL = "selected_model"
    }
}
