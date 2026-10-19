package com.mfa.admanager.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mfa.admanager.demo.ui.screen.AdManagerDemoScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AdManagerDemoScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdManagerDemoPreview() {
    AdManagerDemoScreen()
}
