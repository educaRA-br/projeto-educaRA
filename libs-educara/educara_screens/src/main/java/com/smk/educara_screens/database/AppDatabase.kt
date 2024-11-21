package com.smk.educara_screens.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smk.educara_screens.database.dao.AulaDao
import com.smk.educara_screens.database.dao.ConteudoDao
import com.smk.educara_screens.database.dao.DisciplinaDao
import com.smk.educara_screens.modelo.AulaModelo
import com.smk.educara_screens.modelo.ConteudoModelo
import com.smk.educara_screens.modelo.DisciplinaModelo

@Database(entities = [AulaModelo::class, ConteudoModelo::class, DisciplinaModelo::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aulaDao(): AulaDao
    abstract fun conteudoDao(): ConteudoDao
    abstract fun disciplinaDao(): DisciplinaDao

    companion object {
        fun instancia(context: Context) : AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "educara.db"
            ).allowMainThreadQueries()
                .build()
        }
    }
}