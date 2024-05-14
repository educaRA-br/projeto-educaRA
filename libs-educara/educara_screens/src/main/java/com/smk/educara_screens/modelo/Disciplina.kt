package com.smk.educara_screens.modelo

import kotlinx.serialization.Serializable

@Serializable
data class DisciplinaModelo(
    val id: String,
    val nome: String,
    val detalhes: String
)