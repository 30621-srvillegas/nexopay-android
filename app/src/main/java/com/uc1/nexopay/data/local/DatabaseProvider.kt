package com.uc1.nexopay.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.uc1.nexopay.security.SecureStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseProvider {

    private const val DB_NAME = "nexopay.db"
    private const val PASSPHRASE_KEY = "db_passphrase_v1"
    private const val PASSPHRASE_BYTES = 32

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context, secureStore: SecureStore): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: build(context, secureStore).also { instance = it }
        }
    }

    private fun build(context: Context, secureStore: SecureStore): AppDatabase {
        System.loadLibrary("sqlcipher")

        val passphrase = secureStore.getOrCreateBytes(
            key = PASSPHRASE_KEY,
            size = PASSPHRASE_BYTES
        )
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration()
            .build()
    }
}
