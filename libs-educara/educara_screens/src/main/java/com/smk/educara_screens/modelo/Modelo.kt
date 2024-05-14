package com.smk.educara_screens.modelo

data class Conteudo(val id: String, val nome: String, val detalhes: String, val objeto: String, var bytesObjeto: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Conteudo
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
data class Aula(val id: String, val nome: String, val detalhes: String, val conteudos: List<Conteudo>)
data class Disciplina(val id : String, val nome: String, val detalhes: String, val aulas: List<Aula>)

data class ObjetoSelecionado(var nome: String, var detalhes: String, var caminho: String)
val objetoSelecionado = ObjetoSelecionado("", "", "")