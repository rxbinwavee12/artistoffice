package com.hb.office

import android.content.Context
import androidx.work.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class ApprovalWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()

        try {
            val db = FirebaseFirestore.getInstance()
            // Synchrone Abfrage in der Hintergrund-Task
            val documentTask = db.collection("users").document(uid).get()
            val snapshot = Tasks.await(documentTask, 10, TimeUnit.SECONDS)

            if (snapshot != null && snapshot.exists()) {
                val role = snapshot.getString("role") ?: "user"
                val allowedProjects = (snapshot.get("allowedProjects") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()

                val isApproved = role != "user" || allowedProjects.isNotEmpty()

                if (isApproved) {
                    // 1. Push-Benachrichtigung feuern
                    NotificationHelper.showApprovalNotification(appContext)

                    // 2. WorkManager stoppen, da der Nutzer nun freigeschaltet ist!
                    WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "approval_check_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Mindestabstand für PeriodicWorkRequest ist 15 Minuten
            val workRequest = PeriodicWorkRequestBuilder<ApprovalWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
