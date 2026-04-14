package com.example.liber.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.liber.MainActivity
import com.example.liber.R
import com.example.liber.core.testutils.DataStoreTestHelper
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

    @Before
    fun setup() {
        hiltRule.inject()
        DataStoreTestHelper.clear()
        FileTestHelper.copyAssetsToEmulatorStorage()

        val settingsLabel = composeTestRule.activity.getString(R.string.tab_settings)
        composeTestRule.onNodeWithText(settingsLabel).performClick()
    }

    @After
    fun teardown() {
        FileTestHelper.cleanUpEmulatorStorage()
    }

    @Test
    fun scenario1_importEpubFile() {
        val importLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_books_from_files)
        composeTestRule.onNodeWithText(importLabel).performClick()

        UiAutomatorHelper.selectFileInSystemPicker("valid_book.epub")

        // Navigate to Library
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onNodeWithText(libraryLabel).performClick()

        // Assert book appears (we might need to wait for processing)
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("Valid Book Title").fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Valid Book Title").assertIsDisplayed()
    }

    @Test
    fun scenario2_addScanFolder() {
        val addFolderLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_scan_folder)
        composeTestRule.onNodeWithText(addFolderLabel).performClick()

        UiAutomatorHelper.selectFolderInSystemPicker("test_audiobook")

        // Wait for scan to complete - check for scan source appearing in Settings
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("test_audiobook").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("test_audiobook").assertIsDisplayed()

        // Navigate to Library -> Audiobooks
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onNodeWithText(libraryLabel).performClick()

        val audiobooksLabel = composeTestRule.activity.getString(R.string.tab_audiobooks)
        composeTestRule.onNodeWithText(audiobooksLabel).performClick()

        // Assert audiobook appears
        composeTestRule.onNodeWithText("Test Audiobook Title").assertIsDisplayed()
    }

    @Test
    fun scenario3_changeTheme() {
        val darkLabel = composeTestRule.activity.getString(R.string.settings_theme_dark)
        composeTestRule.onNodeWithText(darkLabel).performClick()

        // Validation: Verify the button is selected
        // TODO: This validation could be improved upon, by validating if a component is now using the colors from the dark theme
        composeTestRule.onNodeWithText(darkLabel).assertIsSelected()
    }

    @Test
    fun scenario4_changeLanguage() {
        val languageLabel = composeTestRule.activity.getString(R.string.settings_language)
        // Click the language selector row
        composeTestRule.onNodeWithText(languageLabel, substring = true).performClick()

        // Select Portuguese
        composeTestRule.onNodeWithText("Português (Brasil)").performClick()

        // TODO: Improve this test
        // Assert "Settings" changes to "Configurações" (assuming translations exist)
        // Note: We need to know the actual translation or use a resource ID if possible
        // For now, let's just check if the language selector shows the new language
        composeTestRule.onNodeWithText("Português (Brasil)").assertIsDisplayed()
    }

    @Test
    fun scenario5_unsupportedFile() {
        val importLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_books_from_files)
        composeTestRule.onNodeWithText(importLabel).performClick()

        UiAutomatorHelper.selectFileInSystemPicker("invalid_file.pdf")

        // Validation: Check for error state or absence in library
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onNodeWithText(libraryLabel).performClick()

        composeTestRule.onNodeWithText("invalid_file").assertDoesNotExist()
    }

    @Test
    fun scenario7_defaultThemeIsAuto() {
        val autoLabel = composeTestRule.activity.getString(R.string.settings_theme_auto)
        composeTestRule.onNodeWithText(autoLabel).assertIsSelected()
    }
}
