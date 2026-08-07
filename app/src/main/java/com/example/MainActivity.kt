package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MainPredictorScreen
import com.example.ui.theme.WingoTheme
import com.example.worker.PeriodSyncWorker

class MainActivity : ComponentActivity() {
  override fun getAttributionTag(): String? {
    return "default"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Schedule background background period syncing via WorkManager
    PeriodSyncWorker.schedulePeriodicSync(applicationContext)

    setContent {
      WingoTheme {
        MainPredictorScreen()
      }
    }
  }
}


