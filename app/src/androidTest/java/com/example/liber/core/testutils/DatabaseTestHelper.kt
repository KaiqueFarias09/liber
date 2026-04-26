package com.example.liber.core.testutils

import androidx.test.platform.app.InstrumentationRegistry
import com.example.liber.data.local.AppDatabase

object DatabaseTestHelper {
    fun clear() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()
    }
}
