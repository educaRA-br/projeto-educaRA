package com.smk.educara_screens.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smk.educara_screens.modelo.AulaModelo

@Dao
interface AulaDao {

    @Query("SELECT * FROM aulas")
    fun buscaTodos() : List<AulaModelo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun salva(vararg aula: AulaModelo)

    @Delete
    fun remove(aula: AulaModelo)

    @Query("SELECT * FROM aulas WHERE id = :id")
    fun buscaPorId(id: Long) : AulaModelo?
}