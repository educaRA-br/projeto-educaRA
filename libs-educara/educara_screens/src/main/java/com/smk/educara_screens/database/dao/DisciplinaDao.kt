package com.smk.educara_screens.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smk.educara_screens.modelo.DisciplinaModelo

@Dao
interface DisciplinaDao {

    @Query("SELECT * FROM disciplinas")
    fun buscaTodos() : List<DisciplinaModelo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun salva(vararg disciplina: DisciplinaModelo)

    @Delete
    fun remove(disciplina: DisciplinaModelo)

    @Query("SELECT * FROM disciplinas WHERE id = :id")
    fun buscaPorId(id: Long) : DisciplinaModelo?
}