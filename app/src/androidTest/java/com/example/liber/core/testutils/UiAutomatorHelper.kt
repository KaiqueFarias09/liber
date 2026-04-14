package com.example.liber.core.testutils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

object UiAutomatorHelper {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private const val TIMEOUT = 5000L

    fun selectFileInSystemPicker(fileName: String) {
        device.wait(Until.hasObject(By.text(fileName)), TIMEOUT)
        device.findObject(UiSelector().text(fileName)).click()
    }

    fun selectFolderInSystemPicker(folderName: String) {
        device.wait(Until.hasObject(By.text(folderName)), TIMEOUT)
        device.findObject(UiSelector().text(folderName)).click()

        val useFolderButton =
            device.findObject(UiSelector().textMatches("(?i)use this folder|select"))
        if (useFolderButton.exists()) {
            useFolderButton.click()
        }

        val allowButton = device.findObject(UiSelector().textMatches("(?i)allow"))
        if (allowButton.exists()) {
            allowButton.click()
        }
    }
}
