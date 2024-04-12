package edu.ifba.educa_ra.modelo

import kotlinx.serialization.Serializable

@Serializable
data class AulaModelo(
    val id: String,
    val nome: String,
    val detalhes: String,
    val disciplina_id: String
)