package com.example.liber.core.testutils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

object DataStoreTestHelper {
    fun clear() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }
}
