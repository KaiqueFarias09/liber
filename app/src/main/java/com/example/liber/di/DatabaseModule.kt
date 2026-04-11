package com.example.liber.di

import android.content.Context
import com.example.liber.data.local.AppDatabase
import com.example.liber.data.local.BookDao
import com.example.liber.data.local.CollectionDao
import com.example.liber.data.local.ScanSourceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getDatabase(context)

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideScanSourceDao(db: AppDatabase): ScanSourceDao = db.scanSourceDao()
}
