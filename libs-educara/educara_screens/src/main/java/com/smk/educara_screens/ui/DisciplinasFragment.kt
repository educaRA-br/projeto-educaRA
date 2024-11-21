package com.smk.educara_screens.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smk.educara_screens.R
import com.smk.educara_screens.api.supabase
import com.smk.educara_screens.database.AppDatabase
import com.smk.educara_screens.database.dao.AulaDao
import com.smk.educara_screens.database.dao.DisciplinaDao
import com.smk.educara_screens.databinding.FragmentDisciplinasBinding
import com.smk.educara_screens.modelo.DisciplinaModelo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DisciplinaHolder(view: View) : RecyclerView.ViewHolder(view) {
    val card: CardView
    val nomeDisciplina: TextView
    val detalhesDisciplina: TextView
    val verAulas: ImageButton

    init {
        card = view.findViewById(R.id.card_disciplina) as CardView
        nomeDisciplina = view.findViewById(R.id.nome_disciplina)
        detalhesDisciplina = view.findViewById(R.id.detalhes_disciplina)
        verAulas = view.findViewById(R.id.ver_aulas)
    }
}

class DisciplinaAdapter() :
    RecyclerView.Adapter<DisciplinaHolder>() {

    private lateinit var navegador: NavController
    private lateinit var disciplinas: List<DisciplinaModelo>

    constructor(navegador: NavController, disciplinas: List<DisciplinaModelo>) : this() {
        this.navegador = navegador
        this.disciplinas = disciplinas
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplinaHolder {
        val cardDisciplina =
            LayoutInflater.from(parent.context).inflate(R.layout.card_disciplina, parent, false)

        return DisciplinaHolder(cardDisciplina)
    }

    override fun onBindViewHolder(holder: DisciplinaHolder, position: Int) {
        val disciplina = disciplinas[position]

        holder.nomeDisciplina.text = disciplina.nome
        holder.detalhesDisciplina.text = disciplina.detalhes

        (View.OnClickListener{
            navegador.navigate(R.id.mostrar_aulas,
                bundleOf("idDisciplina" to disciplina.id)
            )}).also { holder.card.setOnClickListener(it) }.also { holder.verAulas.setOnClickListener(it) }
    }

    override fun getItemCount(): Int {
        return disciplinas.count()
    }
}

class DisciplinasFragment : Fragment() {

    private var _binding: FragmentDisciplinasBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var disciplinaDao: DisciplinaDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDisciplinasBinding.inflate(inflater, container, false)

        // Inicialize o banco de dados
        database = AppDatabase.instancia(requireContext())
        disciplinaDao = database.disciplinaDao()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = LinearLayoutManager(this.context)
        binding.disciplinas.layoutManager = layout

//        GetDisciplinas(::onDisciplinas).execute()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val disciplinas = withContext(Dispatchers.IO) {
                    supabase.from("disciplina").select().decodeList<DisciplinaModelo>()
                }

                disciplinas.map { disciplina ->
                    DisciplinaModelo(
                        id = disciplina.id,
                        nome = disciplina.nome,
                        detalhes = disciplina.detalhes
                    )

                    disciplinaDao.salva(disciplina)
                }

                val disciplinasSalvas = disciplinaDao.buscaTodos()
                println(disciplinasSalvas.toString())

                onDisciplinas(disciplinas)
            } catch (e: Exception) {
                Log.e("error", e.toString())
            }
        }
    }

    private fun onDisciplinas(disciplinas: List<DisciplinaModelo>) {
        if (disciplinas.isEmpty()) {
//            ErroActivity.exibirErro(this.requireActivity(), "disciplinas não encontradas")
        } else {
            val adapter = DisciplinaAdapter(findNavController(), disciplinas)
            binding.disciplinas.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}