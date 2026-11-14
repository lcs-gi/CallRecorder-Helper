package com.callrecorder.helper

import android.media.AudioDeviceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Le haut-parleur n'est jamais forcé quand l'utilisateur porte un casque ou des écouteurs. */
class CallAudioTest {
    @Test
    fun telephoneSeul() {
        assertFalse(CallAudio.isHeadset(listOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)))
        assertFalse(CallAudio.isHeadset(emptyList()))
    }

    @Test
    fun casqueOuEcouteurs() {
        listOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_USB_HEADSET, 26,
        ).forEach { assertTrue("type $it", CallAudio.isHeadset(listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, it))) }
    }
}
