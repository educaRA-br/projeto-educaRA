package com.smk.educara_3d

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.smk.educara_screens.avisar
import org.andresoviedo.android_3d_model_engine.animation.Animator
import org.andresoviedo.android_3d_model_engine.collision.CollisionDetection
import org.andresoviedo.android_3d_model_engine.model.Camera
import org.andresoviedo.android_3d_model_engine.model.Object3DData
import org.andresoviedo.android_3d_model_engine.services.LoaderTask
import org.andresoviedo.android_3d_model_engine.services.Object3DBuilder
import org.andresoviedo.android_3d_model_engine.services.collada.ColladaLoaderTask
import org.andresoviedo.android_3d_model_engine.services.gltf.GltfLoaderTask
import org.andresoviedo.android_3d_model_engine.services.stl.STLLoaderTask
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoaderTask
import org.andresoviedo.util.android.ContentUtils
import org.andresoviedo.util.io.IOUtils
import java.io.IOException
import java.util.Arrays
import java.util.Locale.getDefault

/**
 * This class loads a 3D scena as an example of what can be done with the app
 *
 * @author andresoviedo
 */
@Suppress("NAME_SHADOWING", "DEPRECATED_IDENTITY_EQUALS", "DEPRECATION")
class CarregadorDeCena(visualizadorActivity: VisualizadorActivity) : LoaderTask.Callback {
    /**
     * Parent component
     */
    private val activity: VisualizadorActivity

    /**
     * List of data objects containing info for building the opengl objects
     */
    @get:Synchronized
    var objects: List<Object3DData> = ArrayList()
        private set

    /**
     * Show axis or not
     */
    var isDrawAxis = false

    /**
     * Point of view camera
     */
    var camera: Camera? = null
        private set

    /**
     * Enable or disable blending (transparency)
     */
    var isBlendingEnabled = false
        private set

    /**
     * Whether to draw objects as wireframes
     */
    var isDrawWireframe = false
        private set

    /**
     * Whether to draw using points
     */
    var isDrawPoints = false
        private set

    /**
     * Whether to draw bounding boxes around objects
     */
    var isDrawBoundingBox = false
        private set

    /**
     * Whether to draw face normals. Normally used to debug models
     */
    val isDrawNormals = false

    /**
     * Whether to draw using textures
     */
    var isDrawTextures = true
        private set

    /**
     * Whether to draw using colors or use default white color
     */
    var isDrawColors = true
        private set

    /**
     * Light toggle feature: we have 3 states: no light, light, light + rotation
     */
    private var rotatingLight = false

    /**
     * Light toggle feature: whether to draw using lights
     */
    var isDrawLighting = true
        private set

    /**
     * Animate model (dae only) or not
     */
    var isDoAnimation = false
        private set

    /**
     * show bind pose only
     */
    var isShowBindPose = false
        private set

    /**
     * Draw skeleton or not
     */
    var isDrawSkeleton = false
        private set

    /**
     * Toggle collision detection
     */
    private var isCollision = false

    /**
     * Toggle 3d
     */
    var isStereoscopic = false
        private set

    /**
     * Toggle 3d anaglyph (red, blue glasses)
     */
    var isAnaglyph = false
        private set

    /**
     * Toggle 3d VR glasses
     */
    var isVRGlasses = false
        private set

    /**
     * Object selected by the user
     */
    var selectedObject: Object3DData? = null
        private set

    /**
     * Initial light position
     */
    val lightPosition = floatArrayOf(0f, 0f, 6f, 1f)

    /**
     * Light bulb 3d data
     */
    val lightBulb: Object3DData = Object3DBuilder.buildPoint(lightPosition).setId("light")

    /**
     * Animator
     */
    private val animator = Animator()

    /**
     * Did the user touched the model for the first time?
     */
    private var userHasInteracted = false

    /**
     * time when model loading has started (for stats)
     */
    private var startTime: Long = 0

    init {
        activity = visualizadorActivity
    }

    fun init() {
        // Camera to show a point of view
        camera = Camera()
        camera!!.setChanged(true) // force first draw
        if (activity.getModelUri() == null) {
            return
        }
        startTime = SystemClock.uptimeMillis()

        val modelUri: Uri = activity.getModelUri()!!
        ContentUtils.addUri("${activity.getObject()}.mtl", activity.getTextureUri())

        Log.i("Object3DBuilder", "Loading model $modelUri. async and parallel..")
        if (modelUri.toString().lowercase(getDefault())
                .endsWith(".obj") || activity.getModelType() === 0
        ) {
            WavefrontLoaderTask(activity, modelUri, this).execute()
        } else if (modelUri.toString().lowercase(getDefault())
                .endsWith(".stl") || activity.getModelType() === 1
        ) {
            Log.i("Object3DBuilder", "Loading STL object from: $modelUri")
            STLLoaderTask(this.activity, modelUri, this).execute()
        } else if (modelUri.toString().lowercase(getDefault())
                .endsWith(".dae") || activity.getModelType() === 2
        ) {
            Log.i("Object3DBuilder", "Loading Collada object from: $modelUri")
            ColladaLoaderTask(activity, modelUri, this).execute()
        } else if (modelUri.toString().lowercase(getDefault())
                .endsWith(".gltf") || activity.getModelType() === 3
        ) {
            Log.i("Object3DBuilder", "Loading GLtf object from: $modelUri")
            GltfLoaderTask(activity, modelUri, this).execute()
        }
    }

    /**
     * Hook for animating the objects before the rendering
     */
    fun onDrawFrame() {
        animateLight()

        // smooth camera transition
        camera!!.animate()

        // initial camera animation. animate if user didn't touch the screen
        if (!userHasInteracted) {
            animateCamera()
        }
        if (objects.isEmpty()) return
        if (isDoAnimation) {
            for (i in objects.indices) {
                val obj = objects[i]
                animator.update(obj, isShowBindPose)
            }
        }
    }

    private fun animateLight() {
        if (!rotatingLight) return

        // animate light - Do a complete rotation every 5 seconds.
        val time = SystemClock.uptimeMillis() % 5000L
        val angleInDegrees = 360.0f / 5000.0f * time.toInt()
        lightBulb.setRotationY(angleInDegrees)
    }

    private fun animateCamera() {
        camera!!.translateCamera(0.0025f, 0f)
    }

    @Synchronized
    fun addObject(obj: Object3DData) {
        val newList: MutableList<Object3DData> = ArrayList(objects)
        newList.add(obj)
        objects = newList
        requestRender()
    }

    private fun requestRender() {
        if (activity.getGLView() != null) {
            activity.getGLView()!!.requestRender()
        }
    }

    override fun onStart() {
        ContentUtils.setThreadActivity(activity)
    }

    override fun onLoadComplete(datas: List<Object3DData>) {
        for (data in datas) {
            if ((data.textureData == null) && (data.textureFile != null)) {
                Log.i("LoaderTask", "Loading texture... " + data.textureFile)
                try {
                    ContentUtils.getInputStream(data.textureFile).use { stream ->
                        if (stream != null) {
                            data.textureData = IOUtils.read(stream)
                        }
                    }

                    Log.i("LoaderTask", "Texture OK" + data.textureFile)
                } catch (ex: IOException) {
                    data.addError("Problem loading texture " + data.textureFile)
                }
            }
        }

        val allErrors = ArrayList<String>()
        for (data in datas) {
            addObject(data)
            allErrors.addAll(data.errors)
        }
        if (allErrors.isNotEmpty()) {
            for (error: String in allErrors) {
                Log.e("CarregadorDeCena", error)
            }
        }

        ContentUtils.setThreadActivity(null)
    }

    override fun onLoadError(ex: Exception) {
        avisar(this.activity, "Ocorreu um erro carregando o modelo 3D")

        ContentUtils.setThreadActivity(null)
    }

    fun processTouch(x: Float, y: Float) {
        val mr: RendererizadorDeModelo = activity.getGLView()!!.modelRenderer
        val objectToSelect = CollisionDetection.getBoxIntersection(
            objects,
            mr.getWidth(),
            mr.getHeight(),
            mr.getModelViewMatrix(),
            mr.getModelProjectionMatrix(),
            x,
            y
        )
        if (objectToSelect != null) {
            selectedObject = if (selectedObject === objectToSelect) {
                Log.i("CarregadorDeCena", "Unselected object " + objectToSelect.id)
                null
            } else {
                Log.i("CarregadorDeCena", "Selected object " + objectToSelect.id)
                objectToSelect
            }
            if (isCollision) {
                Log.d("CarregadorDeCena", "Detecting collision...")
                val point = CollisionDetection.getTriangleIntersection(
                    objects,
                    mr.getWidth(),
                    mr.getHeight(),
                    mr.getModelViewMatrix(),
                    mr.getModelProjectionMatrix(),
                    x,
                    y
                )
                if (point != null) {
                    Log.i("CarregadorDeCena", "Drawing intersection point: " + Arrays.toString(point))
                    addObject(
                        Object3DBuilder.buildPoint(point).setColor(floatArrayOf(1.0f, 0f, 0f, 1f))
                    )
                }
            }
        }
    }

    fun processMove() {
        userHasInteracted = true
    }

}