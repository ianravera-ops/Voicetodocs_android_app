package com.voicetodocs.cos.data.digest

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicetodocs.cos.CosApplication

class VipDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? CosApplication ?: return Result.success()
        return try {
            VipDigestRunner(app.container, VipNotifier(app)).run(notify = true)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
