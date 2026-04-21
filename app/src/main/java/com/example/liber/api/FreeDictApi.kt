package com.example.liber.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface FreeDictApi {
    @GET("freedict-database.json")
    suspend fun getDatabase(): List<FreeDictDictionaryDto>
}

@Serializable
data class FreeDictDictionaryDto(
    val name: String? = null,
    val headwords: String? = null,
    val releases: List<FreeDictReleaseDto> = emptyList(),
)

@Serializable
data class FreeDictReleaseDto(
    @SerialName("URL")
    val url: String,
    val platform: String,
    val version: String,
    val size: String? = null,
    val date: String? = null,
)
