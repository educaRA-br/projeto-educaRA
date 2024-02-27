package com.sceneform

import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.Sceneform
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import edu.ifba.educa_ra.R
import edu.ifba.educa_ra.modelo.objetoSelecionado
import edu.ifba.educa_ra.ui.ErroActivity
import edu.ifba.educa_ra.ui.exibirInfoObjetoSelecionado
import kotlinx.android.synthetic.main.activity_visualizador.exibir_informacoes
import java.lang.ref.WeakReference
import java.util.function.Consumer
import java.util.function.Function

class VisualizadorARCoreActivity : AppCompatActivity(), FragmentOnAttachListener, BaseArFragment.OnTapArPlaneListener,
    BaseArFragment.OnSessionConfigurationListener, ArFragment.OnViewCreatedListener {

    private lateinit var areaVisualizacao: ArFragment
    private lateinit var pararVisualizacao: ImageButton
    private lateinit var exibirInformacoes: ImageButton
    private var modelo: Renderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_visualizador)
        supportFragmentManager.addFragmentOnAttachListener(this)

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.area_visualizacao, ArFragment::class.java, null)
                    .commit()
            }
        }

        pararVisualizacao = this.findViewById(R.id.parar_visualizacao) as ImageButton
        pararVisualizacao.setOnClickListener(View.OnClickListener {
            this.finish()
        })

        exibirInformacoes = this.findViewById(R.id.exibir_informacoes) as ImageButton
        exibir_informacoes.setOnClickListener(View.OnClickListener {
            exibirInfoObjetoSelecionado(this)
        })

        carregarModelo()
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id === R.id.area_visualizacao) {
            areaVisualizacao = fragment as ArFragment
            areaVisualizacao.setOnSessionConfigurationListener(this)
            areaVisualizacao.setOnViewCreatedListener(this)
            areaVisualizacao.setOnTapArPlaneListener(this)
        }
    }

    private fun carregarModelo() {
        val weakActivity: WeakReference<VisualizadorARCoreActivity> = WeakReference(this)
        ModelRenderable.builder()
            .setSource(this,
                Uri.parse("file://${objetoSelecionado.caminho}/${objetoSelecionado.nome}.obj"))
            .setIsFilamentGltf(true)
            .setAsyncLoadEnabled(true)
            .build()
            .thenAccept(Consumer { model: ModelRenderable ->
                val activity: VisualizadorARCoreActivity? = weakActivity.get()
                if (activity != null) {
                    activity.modelo = model
                }
            })
            .exceptionally(Function<Throwable, Void?> {
                ErroActivity.exibirErro(this, "não foi possível carregar o modelo")
                null
            })
    }

    override fun onTapPlane(hitResult: HitResult?, plane: Plane?, motionEvent: MotionEvent?) {
        if (modelo == null) {
            return
        }

        val anchor: Anchor = hitResult!!.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.parent = areaVisualizacao.arSceneView.scene

        val model = TransformableNode(areaVisualizacao.transformationSystem)
        model.parent = anchorNode
        model.setRenderable(modelo)
            .animate(true).start()
        model.select()
    }

    override fun onSessionConfiguration(session: Session?, config: Config?) {
        if (session != null && config != null) {
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
        }
    }

    override fun onViewCreated(arSceneView: ArSceneView?) {
        areaVisualizacao.setOnViewCreatedListener(null)
        arSceneView?.setFrameRateFactor(SceneView.FrameRate.FULL)
    }

}