package edu.ifba.educa_ra.modelo

import kotlinx.serialization.Serializable

@Serializable
data class DisciplinaModelo(
    val id: String,
    val nome: String,
    val detalhes: String
)