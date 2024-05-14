package com.smk.educara_screens.modelo

import kotlinx.serialization.Serializable

@Serializable
data class ConteudoModelo(
    val id: String,
    val nome: String,
    val detalhes: String,
    val aula_id: String,
    val objeto: String
)