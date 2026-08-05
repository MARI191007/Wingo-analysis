package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MainPredictorScreen
import com.example.ui.theme.WingoTheme

class MainActivity : ComponentActivity() {
  override fun getAttributionTag(): String? {
    return "default"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      WingoTheme {
        MainPredictorScreen()
      }
    }
  }
}

