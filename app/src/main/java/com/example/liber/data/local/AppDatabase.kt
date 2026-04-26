package com.example.liber.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.Book
import com.example.liber.data.model.BookCollection
import com.example.liber.data.model.Bookmark
import com.example.liber.data.model.Collection
import com.example.liber.data.model.Dictionary
import com.example.liber.data.model.DictionaryEntry
import com.example.liber.data.model.DictionaryLookupHistory
import com.example.liber.data.model.DictionarySense
import com.example.liber.data.model.ReadingSession
import com.example.liber.data.model.ScanSource
import com.example.liber.data.model.WordLemma

@Database(
    entities = [
        Book::class,
        Annotation::class,
        Bookmark::class,
        Collection::class,
        BookCollection::class,
        ScanSource::class,
        Dictionary::class,
        DictionaryEntry::class,
        DictionarySense::class,
        DictionaryLookupHistory::class,
        ReadingSession::class,
        WordLemma::class,
    ],
    version = 16,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun collectionDao(): CollectionDao
    abstract fun scanSourceDao(): ScanSourceDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun wordLemmaDao(): WordLemmaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastOpenedAt INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN wantToRead INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE books ADD COLUMN readingProgress INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN lastLocator TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `annotations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `locator` TEXT NOT NULL,
                        `text` TEXT,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_bookId` ON `annotations` (`bookId`)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN mediaType TEXT")
                db.execSQL("ALTER TABLE books ADD COLUMN durationMillis INTEGER")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN contentId TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scan_sources` (
                        `treeUri` TEXT NOT NULL PRIMARY KEY,
                        `displayName` TEXT NOT NULL,
                        `lastScannedAt` INTEGER,
                        `bookCount` INTEGER NOT NULL DEFAULT 0,
                        `addedAt` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_collections` (
                        `collectionId` INTEGER NOT NULL,
                        `bookId` TEXT NOT NULL,
                        PRIMARY KEY(`collectionId`, `bookId`),
                        FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_collections_collectionId` ON `book_collections` (`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_collections_bookId` ON `book_collections` (`bookId`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `locator` TEXT NOT NULL,
                        `chapter` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_bookId` ON `bookmarks` (`bookId`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN narrator TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN tracksJson TEXT")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE annotations ADD COLUMN endLocator TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionaries` (
                        `id` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `localAlias` TEXT,
                        `sourceLanguageTag` TEXT NOT NULL,
                        `targetLanguageTag` TEXT,
                        `dictionaryType` TEXT NOT NULL,
                        `packageFormat` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `localFilePath` TEXT,
                        `remoteUrl` TEXT,
                        `installSizeBytes` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `priority` INTEGER NOT NULL,
                        `installedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dictionaryId` TEXT NOT NULL,
                        `headword` TEXT NOT NULL,
                        `normalizedHeadword` TEXT NOT NULL,
                        `lemma` TEXT,
                        `languageTag` TEXT NOT NULL,
                        FOREIGN KEY(`dictionaryId`) REFERENCES `dictionaries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_senses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entryId` INTEGER NOT NULL,
                        `partOfSpeech` TEXT,
                        `definition` TEXT NOT NULL,
                        `example` TEXT,
                        FOREIGN KEY(`entryId`) REFERENCES `dictionary_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dictionary_lookup_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        `entryId` INTEGER,
                        `dictionaryId` TEXT,
                        `sourceBookId` TEXT,
                        `lookedUpAt` INTEGER NOT NULL,
                        FOREIGN KEY(`entryId`) REFERENCES `dictionary_entries`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`dictionaryId`) REFERENCES `dictionaries`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_entries_dictionaryId` ON `dictionary_entries` (`dictionaryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_entries_normalizedHeadword` ON `dictionary_entries` (`normalizedHeadword`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_entries_languageTag` ON `dictionary_entries` (`languageTag`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_senses_entryId` ON `dictionary_senses` (`entryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_lookup_history_entryId` ON `dictionary_lookup_history` (`entryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_lookup_history_dictionaryId` ON `dictionary_lookup_history` (`dictionaryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dictionary_lookup_history_lookedUpAt` ON `dictionary_lookup_history` (`lookedUpAt`)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookId` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_bookId` ON `reading_sessions` (`bookId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_startedAt` ON `reading_sessions` (`startedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_sessions_endedAt` ON `reading_sessions` (`endedAt`)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN language TEXT")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_lemmas` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `languageTag` TEXT NOT NULL,
                        `inflection` TEXT NOT NULL,
                        `lemma` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_lemmas_inflection` ON `word_lemmas` (`inflection`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_lemmas_languageTag` ON `word_lemmas` (`languageTag`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "liber_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
