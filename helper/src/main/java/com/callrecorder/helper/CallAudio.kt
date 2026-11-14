package com.callrecorder.helper

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Réglages du son pendant un appel enregistré, pour que le micro capte le correspondant :
 * haut-parleur et/ou volume de l'écouteur au maximum. Tout est rétabli au raccroché.
 *
 * Android ne donne pas le son de l'appel aux applications ordinaires (permission réservée au système) :
 * le correspondant n'est enregistré qu'à travers le micro, d'où ces réglages.
 */
class CallAudio(context: Context) {
    private val am = context.getSystemService(AudioManager::class.java)
    private var previousVolume: Int? = null
    private var speakerForced = false
    private var previousSpeaker = false

    fun apply(speaker: Boolean, boost: Boolean) {
        if (boost && previousVolume == null) runCatching {
            previousVolume = am.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0)
        }.onFailure { Log.w(TAG, "Volume", it) }

        // Casque ou écouteurs : on ne bascule pas l'appel sur le haut-parleur à l'insu de l'utilisateur.
        if (speaker && !hasHeadset()) runCatching { enableSpeaker() }.onFailure { Log.w(TAG, "Haut-parleur", it) }
    }

    /**
     * Le téléphone remet parfois l'écouteur quand l'appel est décroché par le correspondant :
     * le haut-parleur est demandé à nouveau quelques secondes après le début.
     */
    fun reapplySpeaker() {
        if (speakerForced && !hasHeadset()) runCatching { enableSpeaker() }
    }

    fun restore() {
        previousVolume?.let { v -> runCatching { am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, v, 0) } }
        previousVolume = null
        if (speakerForced) runCatching {
            if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice()
            else @Suppress("DEPRECATION") { am.isSpeakerphoneOn = previousSpeaker }
        }
        speakerForced = false
    }

    private fun enableSpeaker() {
        if (Build.VERSION.SDK_INT >= 31) {
            val device = am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } ?: return
            speakerForced = am.setCommunicationDevice(device) || speakerForced
        } else {
            @Suppress("DEPRECATION")
            if (!speakerForced) previousSpeaker = am.isSpeakerphoneOn
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = true
            speakerForced = true
        }
    }

    private fun hasHeadset(): Boolean =
        isHeadset(am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type })

    companion object {
        private const val TAG = "CallAudio"

        private val HEADSET_TYPES = buildSet {
            add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
            add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_USB_HEADSET)
            add(AudioDeviceInfo.TYPE_HEARING_AID)
            add(26) // TYPE_BLE_HEADSET (Android 12)
        }

        /** Un casque, des écouteurs filaires ou Bluetooth, un appareil auditif est connecté. */
        fun isHeadset(outputTypes: Collection<Int>): Boolean = outputTypes.any { it in HEADSET_TYPES }
    }
}
