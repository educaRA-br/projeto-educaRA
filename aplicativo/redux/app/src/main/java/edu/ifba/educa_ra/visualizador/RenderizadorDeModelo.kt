package edu.ifba.educa_ra.visualizador

import android.opengl.GLES20
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import org.andresoviedo.android_3d_model_engine.animation.Animator
import org.andresoviedo.android_3d_model_engine.drawer.DrawerFactory
import org.andresoviedo.android_3d_model_engine.model.AnimatedModel
import org.andresoviedo.android_3d_model_engine.model.Object3D
import org.andresoviedo.android_3d_model_engine.model.Object3DData
import org.andresoviedo.android_3d_model_engine.services.Object3DBuilder
import org.andresoviedo.util.android.GLUtil
import java.io.ByteArrayInputStream
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RendererizadorDeModelo(private val main: VisualizadorDeSuperficieDeModelo) : GLSurfaceView.Renderer {
    // width of the screen
    private var width = 0

    // height of the screen
    private var height = 0

    /**
     * Drawer factory to get right renderer/shader based on object attributes
     */
    private val drawer: DrawerFactory = DrawerFactory(main.context)

    /**
     * 3D Axis (to show if needed)
     */
    private val axis = Object3DBuilder.buildAxis().setId("axis")

    // The wireframe associated shape (it should be made of lines only)
    private val wireframes: MutableMap<Object3DData?, Object3DData?> = HashMap()

    // The loaded textures
    private val textures: MutableMap<Any, Int> = HashMap()

    // The corresponding opengl bounding boxes and drawer
    private val boundingBoxes: MutableMap<Object3DData?, Object3DData?> = HashMap()

    // The corresponding opengl bounding boxes
    private val normals: MutableMap<Object3DData?, Object3DData> = HashMap()
    private val skeleton: MutableMap<Object3DData, Object3DData?> = HashMap()

    // 3D matrices to project our 3D world
    private val viewMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewProjectionMatrix = FloatArray(16)
    private val lightPosInEyeSpace = FloatArray(4)

    // 3D stereoscopic matrix (left & right camera)
    private val viewMatrixLeft = FloatArray(16)
    private val projectionMatrixLeft = FloatArray(16)
    private val viewProjectionMatrixLeft = FloatArray(16)
    private val viewMatrixRight = FloatArray(16)
    private val projectionMatrixRight = FloatArray(16)
    private val viewProjectionMatrixRight = FloatArray(16)

    /**
     * Whether the info of the model has been written to console log
     */
    private val infoLogged: MutableMap<Object3DData?, Boolean> = HashMap()

    /**
     * Switch to akternate drawing of right and left image
     */
    private var anaglyphSwitch = false

    /**
     * Skeleton Animator
     */
    private val animator = Animator()

    /**
     * Did the application explode?
     */
    private var fatalException = false

    private val near: Float = NEAR
    private val far: Float = FAR

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        val backgroundColor: FloatArray = main.visualizadorActivity.getBackgroundColor()
        GLES20.glClearColor(
            backgroundColor[0],
            backgroundColor[1], backgroundColor[2], backgroundColor[3]
        )

        // Use culling to remove back faces.
        // Don't remove back faces so we can see them
        // GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing for hidden-surface elimination.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // Enable not drawing out of view port
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        this.width = width
        this.height = height

        // Adjust the viewport based on geometry changes, such as screen rotation
        GLES20.glViewport(0, 0, width, height)

        // the projection matrix is the 3D virtual space (cube) that we want to project
        val ratio = width.toFloat() / height
        Log.d(TAG, "projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10]")
        Matrix.frustumM(
            projectionMatrix, 0, -ratio, ratio, -1f, 1f,
            this.near,
            this.far
        )
        Matrix.frustumM(
            projectionMatrixRight, 0, -ratio, ratio, -1f, 1f,
            this.near,
            this.far
        )
        Matrix.frustumM(
            projectionMatrixLeft, 0, -ratio, ratio, -1f, 1f,
            this.near,
            this.far
        )
    }

    override fun onDrawFrame(unused: GL10) {
        if (fatalException) {
            return
        }
        try {
            GLES20.glViewport(0, 0, width, height)
            GLES20.glScissor(0, 0, width, height)

            // Draw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            val scene: CarregadorDeCena = main.visualizadorActivity.getScene()!!
            if (scene.isBlendingEnabled) {
                // Enable blending for combining colors when there is transparency
                GLES20.glEnable(GL_BLEND)
                GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            } else {
                GLES20.glDisable(/* cap = */ GL_BLEND)
            }

            // animate scene
            scene.onDrawFrame()

            // recalculate mvp matrix according to where we are looking at now
            val camera = scene.camera
            if (camera!!.hasChanged()) {
                // INFO: Set the camera position (View matrix)
                // The camera has 3 vectors (the position, the vector where we are looking at, and the up position (sky)

                // the projection matrix is the 3D virtual space (cube) that we want to project
                val ratio = width.toFloat() / height
                // Log.v(TAG, "Camera changed: projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10], ");
                if (!scene.isStereoscopic) {
                    Matrix.setLookAtM(
                        viewMatrix,
                        0,
                        camera.xPos,
                        camera.yPos,
                        camera.zPos,
                        camera.xView,
                        camera.yView,
                        camera.zView,
                        camera.xUp,
                        camera.yUp,
                        camera.zUp
                    )
                    Matrix.multiplyMM(
                        viewProjectionMatrix, 0,
                        projectionMatrix, 0, viewMatrix, 0
                    )
                } else {
                    val stereoCamera = camera.toStereo(EYE_DISTANCE)
                    val leftCamera = stereoCamera[0]
                    val rightCamera = stereoCamera[1]

                    // camera on the left for the left eye
                    Matrix.setLookAtM(
                        viewMatrixLeft,
                        0,
                        leftCamera.xPos,
                        leftCamera.yPos,
                        leftCamera.zPos,
                        leftCamera.xView,
                        leftCamera.yView,
                        leftCamera.zView,
                        leftCamera.xUp,
                        leftCamera.yUp,
                        leftCamera.zUp
                    )
                    // camera on the right for the right eye
                    Matrix.setLookAtM(
                        viewMatrixRight,
                        0,
                        rightCamera.xPos,
                        rightCamera.yPos,
                        rightCamera.zPos,
                        rightCamera.xView,
                        rightCamera.yView,
                        rightCamera.zView,
                        rightCamera.xUp,
                        rightCamera.yUp,
                        rightCamera.zUp
                    )
                    if (scene.isAnaglyph) {
                        Matrix.frustumM(
                            projectionMatrixRight, 0, -ratio, ratio, -1f, 1f,
                            this.near,
                            this.far
                        )
                        Matrix.frustumM(
                            projectionMatrixLeft, 0, -ratio, ratio, -1f, 1f,
                            this.near,
                            this.far
                        )
                    } else if (scene.isVRGlasses) {
                        val ratio2 = width.toFloat() / 2 / height
                        Matrix.frustumM(
                            projectionMatrixRight, 0, -ratio2, ratio2, -1f, 1f,
                            this.near,
                            this.far
                        )
                        Matrix.frustumM(
                            projectionMatrixLeft, 0, -ratio2, ratio2, -1f, 1f,
                            this.near,
                            this.far
                        )
                    }
                    // Calculate the projection and view transformation
                    Matrix.multiplyMM(
                        viewProjectionMatrixLeft,
                        0,
                        projectionMatrixLeft,
                        0,
                        viewMatrixLeft,
                        0
                    )
                    Matrix.multiplyMM(
                        viewProjectionMatrixRight,
                        0,
                        projectionMatrixRight,
                        0,
                        viewMatrixRight,
                        0
                    )
                }
                camera.setChanged(false)
            }
            if (!scene.isStereoscopic) {
                this.onDrawFrame(
                    viewMatrix,
                    projectionMatrix, lightPosInEyeSpace, null
                )
                return
            }
            if (scene.isAnaglyph) {
                // INFO: switch because blending algorithm doesn't mix colors
                if (anaglyphSwitch) {
                    this.onDrawFrame(
                        viewMatrixLeft,
                        projectionMatrixLeft,
                        lightPosInEyeSpace,
                        COLOR_RED
                    )
                } else {
                    this.onDrawFrame(
                        viewMatrixRight,
                        projectionMatrixRight,
                        lightPosInEyeSpace,
                        COLOR_BLUE
                    )
                }
                anaglyphSwitch = !anaglyphSwitch
                return
            }
            if (scene.isVRGlasses) {

                // draw left eye image
                GLES20.glViewport(0, 0, width / 2, height)
                GLES20.glScissor(0, 0, width / 2, height)
                this.onDrawFrame(
                    viewMatrixLeft,
                    projectionMatrixLeft,
                    lightPosInEyeSpace,
                    null
                )

                // draw right eye image
                GLES20.glViewport(width / 2, 0, width / 2, height)
                GLES20.glScissor(width / 2, 0, width / 2, height)
                this.onDrawFrame(
                    viewMatrixRight,
                    projectionMatrixRight,
                    lightPosInEyeSpace,
                    null
                )
            }
        } catch (ex: Exception) {
            Log.e("ModelRenderer", "Fatal exception: " + ex.message, ex)
            fatalException = true
        }
    }

    private fun onDrawFrame(
        viewMatrix: FloatArray, projectionMatrix: FloatArray, lightPosInEyeSpace: FloatArray,
        colorMask: FloatArray?
    ) {
        val scene: CarregadorDeCena = main.visualizadorActivity.getScene()!!

        // draw light
        if (scene.isDrawLighting) {
            val lightBulbDrawer = drawer.pointDrawer
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, scene.lightBulb.modelMatrix, 0)

            // Calculate position of the light in eye space to support lighting
            Matrix.multiplyMV(lightPosInEyeSpace, 0, modelViewMatrix, 0, scene.lightPosition, 0)

            // Draw a point that represents the light bulb
            lightBulbDrawer.draw(
                scene.lightBulb, projectionMatrix, viewMatrix, -1, lightPosInEyeSpace,
                colorMask
            )
        }

        // draw axis
        if (scene.isDrawAxis) {
            val basicDrawer = drawer.pointDrawer
            basicDrawer.draw(
                axis, projectionMatrix, viewMatrix, axis.drawMode, axis
                    .drawSize, -1, lightPosInEyeSpace, colorMask
            )
        }


        // is there any object?
        if (scene.objects.isEmpty()) {
            return
        }

        // draw all available objects
        val objects = scene.objects
        for (i in objects.indices) {
            var objData: Object3DData? = null
            try {
                objData = objects[i]
                var drawerObject: Object3D? = drawer.getDrawer(
                    objData, scene.isDrawTextures, scene.isDrawLighting,
                    scene.isDoAnimation, scene.isDrawColors
                ) ?: continue
                if (!infoLogged.containsKey(objData)) {
                    Log.i(
                        "ModelRenderer",
                        "Model '" + objData.id + "'. Drawer " + drawerObject!!.javaClass.name
                    )
                    infoLogged[objData] = true
                }
                val changed = objData.isChanged

                var textureId = textures[objData.textureData]
                var emissiveTextureId = textures[objData.emissiveTextureData]
                if (textureId == null && objData.textureData != null) {
                    //Log.i("ModelRenderer","Loading texture '"+objData.getTextureFile()+"'...");
                    val textureIs = ByteArrayInputStream(objData.textureData)
                    var emissiveTextureIs: ByteArrayInputStream? = null
                    if (emissiveTextureId == null && objData.emissiveTextureData != null) {
                        emissiveTextureIs = ByteArrayInputStream(objData.emissiveTextureData)
                    }
                    val textureIds = GLUtil.loadTexture(textureIs, emissiveTextureIs)
                    textureId = textureIds[0]
                    emissiveTextureId = textureIds[1]
                    textureIs.close()
                    emissiveTextureIs?.close()
                    textures[objData.textureData] = textureId
                    textures[objData.emissiveTextureData] = emissiveTextureId
                    objData.emissiveTextureHandle = emissiveTextureId
                    //Log.i("GLUtil", "Loaded texture ok");
                }
                if (textureId == null) {
                    textureId = -1
                }

                // draw points
                if (objData.drawMode == GLES20.GL_POINTS) {
                    val basicDrawer = drawer.pointDrawer
                    basicDrawer.draw(
                        objData,
                        projectionMatrix,
                        viewMatrix,
                        GLES20.GL_POINTS,
                        lightPosInEyeSpace
                    )
                } else if (scene.isDrawWireframe && objData.drawMode != GLES20.GL_POINTS && objData.drawMode != GLES20.GL_LINES && objData.drawMode != GLES20.GL_LINE_STRIP && objData.drawMode != GLES20.GL_LINE_LOOP) {
                    // Log.d("ModelRenderer","Drawing wireframe model...");
                    try {
                        // Only draw wireframes for objects having faces (triangles)
                        var wireframe = wireframes[objData]
                        if (wireframe == null || changed) {
                            Log.i("ModelRenderer", "Generating wireframe model...")
                            wireframe = Object3DBuilder.buildWireframe(objData)
                            wireframes[objData] = wireframe
                        }
                        drawerObject!!.draw(
                            wireframe, projectionMatrix, viewMatrix, wireframe!!.drawMode,
                            wireframe.drawSize, textureId, lightPosInEyeSpace,
                            colorMask
                        )
                    } catch (e: Error) {
                        Log.e("ModelRenderer", e.message, e)
                    }
                } else if (scene.isDrawPoints || objData.faces == null || !objData.faces.loaded()) {
                    drawerObject!!.draw(
                        objData, projectionMatrix, viewMatrix, GLES20.GL_POINTS, objData.drawSize,
                        textureId, lightPosInEyeSpace, colorMask
                    )
                } else if (scene.isDrawSkeleton && objData is AnimatedModel && objData
                        .animation != null
                ) {
                    var skeleton = skeleton[objData]
                    if (skeleton == null) {
                        skeleton = Object3DBuilder.buildSkeleton(objData as AnimatedModel?)
                        this.skeleton[objData] = skeleton
                    }
                    animator.update(skeleton, scene.isShowBindPose)
                    drawerObject = drawer.getDrawer(
                        skeleton, false, scene.isDrawLighting, scene
                            .isDoAnimation, scene.isDrawColors
                    )
                    drawerObject.draw(
                        skeleton,
                        projectionMatrix,
                        viewMatrix,
                        -1,
                        lightPosInEyeSpace,
                        colorMask
                    )
                } else {
                    drawerObject!!.draw(
                        objData, projectionMatrix, viewMatrix,
                        textureId, lightPosInEyeSpace, colorMask
                    )
                }

                // Draw bounding box
                if (scene.isDrawBoundingBox || scene.selectedObject === objData) {
                    var boundingBoxData = boundingBoxes[objData]
                    if (boundingBoxData == null || changed) {
                        boundingBoxData = Object3DBuilder.buildBoundingBox(objData)
                        boundingBoxes[objData] = boundingBoxData
                    }
                    val boundingBoxDrawer = drawer.boundingBoxDrawer
                    boundingBoxDrawer.draw(
                        boundingBoxData, projectionMatrix, viewMatrix, -1,
                        lightPosInEyeSpace, colorMask
                    )
                }

                // Draw normals
                if (scene.isDrawNormals) {
                    var normalData = normals[objData]
                    if (normalData == null || changed) {
                        normalData = Object3DBuilder.buildFaceNormals(objData)
                        if (normalData != null) {
                            // it can be null if object isnt made of triangles
                            normals[objData] = normalData
                        }
                    }
                    if (normalData != null) {
                        val normalsDrawer = drawer.faceNormalsDrawer
                        normalsDrawer.draw(normalData, projectionMatrix, viewMatrix, -1, null)
                    }
                }
            } catch (ex: Exception) {
                Log.e(
                    "ModelRenderer",
                    "There was a problem rendering the object '" + objData!!.id + "':" + ex.message,
                    ex
                )
            }
        }
    }

    fun getModelViewMatrix(): FloatArray {
        return viewMatrix
    }

    fun getFar(): Float {
        return far
    }

    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }

    fun getModelProjectionMatrix(): FloatArray {
        return projectionMatrix
    }

    companion object {
        private val TAG: String
            get() = RendererizadorDeModelo::class.java.name

        // frustrum - nearest pixel
        private const val NEAR = 1f

        // frustrum - fartest pixel
        private const val FAR = 100f

        // stereoscopic variables
        private const val EYE_DISTANCE = 0.64f
        private val COLOR_RED = floatArrayOf(1.0f, 0.0f, 0.0f, 1f)
        private val COLOR_BLUE = floatArrayOf(0.0f, 1.0f, 0.0f, 1f)
    }
}