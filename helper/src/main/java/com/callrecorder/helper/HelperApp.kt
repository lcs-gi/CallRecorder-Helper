package com.callrecorder.helper

import android.app.Application

class HelperApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
