@file:Suppress("DEPRECATION")

package edu.ifba.educa_ra.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.sceneform.VisualizadorARCoreActivity
import edu.ifba.educa_ra.R
import edu.ifba.educa_ra.api.GetConteudos
import edu.ifba.educa_ra.api.GetObjeto
import edu.ifba.educa_ra.databinding.FragmentConteudosBinding
import edu.ifba.educa_ra.modelo.Conteudo
import edu.ifba.educa_ra.modelo.objetoSelecionado
import edu.ifba.educa_ra.visualizador.VisualizadorActivity
import java.io.File


class ConteudosHolder(view: View) : RecyclerView.ViewHolder(view) {
    val card: CardView
    val nomeConteudo: TextView
    val detalhesConteudo: TextView
    val downloadObjeto: ImageButton
    val removerObjeto: ImageButton
    val verObjetos: ImageButton

    init {
        card = view.findViewById(R.id.card_conteudo) as CardView
        nomeConteudo = view.findViewById(R.id.nome_conteudo)
        detalhesConteudo = view.findViewById(R.id.detalhes_conteudo)
        downloadObjeto = view.findViewById(R.id.download_objeto)
        removerObjeto = view.findViewById(R.id.remover_objeto)
        verObjetos = view.findViewById(R.id.ver_objetos)
    }
}

class ConteudosAdapter() :
    RecyclerView.Adapter<ConteudosHolder>() {

    private lateinit var holder: ConteudosHolder

    private lateinit var contexto: Context
    private lateinit var progresso: ProgressBar
    private lateinit var conteudos: List<Conteudo>

    constructor(contexto: Context, progresso: ProgressBar, conteudos: List<Conteudo>) : this() {
        this.contexto = contexto
        this.progresso = progresso
        this.conteudos = conteudos
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConteudosHolder {
        val cardConteudo =
            LayoutInflater.from(parent.context).inflate(R.layout.card_conteudo, parent, false)

        return ConteudosHolder(cardConteudo).also { holder = it }
    }

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: ConteudosHolder, position: Int) {
        // TODO congelar todo o fragmento (ou app) enquanto o usuario escolhe o objeto
        val conteudo = conteudos[position]

        indicarConteudoDisponivel(holder.card, conteudo)

        holder.nomeConteudo.text = conteudo.nome
        holder.detalhesConteudo.text = conteudo.detalhes
        holder.downloadObjeto.setOnClickListener {
            confirmar(this.contexto,
                "Caso já tenha realizado o download, esta opção irá apagar os arquivos anteriores. Confirma?",
                {
                    progresso.visibility = ProgressBar.VISIBLE
                    GetObjeto(conteudo, contexto.filesDir, ::onProgresso, ::onDownloadFinalizado).execute()
                }, {})
        }
        holder.removerObjeto.setOnClickListener {
            confirmar(this.contexto,
                "Esta opção irá apagar os arquivos deste conteúdo definitivamente. Confirma?",
                {
                    val destino = "${contexto.filesDir.absolutePath}/objeto.${conteudo.id}"

                    val dir = File(destino)
                    if (dir.exists()) {
                        dir.deleteRecursively()
                    }

                    notifyDataSetChanged()
                }, {})}

        (View.OnClickListener{
            objetoSelecionado.nome = conteudo.nome
            objetoSelecionado.detalhes = conteudo.detalhes

            progresso.visibility = ProgressBar.VISIBLE
            GetObjeto(conteudo, contexto.filesDir, ::onProgresso, ::onObjeto).execute()
        }).also { holder.card.setOnClickListener(it)}.also {
            holder.verObjetos.setOnClickListener(it)
        }
    }

    private fun onProgresso(passo: Int) {
        progresso.progress = passo * 20
    }

    private fun indicarConteudoDisponivel(card: CardView, conteudo: Conteudo) {
        val arquivos = File("${contexto.filesDir.absolutePath}/objeto.${conteudo.id}")

        card.setCardBackgroundColor(
            if (arquivos.isDirectory && arquivos.exists())
                ContextCompat.getColor(contexto, R.color.card_conteudo_disponivel)
            else
                ContextCompat.getColor(contexto, R.color.card_disciplina)
        )
    }

    private fun onDownloadFinalizado(caminho: String) {
        notifyDataSetChanged()
        progresso.visibility = ProgressBar.INVISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApi")
    private fun onObjeto(caminho: String) {
        objetoSelecionado.caminho = caminho

        val disponibilidade = ArCoreApk.getInstance().checkAvailability(this.contexto)
        val activity = getActivity(this.contexto)
        if (disponibilidade.isSupported) {
            if (disponibilidade == Availability.SUPPORTED_INSTALLED) {
                val visualizador = Intent(activity, VisualizadorARCoreActivity::class.java)
                activity?.startActivity(visualizador)
            } else if (disponibilidade == Availability.SUPPORTED_NOT_INSTALLED ||
                disponibilidade == Availability.SUPPORTED_APK_TOO_OLD) {
                decidir(contexto, "Você precisa instalar ou atualizar o ARCore ou pode visualizar\n" +
                        "diretamente caso o seu dispositivo não seja compatível.\n" +
                        "O que deseja fazer?",
                    "visualizar", { this.onVisualizarObjeto(activity!!) }, "instalar",
                 { this.onInstalarARCore(activity!!) })
            }
        } else { // Unsupported or unknown.
            ErroActivity.exibirErro(this.contexto, "seu dispositivo não suporta o ARCore")
        }

        onDownloadFinalizado(caminho)
    }

    private fun onInstalarARCore(activity: Activity) {
        ArCoreApk.getInstance().requestInstall(activity, true)
    }

    private fun onVisualizarObjeto(activity: Activity) {
        val visualizador = Intent(activity, VisualizadorActivity::class.java)

        visualizador.putExtra("object", objetoSelecionado.nome)
        visualizador.putExtra("model", "${objetoSelecionado.caminho}/${objetoSelecionado.nome}.obj")
        visualizador.putExtra("texture", "${objetoSelecionado.caminho}/${objetoSelecionado.nome}.mtl")
        visualizador.putExtra("immersiveMode", "false")
        visualizador.putExtra("backgroundColor", "1 1 1 1.000")

        activity?.startActivity(visualizador)
    }

    override fun getItemCount(): Int {
        return conteudos.count()
    }

}

class ConteudosFragment : Fragment() {
    private var _binding: FragmentConteudosBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentConteudosBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = LinearLayoutManager(this.context)
        binding.conteudos.layoutManager = layout

        val idAula = this.arguments?.getString("idAula")
        GetConteudos(idAula!!, ::onConteudos).execute()
    }

    private fun onConteudos(conteudos: List<Conteudo>) {
        if (conteudos.isEmpty()) {
            ErroActivity.exibirErro(this.requireActivity(), "conteúdos não encontrados")
        } else {
            val adapter = ConteudosAdapter(this.requireContext(), binding.progresso, conteudos)
            binding.conteudos.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}