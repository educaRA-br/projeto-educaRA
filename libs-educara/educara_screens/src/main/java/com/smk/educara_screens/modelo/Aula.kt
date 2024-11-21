package com.smk.educara_screens.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "aulas")
data class AulaModelo(
    @PrimaryKey val id: String,
    val nome: String,
    val detalhes: String,
    val disciplina_id: String
)