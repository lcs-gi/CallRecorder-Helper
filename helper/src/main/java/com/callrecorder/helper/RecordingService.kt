package com.callrecorder.helper

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Service de premier plan « micro » pendant l'appel : affiche la notification et protège l'enregistrement.
 * L'enregistrement lui-même est géré par [CallSession] ; si le système refuse ce service
 * (démarrage en arrière-plan restreint), une simple notification le remplace.
 */
class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedAt = CallSession.live.value?.startedAt
        if (startedAt == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this, Notifications.ID_LIVE, Notifications.live(this, startedAt),
                if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0,
            )
        } catch (e: Exception) {
            Log.w("RecordingService", "Premier plan refusé", e)
            postPlain(this, startedAt)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    companion object {
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java))
            } catch (e: Exception) {
                Log.w("RecordingService", "Démarrage refusé", e)
                CallSession.live.value?.let { postPlain(context, it.startedAt) }
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RecordingService::class.java))
            NotificationManagerCompat.from(context).cancel(Notifications.ID_LIVE)
        }

        private fun postPlain(context: Context, startedAt: Long) {
            Notifications.post(context, Notifications.ID_LIVE, Notifications.live(context, startedAt))
        }
    }
}
