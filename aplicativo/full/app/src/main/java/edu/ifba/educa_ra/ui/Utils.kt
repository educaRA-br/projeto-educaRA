package edu.ifba.educa_ra.ui

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.smk.educara_screens.modelo.ObjetoSelecionado

fun exibirInfoObjetoSelecionado(contexto: Context, objetoSelecionado: ObjetoSelecionado) {
    val builder = AlertDialog.Builder(contexto)
    builder.setTitle(objetoSelecionado.nome)
    builder.setMessage(objetoSelecionado.detalhes)
        .setCancelable(false)
        .setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
        }
    val alert = builder.create()
    alert.show()
}

fun avisar(contexto: Context, mensagem: String) {
    val builder = AlertDialog.Builder(contexto)
    builder.setMessage(mensagem)
        .setCancelable(false)
        .setPositiveButton("Ok") { dialog, _ ->
            dialog.dismiss()
        }
    val alert = builder.create()
    alert.show()
}

fun confirmar(contexto: Context, mensagem: String, onSim: View.OnClickListener, onNao: View.OnClickListener) {
    decidir(contexto, mensagem, "sim", onSim, "não", onNao)
}

fun decidir(
    contexto: Context,
    mensagem: String,
    textoDecisao: String,
    decisao: View.OnClickListener,
    textoContrario: String,
    doContrario: View.OnClickListener,
) {
    val builder = AlertDialog.Builder(contexto)
    builder.setMessage(mensagem)
        .setCancelable(false)
        .setPositiveButton(textoDecisao) { dialog, _ ->
            decisao.onClick(null)
            dialog.dismiss()
        }
        .setNegativeButton(textoContrario) { dialog, _ ->
            doContrario.onClick(null)
            dialog.dismiss()
        }
    val alert = builder.create()
    alert.show()
}