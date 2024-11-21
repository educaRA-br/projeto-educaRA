package com.smk.educara_screens.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "conteudos")
data class ConteudoModelo(
    @PrimaryKey val id: String,
    val nome: String,
    val detalhes: String,
    val aula_id: String,
    val objeto: String
)