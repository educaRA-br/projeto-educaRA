package com.smk.educara_screens.modelo

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "disciplinas")
data class DisciplinaModelo(
    @PrimaryKey val id: String,
    val nome: String,
    val detalhes: String
)