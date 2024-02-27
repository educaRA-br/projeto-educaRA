@file:Suppress("DEPRECATION")

package edu.ifba.educa_ra.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import edu.ifba.educa_ra.R
import edu.ifba.educa_ra.api.IsAlive
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask

class InicializacaoHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val atividade: View
    val status: TextView

    init {
        status = view.findViewById(R.id.status_inicializacao)
        atividade = view
    }
}

class InicializacaoActivity : AppCompatActivity() {

    private lateinit var holder: InicializacaoHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = layoutInflater.inflate(R.layout.activity_inicializacao, null)
        holder = InicializacaoHolder(view)

        setContentView(view)
    }

    override fun onStart() {
        super.onStart()

        informar("verificando")
        executarComAtraso(timerTask { IsAlive(::onAlive).execute() })
    }

    private fun onAlive(alive: Boolean) {
        informar(if (alive) "serviços disponíveis" else "serviços indisponíveis")

        if (alive) {
            executarComAtraso(timerTask {
                startActivity(Intent(this@InicializacaoActivity, SeletorActivity::class.java))
                finish()
            })
        } else {
            executarComAtraso(timerTask {
                ErroActivity.exibirErro(this@InicializacaoActivity, "falha na inicialização")
                finish()
            })
        }
    }

    private fun executarComAtraso(rotina: TimerTask) {
        Timer().schedule(rotina, 2000)
    }

    private fun informar(mensagem: String) {
        holder.status.text = mensagem

        holder.status.invalidate()
        holder.status.requestLayout()
    }
}
