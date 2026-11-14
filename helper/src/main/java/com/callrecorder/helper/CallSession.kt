package com.callrecorder.helper

import android.content.Context
import android.os.PowerManager
import android.util.Log
import com.callrecorder.core.AppSettings
import com.callrecorder.core.Direction
import com.callrecorder.core.Finalize
import com.callrecorder.core.Format
import com.callrecorder.core.LiveCall
import com.callrecorder.core.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Enregistrement de l'appel en cours : démarre au décroché, s'arrête au raccroché,
 * puis identifie le correspondant, applique le filtre et remet le fichier à l'application.
 */
object CallSession {
    private const val TAG = "CallSession"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _live = MutableStateFlow<LiveCall?>(null)
    val live: StateFlow<LiveCall?> = _live.asStateFlow()

    /** Appel en cours (enregistré ou non). [generation] distingue deux appels qui se suivent de près. */
    private var active = false
    private var generation = 0
    private var settings = AppSettings()
    private var recorder: AudioRecorder? = null
    private var tempFile: File? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audio: CallAudio? = null

    fun start(context: Context, direction: Direction) {
        if (active) return
        active = true
        val token = ++generation
        val app = context.applicationContext
        val startedAt = System.currentTimeMillis()

        scope.launch {
            // Réglages lus dans l'application (autre processus) : hors du fil principal.
            val s = withContext(Dispatchers.IO) { AppLink.settings(app) }
            if (!active || token != generation || !s.autoRecord) return@launch
            settings = s

            // Haut-parleur / volume avant de démarrer le micro, pour capter le correspondant dès le début.
            val callAudio = CallAudio(app).also { it.apply(speaker = s.autoSpeaker, boost = s.boostVolume) }
            audio = callAudio
            if (s.autoSpeaker) launch {
                for (wait in listOf(1_500L, 3_000L)) {
                    delay(wait)
                    if (token == generation && active) callAudio.reapplySpeaker()
                }
            }

            val file = File(AppLink.pendingDir(app), "recording_$startedAt.m4a.part")
            val r = AudioRecorder(app)
            if (!r.start(file, s.source, s.quality, scope)) {
                Log.e(TAG, "Aucune source audio disponible")
                Notifications.showError(app)
                callAudio.restore()
                audio = null
                return@launch
            }
            recorder = r
            tempFile = file
            val call = LiveCall(startedAt, direction)
            _live.value = call
            wakeLock = app.getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CallRecorderHelper:call")
                .apply { acquire(4 * 3600_000L) }
            RecordingService.start(app)
            launch(Dispatchers.IO) { AppLink.liveStart(app, call) }
        }
    }

    fun stop(context: Context) {
        if (!active) return
        active = false
        audio?.restore()
        audio = null
        val r = recorder ?: return
        val call = _live.value ?: return
        val file = tempFile ?: return
        recorder = null
        tempFile = null
        _live.value = null
        val (wave, silent) = r.stop()
        val source = r.usedSource
        runCatching { wakeLock?.release() }
        wakeLock = null

        val app = context.applicationContext
        RecordingService.stop(app)
        val endedAt = System.currentTimeMillis()
        val s = settings

        scope.launch(Dispatchers.IO) {
            AppLink.liveStop(app)
            val info = CallInfoResolver(app).resolve(call.startedAt)
            val durationMs = endedAt - call.startedAt
            if (!Finalize.shouldKeep(file.length(), durationMs, s.filter, info)) {
                file.delete()
                return@launch
            }
            val id = UUID.randomUUID().toString()
            val rec = Recording(
                id = id,
                // Nom proposé ; l'application le rend unique dans son dossier.
                fileName = Format.fileName(app.getString(R.string.file_prefix), call.startedAt, info.contactName ?: info.number),
                startedAt = call.startedAt,
                durationMs = durationMs,
                direction = info.direction ?: call.direction,
                number = info.number,
                contactName = info.contactName,
                sizeBytes = file.length(),
                silent = silent,
                audioSource = source ?: s.source,
            )
            AppLink.enqueue(app, rec, file, wave)
            AppLink.deliver(app)
            // Application absente ou injoignable : l'enregistrement attend dans le Helper.
            if (AppLink.pendingCount(app) > 0) Notifications.showPending(app)
        }
    }
}
