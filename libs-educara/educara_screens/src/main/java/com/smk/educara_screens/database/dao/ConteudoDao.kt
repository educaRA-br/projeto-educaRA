package com.smk.educara_screens.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smk.educara_screens.modelo.ConteudoModelo

@Dao
interface ConteudoDao {

    @Query("SELECT * FROM conteudos")
    fun buscaTodos() : List<ConteudoModelo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun salva(vararg conteudo: ConteudoModelo)

    @Delete
    fun remove(conteudo: ConteudoModelo)

    @Query("SELECT * FROM conteudos WHERE id = :id")
    fun buscaPorId(id: Long) : ConteudoModelo?
}