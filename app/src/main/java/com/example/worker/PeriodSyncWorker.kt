package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.WingoDatabase
import com.example.data.repository.WingoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PeriodSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = WingoDatabase.getInstance(applicationContext)
            val repository = WingoRepository(db.periodDao(), db.predictionDao())

            val gameModes = listOf("1Min", "3Min", "5Min", "10Min", "30s")
            for (mode in gameModes) {
                repository.syncOnlinePeriodHistory(mode)
            }

            // Schedule the next sync in 5 minutes
            scheduleNextRun(applicationContext)

            Result.success()
        } catch (e: Exception) {
            // Re-schedule even if failed so background sync continues
            scheduleNextRun(applicationContext)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "PeriodSyncWorker_5Min"

        private fun scheduleNextRun(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<PeriodSyncWorker>()
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Immediate initial sync
            val immediateRequest = OneTimeWorkRequestBuilder<PeriodSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                immediateRequest
            )
        }
    }
}
