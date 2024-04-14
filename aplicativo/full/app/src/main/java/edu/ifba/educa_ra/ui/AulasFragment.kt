@file:Suppress("DEPRECATION")

package edu.ifba.educa_ra.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.ifba.educa_ra.R
import edu.ifba.educa_ra.api.GetAulas
import edu.ifba.educa_ra.api.supabase
import edu.ifba.educa_ra.databinding.FragmentAulasBinding
import edu.ifba.educa_ra.modelo.Aula
import edu.ifba.educa_ra.modelo.AulaModelo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale.filter

class AulasHolder(view: View) : RecyclerView.ViewHolder(view) {
    val card: CardView
    val nomeAula: TextView
    val detalhesAula: TextView
    val verConteudos: ImageButton

    init {
        card = view.findViewById(R.id.card_aula) as CardView
        nomeAula = view.findViewById(R.id.nome_aula)
        detalhesAula = view.findViewById(R.id.detalhes_aula)
        verConteudos = view.findViewById(R.id.ver_conteudos)
    }
}

class AulasAdapter() :
    RecyclerView.Adapter<AulasHolder>() {

    private lateinit var aulas: List<AulaModelo>
    private lateinit var navegador: NavController

    constructor(navegador: NavController, aulas: List<AulaModelo>) : this() {
        this.navegador = navegador
        this.aulas = aulas
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AulasHolder {
        val cardConteudo =
            LayoutInflater.from(parent.context).inflate(R.layout.card_aula, parent, false)

        return AulasHolder(cardConteudo)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: AulasHolder, position: Int) {
        val aula = aulas[position]

        holder.nomeAula.text = aula.nome
        holder.detalhesAula.text = aula.detalhes

        (View.OnClickListener{
            navegador.navigate(
                R.id.mostrar_conteudos,
                bundleOf("idAula" to aula.id)
            )}).also { holder.card.setOnClickListener(it) }.also { holder.verConteudos.setOnClickListener(it) }
    }

    override fun getItemCount(): Int {
        return aulas.count()
    }

}

class AulasFragment : Fragment() {

    private var _binding: FragmentAulasBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAulasBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = LinearLayoutManager(this.context)
        binding.aulas.layoutManager = layout

//        GetAulas(idDisciplina!!, ::onAulas).execute()
        val idDisciplina = this.arguments?.getString("idDisciplina")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val aulas = withContext(Dispatchers.IO) {
                    supabase.from("aula").select() {
                        filter {
                            if (idDisciplina != null) {
                                eq("disciplina_id", idDisciplina)
                            }
                        }
                    }

                }.decodeList<AulaModelo>()
                onAulas(aulas)
            } catch (e: Exception) {
                Log.e("error", e.toString())
            }
        }
    }

    private fun onAulas(aulas: List<AulaModelo>) {
        if (aulas.isEmpty()) {
            ErroActivity.exibirErro(this.requireActivity(), "aulas não encontradas")
        } else {
            val adapter = AulasAdapter(findNavController(), aulas)
            binding.aulas.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}