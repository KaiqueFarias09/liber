package com.example.liber.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesSearchApi {
    @GET("search")
    suspend fun searchAudiobooks(
        @Query("term") term: String,
        @Query("entity") entity: String = "audiobook",
        @Query("limit") limit: Int = 10
    ): ITunesSearchResponse
}

@Serializable
data class ITunesSearchResponse(
    val resultCount: Int,
    val results: List<ITunesSearchResult>
)

@Serializable
data class ITunesSearchResult(
    val wrapperType: String? = null,
    val collectionName: String? = null,
    val artistName: String? = null,
    val artworkUrl100: String? = null,
) {
    val highResArtworkUrl: String?
        get() = artworkUrl100?.replace("100x100bb.jpg", "1000x1000bb.jpg")
}
