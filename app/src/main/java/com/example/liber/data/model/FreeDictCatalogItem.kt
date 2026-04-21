package com.example.liber.data.model

data class FreeDictCatalogItem(
    val code: String,
    val sourceLanguageTag: String,
    val targetLanguageTag: String,
    val version: String,
    val headwords: Int?,
    val stardictUrl: String,
    val stardictSizeBytes: Long?,
)
