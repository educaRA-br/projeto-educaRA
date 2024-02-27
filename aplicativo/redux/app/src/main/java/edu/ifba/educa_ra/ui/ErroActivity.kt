package edu.ifba.educa_ra.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.internal.ContextUtils
import edu.ifba.educa_ra.databinding.ActivityErroBinding


class ErroActivity() : AppCompatActivity() {
    private lateinit var binding: ActivityErroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityErroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var mensagem = "ocorreu um erro inesperado"
        if (intent.extras != null) {
            mensagem = intent.extras!!.getString("mensagem").toString()
        }

        binding.mensagem.text = mensagem
        binding.botaoVoltar.setOnClickListener{
            startActivity(Intent(this, InicializacaoActivity::class.java))

            finish()
        }
    }

    companion object {
        @SuppressLint("RestrictedApi")
        fun exibirErro(contexto: Context, mensagem: String)
        {
            try {
                val activity = ContextUtils.getActivity(contexto)
                val intencao = Intent(activity, ErroActivity::class.java)
                intencao.putExtra("mensagem", mensagem)

                activity!!.startActivity(intencao)
            } catch (e: Exception) {
                Log.e("exibirErro()", e.toString())
            }
        }
    }

}

