package com.smk.educara_3d

import android.annotation.SuppressLint
import android.opengl.GLSurfaceView
import android.view.MotionEvent

/**
 * This is the actual opengl view. From here we can detect touch gestures for example
 *
 * @author andresoviedo
 */
@SuppressLint("ViewConstructor")
class VisualizadorDeSuperficieDeModelo(private val visualizador: VisualizadorActivity) : GLSurfaceView(visualizador) {
    private val mRenderer: RendererizadorDeModelo
    private val touchHandler: ControladorDeToque

    init {
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)

        // This is the actual renderer of the 3D space
        mRenderer = RendererizadorDeModelo(this)
        setRenderer(mRenderer)

        // Render the view only when there is a change in the drawing data
        touchHandler = ControladorDeToque(this, mRenderer)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        try {
            return touchHandler.onTouchEvent(event)
        } catch (ex: Exception) {
            return false
        }
    }

    val modelRenderer: RendererizadorDeModelo
        get() = mRenderer

    val visualizadorActivity: VisualizadorActivity
        get() = visualizador
}