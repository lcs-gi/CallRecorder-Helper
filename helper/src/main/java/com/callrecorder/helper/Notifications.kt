package com.callrecorder.helper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.callrecorder.core.Contract

/** Notifications du Helper. « Appel enregistré » est affichée par l'application, qui reçoit l'enregistrement. */
object Notifications {
    const val CHANNEL_LIVE = "live"
    const val CHANNEL_STATUS = "status"
    const val ID_LIVE = 1
    private const val ID_ERROR = 2
    private const val ID_PENDING = 3
    private const val ID_CALIBRATION = 4

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE, context.getString(R.string.channel_live), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, context.getString(R.string.channel_status), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun canNotify(context: Context) =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** L'application CallRecorder si elle est installée, sinon l'écran du Helper. */
    private fun openTarget(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(Contract.APP_PACKAGE)
            ?: Intent(context, HelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openHelper(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 1, Intent(context, HelperActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun live(context: Context, startedAt: Long) =
        NotificationCompat.Builder(context, CHANNEL_LIVE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_recording))
            .setContentText(context.getString(R.string.notif_recording_text))
            .setWhen(startedAt)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openTarget(context))
            .build()

    fun showError(context: Context) = post(
        context, ID_ERROR,
        NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_error))
            .setContentText(context.getString(R.string.notif_error_text))
            .setAutoCancel(true)
            .setContentIntent(openHelper(context)),
    )

    fun showPending(context: Context) = post(
        context, ID_PENDING,
        NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_pending))
            .setContentText(context.getString(R.string.notif_pending_text))
            .setAutoCancel(true)
            .setContentIntent(openHelper(context)),
    )

    /** Assistant de réglage du son : les essais sont finis, l'utilisateur peut raccrocher. */
    fun showCalibrationDone(context: Context) = post(
        context, ID_CALIBRATION,
        NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_calibration))
            .setContentText(context.getString(R.string.notif_calibration_text))
            .setAutoCancel(true)
            .setTimeoutAfter(5 * 60_000L)
            .setContentIntent(openTarget(context)),
    )

    fun cancelPending(context: Context) = NotificationManagerCompat.from(context).cancel(ID_PENDING)

    /** Permission vérifiée juste avant ; elle peut être retirée entre-temps, d'où la SecurityException. */
    fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("Notifications", "Notification refusée", e)
        }
    }

    private fun post(context: Context, id: Int, b: NotificationCompat.Builder) = post(context, id, b.build())
}
