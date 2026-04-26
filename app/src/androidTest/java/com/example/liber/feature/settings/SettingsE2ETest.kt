package com.example.liber.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.os.LocaleListCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.liber.MainActivity
import com.example.liber.R
import com.example.liber.core.testutils.DataStoreTestHelper
import com.example.liber.core.testutils.DatabaseTestHelper
import com.example.liber.core.testutils.FileTestHelper
import com.example.liber.core.testutils.UiAutomatorHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsE2ETest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.POST_NOTIFICATIONS,
    )

    @Before
    fun setup() {
        hiltRule.inject()

        // Force English at the start of every test
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }

        com.example.liber.feature.reader.engine.CREngine.init(InstrumentationRegistry.getInstrumentation().targetContext)

        DataStoreTestHelper.clear()
        DatabaseTestHelper.clear()
        FileTestHelper.copyAssetsToEmulatorStorage()

        val settingsLabel = composeTestRule.activity.getString(R.string.tab_settings)
        composeTestRule.onAllNodesWithText(settingsLabel).onLast().performClick()
    }

    @After
    fun teardown() {
        // FileTestHelper.cleanUpEmulatorStorage()
    }

    @Test
    fun scenario1_importEpubFile() {
        val importLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_books_from_files)
        composeTestRule.onNodeWithText(importLabel).performClick()

        UiAutomatorHelper.selectFileInSystemPicker("Jekyll.epub")

        // Navigate to Library
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onAllNodesWithText(libraryLabel).onLast().performClick()

        // Assert book appears via content description (cover)
        val bookTitle = "The strange case of Dr. Jekyll and Mr. Hyde"
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithContentDescription(bookTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun scenario2_addScanFolder() {
        val scanFoldersLabel =
            composeTestRule.activity.getString(R.string.settings_label_scan_folders)
        composeTestRule.onNodeWithText(scanFoldersLabel).performClick()

        // Wait for the Scan Folders screen to open (assert description text)
        val description = composeTestRule.activity.getString(R.string.scan_folders_description)
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText(description, substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // Click Add folder
        val addFolderLabel = composeTestRule.activity.getString(R.string.scan_folders_add_action)
        composeTestRule.onNodeWithText(addFolderLabel).performClick()

        UiAutomatorHelper.selectFolderInSystemPicker("test_audiobook")

        // Wait for the app to resume and for Compose to be ready
        Thread.sleep(2000)

        // Verify folder is listed in ScanFoldersScreen
        composeTestRule.onAllNodesWithText("test_audiobook", substring = true).onLast().assertIsDisplayed()

        // Go back to Settings screen so we can see the bottom navigation tabs
        val backLabel = composeTestRule.activity.getString(R.string.audio_control_back)
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // Give it a moment to transition back
        Thread.sleep(1000)

        // Navigate to Library -> Audiobooks
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onAllNodesWithText(libraryLabel).onLast().performClick()

        val audiobooksLabel = composeTestRule.activity.getString(R.string.tab_audiobooks)
        composeTestRule.onNodeWithText(audiobooksLabel).performClick()

        // Assert empty state or banner appears since the folder is empty
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("No audiobooks", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No audiobooks", substring = true).assertIsDisplayed()
    }

    @Test
    fun scenario2b_addScanFolderWithAudiobook() {
        val scanFoldersLabel =
            composeTestRule.activity.getString(R.string.settings_label_scan_folders)
        composeTestRule.onNodeWithText(scanFoldersLabel).performClick()

        // Click Add folder
        val addFolderLabel = composeTestRule.activity.getString(R.string.scan_folders_add_action)
        composeTestRule.onNodeWithText(addFolderLabel).performClick()

        UiAutomatorHelper.selectFolderInSystemPicker("white_nights_librivox")

        // Wait for scan to complete
        Thread.sleep(2000)

        // Verify folder is listed
        composeTestRule.onAllNodesWithText("white_nights_librivox", substring = true).onLast().assertIsDisplayed()

        // Go back to Settings
        val backLabel = composeTestRule.activity.getString(R.string.audio_control_back)
        composeTestRule.onNodeWithContentDescription(backLabel).performClick()

        // Navigate to Library -> Audiobooks
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onAllNodesWithText(libraryLabel).onLast().performClick()

        val audiobooksLabel = composeTestRule.activity.getString(R.string.tab_audiobooks)
        composeTestRule.onNodeWithText(audiobooksLabel).performClick()

        // Assert audiobook appears with title from metadata
        val audioTitle = "White Nights"
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithContentDescription(audioTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(audioTitle, substring = true).assertIsDisplayed()
    }

    @Test
    fun scenario3_changeTheme() {
        val darkLabel = composeTestRule.activity.getString(R.string.settings_theme_dark)
        composeTestRule.onNodeWithText(darkLabel).performClick()

        // Validation: Verify the button is selected
        composeTestRule.onNodeWithText(darkLabel).assertIsSelected()
    }

    @Test
    fun scenario4_changeLanguage() {
        val languageLabel = composeTestRule.activity.getString(R.string.settings_language)
        // Click the language selector row
        composeTestRule.onNodeWithText(languageLabel, substring = true).performClick()

        // Select Portuguese
        composeTestRule.onNodeWithText("Português (Brasil)").performClick()

        // For now, let's just check if the language selector shows the new language
        composeTestRule.onNodeWithText("Português (Brasil)").assertIsDisplayed()
    }

    @Test
    fun scenario7_defaultThemeIsAuto() {
        val autoLabel = composeTestRule.activity.getString(R.string.settings_theme_auto)
        composeTestRule.onNodeWithText(autoLabel).assertIsSelected()
    }
}
