package com.example.liber.core.testutils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataStoreEntryPoint {
    fun dataStore(): DataStore<Preferences>
}

object DataStoreTestHelper {
    fun clear() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val dataStore = EntryPoints.get(context, DataStoreEntryPoint::class.java).dataStore()
        runBlocking {
            dataStore.edit { it.clear() }
        }
    }
}
