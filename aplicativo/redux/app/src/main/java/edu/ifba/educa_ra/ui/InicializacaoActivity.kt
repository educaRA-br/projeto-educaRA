@file:Suppress("DEPRECATION")

package edu.ifba.educa_ra.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.smk.educara_3d.VisualizadorActivity
import com.smk.educara_screens.ui.DataHolder
import com.smk.educara_screens.ui.SelecaoActivity
import edu.ifba.educa_ra.R
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

        val objetoSelecionado = DataHolder.objetoSelecionado

        if (objetoSelecionado != null) {
            this.onVisualizarObjeto(this)

        } else {
            informar("verificando")
            executarComAtraso(timerTask {
                startActivity(Intent(this@InicializacaoActivity, SelecaoActivity::class.java))
//                finish()
            })
        }
    }

    private fun onVisualizarObjeto(activity: Activity) {
        val visualizador = Intent(activity, VisualizadorActivity::class.java)

        visualizador.putExtra("object", DataHolder.objetoSelecionado?.nome)
        visualizador.putExtra("model", "${DataHolder.objetoSelecionado?.caminho}/${DataHolder.objetoSelecionado?.nome}.obj")
        visualizador.putExtra("texture", "${DataHolder.objetoSelecionado?.caminho}/${DataHolder.objetoSelecionado?.nome}.mtl")
        visualizador.putExtra("immersiveMode", "false")
        visualizador.putExtra("backgroundColor", "1 1 1 1.000")

        activity?.startActivity(visualizador)
        DataHolder.objetoSelecionado = null
    }

//    private fun onAlive(alive: Boolean) {
//        informar(if (alive) "serviços disponíveis" else "serviços indisponíveis")
//
//        if (alive) {
//            executarComAtraso(timerTask {
//                startActivity(Intent(this@InicializacaoActivity, SeletorActivity::class.java))
//                finish()
//            })
//        } else {
//            executarComAtraso(timerTask {
//                ErroActivity.exibirErro(this@InicializacaoActivity, "falha na inicialização")
//                finish()
//            })
//        }
//    }

    private fun executarComAtraso(rotina: TimerTask) {
        Timer().schedule(rotina, 2000)
    }

    private fun informar(mensagem: String) {
        holder.status.text = mensagem

        holder.status.invalidate()
        holder.status.requestLayout()
    }
}
