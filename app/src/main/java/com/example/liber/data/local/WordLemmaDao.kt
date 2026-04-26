package com.example.liber.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.liber.data.model.WordLemma
import kotlinx.coroutines.flow.Flow

@Dao
interface WordLemmaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLemmas(lemmas: List<WordLemma>)

    @Query("SELECT lemma FROM word_lemmas WHERE inflection = :inflection AND languageTag = :languageTag")
    suspend fun findLemmas(inflection: String, languageTag: String): List<String>

    @Query("SELECT COUNT(*) FROM word_lemmas WHERE languageTag = :languageTag")
    suspend fun getCountForLanguage(languageTag: String): Int

    @Query("DELETE FROM word_lemmas WHERE languageTag = :languageTag")
    suspend fun deleteLemmasForLanguage(languageTag: String)

    @Query("SELECT DISTINCT languageTag FROM word_lemmas")
    fun getLanguagesWithLemmas(): Flow<List<String>>
}
