package com.smk.educara_3d

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.smk.educara_screens.exibirInfoObjetoSelecionado
import com.smk.educara_screens.ui.DataHolder.Companion.objetoSelecionado
import org.andresoviedo.util.android.ContentUtils

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
class VisualizadorActivity : Activity() {
    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private var modelType = 0

    /**
     * The name of the object
     */
    private var obj: String? = null

    /**
     * The 3d model file to load. Passed as input parameter
     */
    private var model: Uri? = null

    /**
     * The texture file to load. Passed as input parameter
     */
    private var texture: Uri? = null

    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     */
    private var immersiveMode: Boolean = true

    /**
     * Background GL clear color. Default is light gray
     */
    private val backgroundColor: FloatArray = floatArrayOf(0f, 0f, 0f, 1.0f)
    private var gLView: VisualizadorDeSuperficieDeModelo? = null
    private var scene: CarregadorDeCena? = null
    private var handler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Try to get input parameters
        val b = intent.extras
        if (b != null) {
            if (b.getString("object") != null) {
                obj = b.getString("object")
            }
            if (b.getString("model") != null) {
                model = Uri.parse("file://" + b.getString("model"))
            }
            if (b.getString("texture") != null) {
                texture = Uri.parse("file://" + b.getString("texture"))
            }
            modelType = if (b.getString("type") != null) b.getString("type")!!.toInt() else -1
            immersiveMode = "true".equals(b.getString("immersiveMode"), ignoreCase = true)
            try {
                val backgroundColors = b.getString("backgroundColor")!!
                    .split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                backgroundColor[0] = backgroundColors[0].toFloat()
                backgroundColor[1] = backgroundColors[1].toFloat()
                backgroundColor[2] = backgroundColors[2].toFloat()
                backgroundColor[3] = backgroundColors[3].toFloat()
            } catch (ex: Exception) {
                // Assuming default background color
            }
        }
        handler = Handler(mainLooper)

        // Create our 3D sceneario
        scene = CarregadorDeCena(this)
        scene!!.init()

        // Use a layout to wrap a model viewer
        setContentView(buildLayout(VisualizadorDeSuperficieDeModelo(this)))

        ContentUtils.printTouchCapabilities(packageManager)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun buildLayout(visualizador: VisualizadorDeSuperficieDeModelo): FrameLayout {
        val frame = FrameLayout(this)
        frame.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        frame.addView(visualizador)

        val linear = LinearLayout(this)
        linear.orientation = LinearLayout.VERTICAL
        linear.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        linear.gravity = Gravity.END or Gravity.TOP
        frame.addView(linear)

        val goBack = ImageButton(this)
        goBack.setImageResource(R.drawable.arrow_back)
        goBack.setBackgroundColor(Color.TRANSPARENT)
        goBack.setOnClickListener {
            this.finish()
        }

        val goBackLayout = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT)
        goBackLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1)
        goBack.layoutParams = goBackLayout
        linear.addView(goBack)

        val info = ImageButton(this)
        info.setImageResource(R.drawable.information)
        info.setBackgroundColor(Color.TRANSPARENT)
        info.setOnClickListener {
            objetoSelecionado?.let { obj -> exibirInfoObjetoSelecionado(this, obj) }
        }

        val infoLayout = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT)
        infoLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1)
        info.layoutParams = infoLayout
        linear.addView(info)

        return frame
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUIDelayed()
        }
    }

    private fun hideSystemUIDelayed() {
        if (!immersiveMode) {
            return
        }
        handler!!.removeCallbacksAndMessages(null)
    }

    fun getGLView(): VisualizadorDeSuperficieDeModelo? {
        return gLView
    }

    fun getBackgroundColor(): FloatArray {
        return backgroundColor
    }

    fun getScene(): CarregadorDeCena? {
        return scene
    }

    fun getObject(): String? {
        return obj
    }

    fun getModelUri(): Uri? {
        return model
    }

    fun getTextureUri(): Uri? {
        return texture
    }

    fun getModelType(): Int {
        return modelType
    }
}