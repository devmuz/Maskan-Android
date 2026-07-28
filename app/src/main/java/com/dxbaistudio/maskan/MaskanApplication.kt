package com.dxbaistudio.maskan

import android.app.Application
import com.dxbaistudio.maskan.di.AppContainer

class MaskanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
