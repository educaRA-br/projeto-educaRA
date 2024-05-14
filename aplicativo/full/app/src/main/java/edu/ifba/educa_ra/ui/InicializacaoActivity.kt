@file:Suppress("DEPRECATION")

package edu.ifba.educa_ra.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.ar.core.ArCoreApk
import com.smk.educara_3d.VisualizadorActivity
import com.smk.educara_arcore.VisualizadorARCoreActivity
import com.smk.educara_screens.ui.DataHolder
import com.smk.educara_screens.ui.DataHolder.Companion.objetoSelecionado
import com.smk.educara_screens.ui.SelecaoActivity
import edu.ifba.educa_ra.R
import edu.ifba.educa_ra.databinding.ActivityInicializacaoBinding
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask
import kotlin.reflect.KFunction1

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
    private lateinit var binding: ActivityInicializacaoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicializacaoBinding.inflate(layoutInflater)
        val view = binding.root
        holder = InicializacaoHolder(view)

        setContentView(view)
    }

    @RequiresApi(Build.VERSION_CODES.CUPCAKE)
    override fun onStart() {
        super.onStart()

        val objetoSelecionado = DataHolder.objetoSelecionado

        val bundle = Bundle().apply {
            putString("nome", objetoSelecionado?.nome)
            putString("detalhes", objetoSelecionado?.detalhes)
            putString("caminho", objetoSelecionado?.caminho)
        }

        if (objetoSelecionado != null) {
            val disponibilidade = ArCoreApk.getInstance().checkAvailability(this)
        if (disponibilidade.isSupported) {
            if (disponibilidade == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                val visualizador = Intent(this, VisualizadorARCoreActivity::class.java).apply {
                    putExtras(bundle)
                }
                startActivity(visualizador)
            } else if (disponibilidade == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED ||
                disponibilidade == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD) {
                decidir(this, "Você precisa instalar ou atualizar o ARCore ou pode visualizar\n" +
                        "diretamente caso o seu dispositivo não seja compatível.\n" +
                        "O que deseja fazer?",
                    "visualizar", { this.onVisualizarObjeto(this) }, "instalar",
                    { this.onInstalarARCore(this) })
            }
        } else { // Unsupported or unknown.
            ErroActivity.exibirErro(this, "seu dispositivo não suporta o ARCore")
        }
            DataHolder.objetoSelecionado = null
        } else {
            informar("verificando")
            executarComAtraso(timerTask { VerificarARCore(this@InicializacaoActivity, ::informarSobreARCore).execute() })
        }
    }

    private fun onInstalarARCore(activity: Activity) {
        ArCoreApk.getInstance().requestInstall(activity, true)
    }

//    private fun onAlive(alive: Boolean) {
//        informar(if (alive) "serviços disponíveis" else "serviços indisponíveis")
//
//        if (alive) {
//            executarComAtraso(timerTask { VerificarARCore(this@InicializacaoActivity, ::informarSobreARCore).execute() })
//        } else {
//            executarComAtraso(timerTask {
//                ErroActivity.exibirErro(this@InicializacaoActivity, "falha na inicialização")
//                finish()
//            })
//        }
//    }

    private fun informarSobreARCore(mensagem: String) {
        informar(mensagem)

        executarComAtraso(timerTask {
            startActivity(Intent(this@InicializacaoActivity, SelecaoActivity::class.java))
//            finish()
        })
    }

    private fun executarComAtraso(rotina: TimerTask) {
        Timer().schedule(rotina, 2000)
    }

    private fun informar(mensagem: String) {
        holder.status.text = mensagem

        holder.status.invalidate()
        holder.status.requestLayout()
    }

    private fun onVisualizarObjeto(activity: Activity) {
        val visualizador = Intent(activity, VisualizadorActivity::class.java)

        visualizador.putExtra("object", objetoSelecionado?.nome)
        visualizador.putExtra("model", "${objetoSelecionado?.caminho}/${objetoSelecionado?.nome}.obj")
        visualizador.putExtra("texture", "${objetoSelecionado?.caminho}/${objetoSelecionado?.nome}.mtl")
        visualizador.putExtra("immersiveMode", "false")
        visualizador.putExtra("backgroundColor", "1 1 1 1.000")

        activity?.startActivity(visualizador)
    }
}

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
class VerificarARCore(private val contexto: Context, private val onInformacao: KFunction1<String, Unit>) :
    AsyncTask<Void, Void, String>() {
    override fun doInBackground(vararg params: Void?): String {
        var mensagem = "não foi possível verificar o ARCore"

        val disponibilidade = ArCoreApk.getInstance().checkAvailability(contexto)
        if (disponibilidade == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED || disponibilidade == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD) {
            mensagem = "ARCore precisa ser instalado/atualizado"
        } else if (disponibilidade == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            mensagem = "ARCore instalado"
        } else if (disponibilidade == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            mensagem = "dispositivo não suporta o ARCore"
        }

        return mensagem
    }

    @RequiresApi(Build.VERSION_CODES.CUPCAKE)
    override fun onPostExecute(mensagem: String) {
        super.onPostExecute(mensagem)

        onInformacao(mensagem)
    }


}
