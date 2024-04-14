@file:Suppress("DEPRECATION")

package edu.ifba.educa_ra.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.ar.core.ArCoreApk
import edu.ifba.educa_ra.R
import edu.ifba.educa_ra.api.IsAlive
import edu.ifba.educa_ra.databinding.ActivityInicializacaoBinding
import edu.ifba.educa_ra.modelo.Disciplina
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

    override fun onStart() {
        super.onStart()

        informar("verificando")
        executarComAtraso(timerTask { VerificarARCore(this@InicializacaoActivity, ::informarSobreARCore).execute() })
    }

    private fun onAlive(alive: Boolean) {
        informar(if (alive) "serviços disponíveis" else "serviços indisponíveis")

        if (alive) {
            executarComAtraso(timerTask { VerificarARCore(this@InicializacaoActivity, ::informarSobreARCore).execute() })
        } else {
            executarComAtraso(timerTask {
                ErroActivity.exibirErro(this@InicializacaoActivity, "falha na inicialização")
                finish()
            })
        }
    }

    private fun informarSobreARCore(mensagem: String) {
        informar(mensagem)

        executarComAtraso(timerTask {
            startActivity(Intent(this@InicializacaoActivity, SeletorActivity::class.java))
            finish()
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
}

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

    override fun onPostExecute(mensagem: String) {
        super.onPostExecute(mensagem)

        onInformacao(mensagem)
    }


}
