package com.weatherxm.data.datasource

import android.content.SharedPreferences
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

interface SharedPreferencesDataSource {
    fun getPreferenceChangeFlow(): Flow<String>
}

class SharedPreferencesDataSourceImpl(
    preferences: SharedPreferences
) : SharedPreferencesDataSource {

    private val flow = MutableSharedFlow<String>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            Timber.d("SharedPreferences key changed [key=$key]")
            flow.tryEmit(key)
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }

    override fun getPreferenceChangeFlow() = flow
}
