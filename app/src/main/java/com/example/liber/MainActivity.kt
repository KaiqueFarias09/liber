package com.example.liber

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.liber.ui.LiberApp
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.reader.AnnotationRequest
import com.example.liber.ui.theme.LiberTheme

class MainActivity : FragmentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LiberTheme {
                LiberApp(viewModel = viewModel)
            }
        }
    }

    /**
     * Intercept floating ActionModes (triggered by long-press text selection in the EPUB WebView)
     * to inject "Highlight" and "Add Note" items into the system selection menu.
     */
    override fun onWindowStartingActionMode(
        callback: ActionMode.Callback?,
        type: Int,
    ): ActionMode? {
        // TYPE_FLOATING is the floating text-selection toolbar used by WebView
        if (type == ActionMode.TYPE_FLOATING && findWebView(window.decorView) != null) {
            return super.onWindowStartingActionMode(
                AnnotationActionModeCallback(callback),
                type,
            )
        }
        return super.onWindowStartingActionMode(callback, type)
    }

    // ── Inner class ───────────────────────────────────────────────────────────

    private inner class AnnotationActionModeCallback(
        private val wrapped: ActionMode.Callback?,
    ) : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            wrapped?.onCreateActionMode(mode, menu)
            menu.add(Menu.NONE, ID_HIGHLIGHT, ORDER_HIGHLIGHT, getString(R.string.action_highlight))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            menu.add(Menu.NONE, ID_ADD_NOTE, ORDER_ADD_NOTE, getString(R.string.action_add_note))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
            wrapped?.onPrepareActionMode(mode, menu) ?: false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                ID_HIGHLIGHT -> {
                    captureSelectionAndPost(mode) { text ->
                        viewModel.requestAnnotation(AnnotationRequest.Highlight(text))
                    }
                    true
                }
                ID_ADD_NOTE -> {
                    captureSelectionAndPost(mode) { text ->
                        viewModel.requestAnnotation(AnnotationRequest.Note(text))
                    }
                    true
                }
                else -> wrapped?.onActionItemClicked(mode, item) ?: false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            wrapped?.onDestroyActionMode(mode)
        }

        /**
         * Reads `window.getSelection().toString()` from the Readium WebView — the
         * [ActionMode] is finished INSIDE the JS callback so the selection is still
         * live when the script runs.
         */
        private fun captureSelectionAndPost(
            mode: ActionMode,
            onResult: (String?) -> Unit,
        ) {
            val webView = findWebView(window.decorView)
            if (webView != null) {
                webView.evaluateJavascript("window.getSelection().toString()") { raw ->
                    // raw is a JSON string — strip the surrounding quotes
                    val text = raw?.removeSurrounding("\"")?.takeIf { it.isNotBlank() }
                    onResult(text)
                    mode.finish()
                }
            } else {
                onResult(null)
                mode.finish()
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Recursively finds the first [WebView] in the view hierarchy, or null. */
    private fun findWebView(view: View?): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWebView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    companion object {
        private const val ID_HIGHLIGHT = 9_001
        private const val ID_ADD_NOTE  = 9_002
        private const val ORDER_HIGHLIGHT = 100
        private const val ORDER_ADD_NOTE  = 101
    }
}
