package com.example.liber.core.testutils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

object UiAutomatorHelper {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private const val TIMEOUT = 10000L

    fun selectFileInSystemPicker(fileName: String) {
        device.waitForIdle()

        val fileSelector = UiSelector().text(fileName)
        val fileObject = device.findObject(fileSelector)

        // Wait for picker activity to stabilize
        Thread.sleep(1000)

        // Try to find the file directly first
        if (fileObject.waitForExists(TIMEOUT)) {
            fileObject.click()
            return
        }

        // If not found, try to scroll
        try {
            val scrollableList = UiScrollable(UiSelector().scrollable(true))
            if (scrollableList.exists()) {
                if (scrollableList.scrollIntoView(fileSelector)) {
                    device.findObject(fileSelector).click()
                    return
                }
            }
        } catch (e: Exception) {
            // Might not be scrollable or already in view
        }

        // Try searching as a fallback
        val searchButton =
            device.findObject(UiSelector().descriptionMatches("(?i)Search|Pesquisar|Search files"))
        if (searchButton.exists()) {
            searchButton.click()
            val searchField =
                device.findObject(UiSelector().className("android.widget.EditText"))
            if (searchField.waitForExists(2000)) {
                searchField.setText(fileName)
                device.pressEnter()
                val foundAfterSearch = device.findObject(fileSelector)
                if (foundAfterSearch.waitForExists(TIMEOUT)) {
                    foundAfterSearch.click()
                    return
                }
            }
        }

        throw RuntimeException("Could not find file '$fileName' in system picker.")
    }

    fun selectFolderInSystemPicker(folderName: String) {
        device.waitForIdle()
        Thread.sleep(1000)

        val folderSelector = UiSelector().text(folderName)
        val folderObject = device.findObject(folderSelector)

        if (folderObject.waitForExists(TIMEOUT)) {
            folderObject.click()
        } else {
            // Try to scroll
            try {
                val scrollableList = UiScrollable(UiSelector().scrollable(true))
                if (scrollableList.exists()) {
                    scrollableList.scrollIntoView(folderSelector)
                    device.findObject(folderSelector).click()
                }
            } catch (e: Exception) {}
        }

        val useFolderButton =
            device.findObject(UiSelector().textMatches("(?i)use this folder|select|usar esta pasta"))
        if (useFolderButton.waitForExists(TIMEOUT)) {
            useFolderButton.click()
        }

        val allowButton = device.findObject(UiSelector().textMatches("(?i)allow|permitir"))
        if (allowButton.waitForExists(TIMEOUT)) {
            allowButton.click()
        }
    }
}
