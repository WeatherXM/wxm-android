package com.weatherxm.data.repository

import com.weatherxm.data.datasource.SharedPreferencesDataSource
import kotlinx.coroutines.flow.Flow

interface SharedPreferencesRepository {
    fun getPreferenceChangeFlow(): Flow<String>
}

class SharedPreferenceRepositoryImpl(
    private val source: SharedPreferencesDataSource
) : SharedPreferencesRepository {
    override fun getPreferenceChangeFlow() = source.getPreferenceChangeFlow()
}
