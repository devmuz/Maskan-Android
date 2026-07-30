package com.maskan.mobileapp

import android.app.Application
import com.maskan.mobileapp.di.AppContainer

class MaskanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
