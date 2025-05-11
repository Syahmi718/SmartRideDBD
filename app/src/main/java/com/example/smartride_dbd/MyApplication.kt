package com.example.smartride_dbd

import android.app.Application

class MyApplication : Application() {
    var notificationsEnabled: Boolean = true
    val uiViewModel: UiViewModel by lazy { UiViewModel() }
}
