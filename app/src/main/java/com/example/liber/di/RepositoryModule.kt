package com.example.liber.di

import android.app.Application
import com.example.liber.api.FreeDictApi
import com.example.liber.data.local.BookDao
import com.example.liber.data.local.CollectionDao
import com.example.liber.data.local.DictionaryDao
import com.example.liber.data.local.ScanSourceDao
import com.example.liber.data.repository.BookImporter
import com.example.liber.data.repository.BookRepository
import com.example.liber.data.repository.CollectionRepository
import com.example.liber.data.repository.DictionaryRepository
import com.example.liber.data.repository.ScanSourceRepository
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
    fun provideBookRepository(bookDao: BookDao): BookRepository =
        BookRepository(bookDao)

    @Provides
    @Singleton
    fun provideCollectionRepository(collectionDao: CollectionDao): CollectionRepository =
        CollectionRepository(collectionDao)

    @Provides
    @Singleton
    fun provideScanSourceRepository(scanSourceDao: ScanSourceDao): ScanSourceRepository =
        ScanSourceRepository(scanSourceDao)

    @Provides
    @Singleton
    fun provideBookImporter(application: Application): BookImporter =
        BookImporter(application)

    @Provides
    @Singleton
    fun provideDictionaryRepository(
        dictionaryDao: DictionaryDao,
        freeDictApi: FreeDictApi,
        application: Application,
    ): DictionaryRepository =
        DictionaryRepository(dictionaryDao, freeDictApi, application)

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(application: Application): UserPreferencesRepository =
        UserPreferencesRepository(application)
}
