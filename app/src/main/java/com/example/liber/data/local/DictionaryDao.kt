package com.example.liber.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.DictionaryEntry
import com.example.liber.data.model.DictionaryLookupHistory
import com.example.liber.data.model.DictionarySense
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionaries ORDER BY isEnabled DESC, priority ASC, displayName ASC")
    fun getAllDictionaries(): Flow<List<Dictionary>>

    @Query("SELECT * FROM dictionaries WHERE id = :id LIMIT 1")
    suspend fun getDictionaryById(id: String): Dictionary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDictionary(dictionary: Dictionary)

    @Query("DELETE FROM dictionaries WHERE id = :id")
    suspend fun deleteDictionary(id: String)

    @Query("UPDATE dictionaries SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDictionaryEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("UPDATE dictionaries SET localAlias = :localAlias, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDictionaryAlias(id: String, localAlias: String?, updatedAt: Long)

    @Query("UPDATE dictionaries SET priority = :priority, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDictionaryPriority(id: String, priority: Int, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<DictionaryEntry>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSenses(senses: List<DictionarySense>)

    @Query("DELETE FROM dictionary_entries WHERE dictionaryId = :dictionaryId")
    suspend fun clearEntriesForDictionary(dictionaryId: String)

    @Query(
        """
        SELECT e.*
        FROM dictionary_entries e
        INNER JOIN dictionaries d ON d.id = e.dictionaryId
        WHERE d.isEnabled = 1
          AND e.languageTag = :languageTag
          AND (
            e.normalizedHeadword LIKE :normalizedPrefix
            OR e.lemma LIKE :rawPrefix
          )
        ORDER BY d.priority ASC, e.headword ASC
        LIMIT :limit
        """
    )
    suspend fun searchEntries(
        languageTag: String,
        normalizedPrefix: String,
        rawPrefix: String,
        limit: Int,
    ): List<DictionaryEntry>

    @Transaction
    @Query("SELECT * FROM dictionary_entries WHERE id = :entryId LIMIT 1")
    suspend fun getEntryWithSenses(entryId: Long): DictionaryEntryWithSenses?

    @Transaction
    @Query(
        """
        SELECT *
        FROM dictionary_entries
        WHERE id IN (:entryIds)
        ORDER BY headword ASC
        """
    )
    suspend fun getEntriesWithSenses(entryIds: List<Long>): List<DictionaryEntryWithSenses>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLookupHistory(item: DictionaryLookupHistory)

    @Query("SELECT * FROM dictionary_lookup_history ORDER BY lookedUpAt DESC LIMIT :limit")
    fun getLookupHistory(limit: Int = 100): Flow<List<DictionaryLookupHistory>>

    @Query("DELETE FROM dictionary_lookup_history WHERE id = :id")
    suspend fun deleteLookupHistory(id: Long)

    @Query("DELETE FROM dictionary_lookup_history")
    suspend fun clearLookupHistory()
}
