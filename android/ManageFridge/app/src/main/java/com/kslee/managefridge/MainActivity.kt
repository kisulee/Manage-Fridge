package com.kslee.managefridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kslee.managefridge.vision.EntryChoiceActivity
import com.kslee.managefridge.ui.theme.ManageFridgeTheme
import com.kslee.managefridge.vision.kotlin.CameraXLivePreviewActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManageFridgeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InitUi()
                }
            }
        }
    }
}

@Composable
fun InitUi( modifier: Modifier = Modifier) {
    val activity = LocalContext.current as Activity
    Button(
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.wrapContentSize(),
        onClick = {
        activity.startActivity(Intent(activity, EntryChoiceActivity::class.java))
    }) {
        Text(text = "다음")
    }
    activity.startActivity(Intent(activity, EntryChoiceActivity::class.java))
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ManageFridgeTheme {
        InitUi()
    }
}