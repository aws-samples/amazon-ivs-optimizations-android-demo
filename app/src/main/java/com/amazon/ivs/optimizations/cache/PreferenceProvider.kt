package com.amazon.ivs.optimizations.cache

import android.content.Context
import com.amazon.ivs.optimizations.ui.settings.IVS_PLAYBACK_URL_BASE
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

const val PREFERENCES_NAME = "app_preferences"

class PreferenceProvider(context: Context, preferencesName: String) {

    var capturedClickTime by longPreference()
    var useCustomUrl by booleanPreference()
    var customLiveStreamUrl by stringPreference()
    val playbackUrl get() = customLiveStreamUrl.takeIf { useCustomUrl && customLiveStreamUrl?.contains(IVS_PLAYBACK_URL_BASE) == true }

    private val sharedPreferences by lazy { context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE) }

    private fun longPreference() = object : ReadWriteProperty<Any?, Long?> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getLong(property.name, -1)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Long?) {
            sharedPreferences.edit().putLong(property.name, value ?: -1).apply()
        }
    }

    private fun booleanPreference() = object : ReadWriteProperty<Any?, Boolean> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) =
            sharedPreferences.getBoolean(property.name, false)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
            sharedPreferences.edit().putBoolean(property.name, value).apply()
        }
    }

    private fun stringPreference() = object : ReadWriteProperty<Any?, String?> {

        override fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(property.name, null)

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
            sharedPreferences.edit().putString(property.name, value).apply()
        }
    }
}
