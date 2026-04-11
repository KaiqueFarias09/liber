package com.example.liber.data.model

sealed class ScanState {
    data object Idle : ScanState()
    data class Scanning(
        val folderName: String,
        val current: Int,
        val total: Int,
        val newlyAdded: Int,
    ) : ScanState()

    data class Finished(
        val folderName: String,
        val added: Int,
        val skipped: Int,
    ) : ScanState()

    data class Failed(val reason: String) : ScanState()
}
