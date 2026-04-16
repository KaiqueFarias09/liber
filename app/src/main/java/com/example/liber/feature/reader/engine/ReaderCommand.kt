package com.example.liber.feature.reader.engine

/**
 * Integer command codes for DocView.doCommand().
 * Values match LVDOCVIEW_COMMANDS_START (100) + offsets from lvdocviewcmd.h,
 * plus the Android-specific codes from ReaderCommand.java.
 */
object ReaderCommand {
    const val DCMD_NONE = 0

    // ── Engine navigation (100–140) ──────────────────────────────────────────
    const val DCMD_BEGIN = 100
    const val DCMD_LINEUP = 101
    const val DCMD_PAGEUP = 102
    const val DCMD_PAGEDOWN = 103
    const val DCMD_LINEDOWN = 104
    const val DCMD_LINK_FORWARD = 105
    const val DCMD_LINK_BACK = 106
    const val DCMD_LINK_NEXT = 107
    const val DCMD_LINK_PREV = 108
    const val DCMD_LINK_GO = 109
    const val DCMD_END = 110
    const val DCMD_GO_POS = 111
    const val DCMD_GO_PAGE = 112
    const val DCMD_ZOOM_IN = 113
    const val DCMD_ZOOM_OUT = 114
    const val DCMD_TOGGLE_TEXT_FORMAT = 115
    const val DCMD_BOOKMARK_SAVE_N = 116
    const val DCMD_BOOKMARK_GO_N = 117
    const val DCMD_MOVE_BY_CHAPTER = 118  // param: -1 prev, +1 next
    const val DCMD_GO_SCROLL_POS = 119
    const val DCMD_TOGGLE_PAGE_SCROLL_VIEW = 120
    const val DCMD_LINK_FIRST = 121
    const val DCMD_ROTATE_BY = 122
    const val DCMD_ROTATE_SET = 123
    const val DCMD_SAVE_HISTORY = 124
    const val DCMD_SAVE_TO_CACHE = 125
    const val DCMD_SET_BASE_FONT_WEIGHT = 126
    const val DCMD_SCROLL_BY = 127       // param: pixels to scroll (scroll mode)
    const val DCMD_REQUEST_RENDER = 128
    const val DCMD_GO_PAGE_DONT_SAVE_HISTORY = 129
    const val DCMD_SET_INTERNAL_STYLES = 130

    // Selection
    const val DCMD_SELECT_FIRST_SENTENCE = 131
    const val DCMD_SELECT_NEXT_SENTENCE = 132
    const val DCMD_SELECT_PREV_SENTENCE = 133
    const val DCMD_SELECT_MOVE_LEFT_BOUND_BY_WORDS = 134
    const val DCMD_SELECT_MOVE_RIGHT_BOUND_BY_WORDS = 135

    const val DCMD_SET_TEXT_FORMAT = 136
    const val DCMD_SET_DOC_FONTS = 137
    const val DCMD_SET_REQUESTED_DOM_VERSION = 138
    const val DCMD_SET_RENDER_BLOCK_RENDERING_FLAGS = 139
    const val DCMD_SET_ROTATION_INFO_FOR_AA = 140
}
