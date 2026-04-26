package com.example.liber.feature.reader

import androidx.appcompat.app.AppCompatDelegate
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
import com.example.liber.core.testutils.FileTestHelper
import com.example.liber.core.testutils.UiAutomatorHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ReaderE2ETest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    @Before
    fun setup() {
        hiltRule.inject()

        // Force English at the start of every test
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }

        DataStoreTestHelper.clear()
        FileTestHelper.copyAssetsToEmulatorStorage()
    }

    @After
    fun teardown() {
        // FileTestHelper.cleanUpEmulatorStorage()
    }

    @Test
    fun importAndOpenJekyllAndHyde_displaysCorrectTitle() {
        // 1. Go to Settings and import Jekyll & Hyde
        val settingsLabel = composeTestRule.activity.getString(R.string.tab_settings)
        composeTestRule.onAllNodesWithText(settingsLabel).onLast().performClick()

        val importLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_books_from_files)
        composeTestRule.onNodeWithText(importLabel).performClick()

        UiAutomatorHelper.selectFileInSystemPicker("Jekyll.epub")

        // 2. Go to Library
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onAllNodesWithText(libraryLabel).onLast().performClick()

        // 3. Click on the book via its cover content description
        val bookTitle = "The strange case of Dr. Jekyll and Mr. Hyde"
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithContentDescription(bookTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(bookTitle, substring = true).performClick()

        // 4. Verify Reader opens (TopBar shows full title)
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithText(bookTitle, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun importAndOpenMobyDick_displaysCorrectTitle() {
        // 1. Go to Settings and import Moby Dick
        val settingsLabel = composeTestRule.activity.getString(R.string.tab_settings)
        composeTestRule.onAllNodesWithText(settingsLabel).onLast().performClick()

        val importLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_books_from_files)
        composeTestRule.onNodeWithText(importLabel).performClick()

        UiAutomatorHelper.selectFileInSystemPicker("MobyDick.epub")

        // 2. Go to Library
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onAllNodesWithText(libraryLabel).onLast().performClick()

        // 3. Click on the book
        val bookTitle = "Moby Dick; Or, The Whale"
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithContentDescription(bookTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(bookTitle, substring = true).performClick()

        // 4. Verify Reader opens
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithText(bookTitle, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun importAndOpenCrimeAndPunishment_displaysCorrectTitle() {
        // 1. Go to Settings and import Crime and Punishment
        val settingsLabel = composeTestRule.activity.getString(R.string.tab_settings)
        composeTestRule.onAllNodesWithText(settingsLabel).onLast().performClick()

        val importLabel =
            composeTestRule.activity.getString(R.string.settings_action_add_books_from_files)
        composeTestRule.onNodeWithText(importLabel).performClick()

        UiAutomatorHelper.selectFileInSystemPicker("Crime.epub")

        // 2. Go to Library
        val libraryLabel = composeTestRule.activity.getString(R.string.tab_library)
        composeTestRule.onAllNodesWithText(libraryLabel).onLast().performClick()

        // 3. Click on the book
        val bookTitle = "Crime and Punishment"
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithContentDescription(bookTitle, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(bookTitle, substring = true).performClick()

        // 4. Verify Reader opens
        composeTestRule.waitUntil(30000) {
            composeTestRule.onAllNodesWithText(bookTitle, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
