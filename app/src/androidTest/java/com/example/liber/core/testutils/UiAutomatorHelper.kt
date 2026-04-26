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
        Thread.sleep(1500)

        // 1. Try to navigate to "Books" or "Internal Storage" via the drawer
        try {
            val drawerButton = device.findObject(UiSelector().descriptionMatches("(?i)Show roots|Navegar para cima|Open navigation drawer|Navegação"))
            if (drawerButton.waitForExists(3000)) {
                drawerButton.click()
                Thread.sleep(1000)
                
                val booksRoot = device.findObject(UiSelector().textMatches("(?i)Books|Livros"))
                if (booksRoot.waitForExists(2000)) {
                    booksRoot.click()
                } else {
                    val internalStorage = device.findObject(UiSelector().textMatches("(?i).*storage.*|.*sdcard.*|sdk_gphone.*"))
                    if (internalStorage.waitForExists(2000)) {
                        internalStorage.click()
                    } else {
                        device.pressBack()
                    }
                }
            }
        } catch (e: Exception) {
        }
        
        Thread.sleep(1500)

        // 2. Enter "Books" if not already there
        val booksInList = device.findObject(UiSelector().textMatches("(?i)Books|Livros"))
        if (booksInList.waitForExists(3000)) {
            booksInList.click()
            Thread.sleep(1000)
        }

        // 3. Enter "LiberTests"
        val testFolderInList = device.findObject(UiSelector().text("LiberTests"))
        if (testFolderInList.waitForExists(3000)) {
            testFolderInList.click()
            Thread.sleep(1000)
        }

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
        Thread.sleep(1500)

        // 1. Try to get to "Books" or "Internal Storage" via the drawer
        try {
            val drawerButton = device.findObject(UiSelector().descriptionMatches("(?i)Show roots|Navegar para cima|Open navigation drawer|Navegação"))
            if (drawerButton.waitForExists(3000)) {
                drawerButton.click()
                Thread.sleep(1000)
                
                val booksRoot = device.findObject(UiSelector().textMatches("(?i)Books|Livros"))
                if (booksRoot.waitForExists(2000)) {
                    booksRoot.click()
                } else {
                    val internalStorage = device.findObject(UiSelector().textMatches("(?i).*storage.*|.*sdcard.*|sdk_gphone.*"))
                    if (internalStorage.waitForExists(2000)) {
                        internalStorage.click()
                    } else {
                        // If we can't find a good root, just close the drawer and try our luck in the current list
                        device.pressBack()
                    }
                }
            }
        } catch (e: Exception) {
            // Navigation drawer might not exist or be different, continue
        }

        Thread.sleep(1500)

        // 2. If we are at the root (like in the screenshot), we need to enter "Books"
        val booksInList = device.findObject(UiSelector().textMatches("(?i)Books|Livros"))
        if (booksInList.waitForExists(3000)) {
            booksInList.click()
            Thread.sleep(1000)
        }

        // 3. Enter "LiberTests"
        val testFolderInList = device.findObject(UiSelector().text("LiberTests"))
        if (testFolderInList.waitForExists(3000)) {
            testFolderInList.click()
            Thread.sleep(1000)
        }

        // 4. Find and click the specific folder we want (e.g., "test_audiobook")
        val folderSelector = UiSelector().text(folderName)
        val folderObject = device.findObject(folderSelector)

        if (folderObject.waitForExists(TIMEOUT)) {
            folderObject.click()
            Thread.sleep(1000)
        } else {
            // Try to scroll if it's not visible
            try {
                val scrollableList = UiScrollable(UiSelector().scrollable(true))
                if (scrollableList.exists()) {
                    if (scrollableList.scrollIntoView(folderSelector)) {
                        device.findObject(folderSelector).click()
                        Thread.sleep(1000)
                    }
                }
            } catch (e: Exception) {}
        }

        // 5. Finalize selection
        val useFolderButton =
            device.findObject(UiSelector().textMatches("(?i)use this folder|select|usar esta pasta|selecionar"))
        if (useFolderButton.waitForExists(TIMEOUT)) {
            useFolderButton.click()
        } else {
            // Try searching by ID if text match fails
            val useButtonById = device.findObject(UiSelector().resourceId("android:id/button1"))
            if (useButtonById.waitForExists(2000)) {
                useButtonById.click()
            } else {
                throw RuntimeException("Could not find 'Use this folder' or 'Select' button. Current state might be invalid.")
            }
        }

        val allowButton = device.findObject(UiSelector().textMatches("(?i)allow|permitir"))
        if (allowButton.waitForExists(TIMEOUT)) {
            allowButton.click()
        }
        
        // Wait for the picker to disappear and return to our app
        val appPackage = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        device.wait(Until.hasObject(By.pkg(appPackage)), TIMEOUT)
        Thread.sleep(2000)
    }
}
