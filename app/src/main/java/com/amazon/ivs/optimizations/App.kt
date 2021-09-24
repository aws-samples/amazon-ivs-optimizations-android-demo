package com.amazon.ivs.optimizations

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.amazon.ivs.optimizations.common.LineNumberDebugTree
import timber.log.Timber

open class App : Application(), ViewModelStoreOwner {

    override fun getViewModelStore() = appViewModelStore

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree("IVS Best Practices"))
        }
    }

    companion object {
        private val appViewModelStore: ViewModelStore by lazy {
            ViewModelStore()
        }
    }
}
