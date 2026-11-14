package com.callrecorder.helper

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.callrecorder.core.CallStateTracker
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cœur de l'enregistrement automatique.
 * Lié en permanence par le système, il garde l'application en vie, écoute l'état des appels et,
 * depuis Android 10, donne le droit de capter le micro pendant un appel. Il ignore tout le contenu de l'écran.
 */
class CallAccessibilityService : AccessibilityService() {
    private val tracker = CallStateTracker()
    private var telephonyCallback: Any? = null
    private var phoneListener: Any? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _running.value = true
        listen()
        // Enregistrements restés en attente (application absente lors d'un appel précédent…).
        kotlin.concurrent.thread(name = "deliver") { AppLink.cleanup(this); AppLink.deliver(this) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        unlisten()
        CallSession.stop(this)
        instance = null
        _running.value = false
        super.onDestroy()
    }

    /** Appelé aussi quand l'utilisateur accorde la permission Téléphone après coup. */
    fun listen() {
        if (telephonyCallback != null || phoneListener != null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return
        val tm = getSystemService(TelephonyManager::class.java) ?: return
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) = onState(state)
                }
                tm.registerTelephonyCallback(mainExecutor, cb)
                telephonyCallback = cb
            } else {
                @Suppress("DEPRECATION")
                val l = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) = onState(state)
                }
                @Suppress("DEPRECATION")
                tm.listen(l, PhoneStateListener.LISTEN_CALL_STATE)
                phoneListener = l
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Écoute des appels refusée", e)
        }
    }

    private fun unlisten() {
        val tm = getSystemService(TelephonyManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 31) (telephonyCallback as? TelephonyCallback)?.let { tm.unregisterTelephonyCallback(it) }
        @Suppress("DEPRECATION")
        (phoneListener as? PhoneStateListener)?.let { tm.listen(it, PhoneStateListener.LISTEN_NONE) }
        telephonyCallback = null
        phoneListener = null
    }

    private fun onState(state: Int) {
        when (val action = tracker.onState(state)) {
            is CallStateTracker.Action.Start -> CallSession.start(this, action.direction)
            CallStateTracker.Action.Stop -> CallSession.stop(this)
            null -> Unit
        }
    }

    companion object {
        private const val TAG = "CallA11yService"
        var instance: CallAccessibilityService? = null
            private set

        private val _running = MutableStateFlow(false)
        /** Service connecté par le système dans ce processus. */
        val running: StateFlow<Boolean> = _running.asStateFlow()

        /** Activé dans les réglages d'accessibilité (même si le processus vient de démarrer). */
        fun isEnabled(context: Context): Boolean {
            val me = ComponentName(context, CallAccessibilityService::class.java)
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return list.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
