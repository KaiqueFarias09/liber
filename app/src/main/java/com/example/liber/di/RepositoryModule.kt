package com.example.liber.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.liber.api.FreeDictApi
import com.example.liber.core.logging.AppLogger
import com.example.liber.data.local.BookDao
import com.example.liber.data.local.CollectionDao
import com.example.liber.data.local.DictionaryDao
import com.example.liber.data.local.ReadingSessionDao
import com.example.liber.data.local.ScanSourceDao
import com.example.liber.data.local.WordLemmaDao
import com.example.liber.data.repository.BookImporter
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.CollectionRepository
import com.example.liber.data.repository.DictionaryRepository
import com.example.liber.data.repository.ReadingInsightsRepository
import com.example.liber.data.repository.ScanSourceRepository
import com.example.liber.data.repository.StarDictIndexer
import com.example.liber.data.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFreeDictApi(): FreeDictApi {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl("https://freedict.org/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FreeDictApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao,
        appLogger: AppLogger,
    ): BookRepository = BookRepository(bookDao, appLogger)

    @Provides
    @Singleton
    fun provideCollectionRepository(
        collectionDao: CollectionDao,
        appLogger: AppLogger,
    ): CollectionRepository = CollectionRepository(collectionDao, appLogger)

    @Provides
    @Singleton
    fun provideScanSourceRepository(
        scanSourceDao: ScanSourceDao,
        appLogger: AppLogger,
    ): ScanSourceRepository = ScanSourceRepository(scanSourceDao, appLogger)

    @Provides
    @Singleton
    fun provideBookImporter(
        application: Application,
        appLogger: AppLogger,
    ): BookImporter = BookImporter(application, appLogger)

    @Provides
    @Singleton
    fun provideDictionaryRepository(
        dictionaryDao: DictionaryDao,
        wordLemmaDao: WordLemmaDao,
        freeDictApi: FreeDictApi,
        starDictIndexer: StarDictIndexer,
        application: Application,
        appLogger: AppLogger,
    ): DictionaryRepository =
        DictionaryRepository(
            dictionaryDao,
            wordLemmaDao,
            freeDictApi,
            starDictIndexer,
            application,
            appLogger
        )

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>,
        appLogger: AppLogger,
    ): UserPreferencesRepository = UserPreferencesRepository(dataStore, appLogger)

    @Provides
    @Singleton
    fun provideReadingInsightsRepository(
        readingSessionDao: ReadingSessionDao,
        appLogger: AppLogger,
    ): ReadingInsightsRepository = ReadingInsightsRepository(readingSessionDao, appLogger)
}
