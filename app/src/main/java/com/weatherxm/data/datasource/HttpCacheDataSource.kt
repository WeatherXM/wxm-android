package com.weatherxm.data.datasource

import okhttp3.Cache

interface HttpCacheDataSource {
    suspend fun clear()
}

class HttpCacheDataSourceImpl(private val okHttpCache: Cache) : HttpCacheDataSource {

    override suspend fun clear() {
        okHttpCache.evictAll()
    }
}
