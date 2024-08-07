package org.andresoviedo.app.model3D.controller

import android.graphics.PointF
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import edu.ifba.educa_ra.visualizador.VisualizadorDeSuperficieDeModelo
import edu.ifba.educa_ra.visualizador.CarregadorDeCena
import edu.ifba.educa_ra.visualizador.RendererizadorDeModelo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

class ControladorDeToque(private val view: VisualizadorDeSuperficieDeModelo, renderer: RendererizadorDeModelo) {
    private val mRenderer: RendererizadorDeModelo
    private var pointerCount = 0
    private var x1 = Float.MIN_VALUE
    private var y1 = Float.MIN_VALUE
    private var x2 = Float.MIN_VALUE
    private var y2 = Float.MIN_VALUE
    private var dx1 = Float.MIN_VALUE
    private var dy1 = Float.MIN_VALUE
    private var dx2 = Float.MIN_VALUE
    private var dy2 = Float.MIN_VALUE
    private var length = Float.MIN_VALUE
    private var previousLength = Float.MIN_VALUE
    private var currentPress1 = Float.MIN_VALUE
    private var currentPress2 = Float.MIN_VALUE
    private var rotation = 0f
    private var currentSquare = Int.MIN_VALUE
    private var isOneFixedAndOneMoving = false
    private var fingersAreClosing = false
    private var isRotating = false
    private var gestureChanged = false
    private var moving = false
    private var simpleTouch = false
    private var lastActionTime: Long = 0
    private var touchDelay = -2
    private var touchStatus = -1
    private var previousX1 = 0f
    private var previousY1 = 0f
    private var previousX2 = 0f
    private var previousY2 = 0f
    private val previousVector = FloatArray(4)
    private val vector = FloatArray(4)
    private val rotationVector = FloatArray(4)
    private var previousRotationSquare = 0f

    init {
        this.mRenderer = renderer
    }

    @Synchronized
    fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_HOVER_EXIT, MotionEvent.ACTION_OUTSIDE -> {
                // this to handle "1 simple touch"
                if (lastActionTime > SystemClock.uptimeMillis() - 250) {
                    simpleTouch = true
                } else {
                    gestureChanged = true
                    touchDelay = 0
                    lastActionTime = SystemClock.uptimeMillis()
                    simpleTouch = false
                }
                moving = false
            }

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_HOVER_ENTER -> {
                Log.d(TAG, "Gesture changed...")
                gestureChanged = true
                touchDelay = 0
                lastActionTime = SystemClock.uptimeMillis()
                simpleTouch = false
            }

            MotionEvent.ACTION_MOVE -> {
                moving = true
                simpleTouch = false
                touchDelay++
            }

            else -> {
                Log.w(TAG, "Unknown state: " + motionEvent.action)
                gestureChanged = true
            }
        }
        pointerCount = motionEvent.pointerCount
        if (pointerCount == 1) {
            x1 = motionEvent.x
            y1 = motionEvent.y
            if (gestureChanged) {
                Log.d(TAG, "x:$x1,y:$y1")
                previousX1 = x1
                previousY1 = y1
            }
            dx1 = x1 - previousX1
            dy1 = y1 - previousY1
        } else if (pointerCount == 2) {
            x1 = motionEvent.getX(0)
            y1 = motionEvent.getY(0)
            x2 = motionEvent.getX(1)
            y2 = motionEvent.getY(1)
            vector[0] = x2 - x1
            vector[1] = y2 - y1
            vector[2] = 0f
            vector[3] = 1f
            var len = Matrix.length(vector[0], vector[1], vector[2])
            vector[0] /= len
            vector[1] /= len

            // Log.d(TAG, "x1:" + x1 + ",y1:" + y1 + ",x2:" + x2 + ",y2:" + y2);
            if (gestureChanged) {
                previousX1 = x1
                previousY1 = y1
                previousX2 = x2
                previousY2 = y2
                System.arraycopy(vector, 0, previousVector, 0, vector.size)
            }
            dx1 = x1 - previousX1
            dy1 = y1 - previousY1
            dx2 = x2 - previousX2
            dy2 = y2 - previousY2
            rotationVector[0] = previousVector[1] * vector[2] - previousVector[2] * vector[1]
            rotationVector[1] = previousVector[2] * vector[0] - previousVector[0] * vector[2]
            rotationVector[2] = previousVector[0] * vector[1] - previousVector[1] * vector[0]
            len = Matrix.length(
                rotationVector[0], rotationVector[1],
                rotationVector[2]
            )
            rotationVector[0] /= len
            rotationVector[1] /= len
            rotationVector[2] /= len
            previousLength = sqrt(
                    (previousX2 - previousX1).toDouble().pow(2.0) + (previousY2 - previousY1).toDouble().pow(2.0)
                ).toFloat()
            length =
                sqrt((x2 - x1).toDouble().pow(2.0) + (y2 - y1).toDouble().pow(2.0))
                    .toFloat()
            currentPress1 = motionEvent.getPressure(0)
            currentPress2 = motionEvent.getPressure(1)
            rotation = 0f
            rotation = TouchScreen.getRotation360(motionEvent)
            currentSquare = TouchScreen.getSquare(motionEvent)
            if (currentSquare == 1 && previousRotationSquare == 4f) {
                rotation = 0f
            } else if (currentSquare == 4 && previousRotationSquare == 1f) {
                rotation = 360f
            }

            // gesture detection
            isOneFixedAndOneMoving = dx1 + dy1 == 0f != (dx2 + dy2 == 0f)
            fingersAreClosing =
                !isOneFixedAndOneMoving && abs(dx1 + dx2) < 10 && abs(dy1 + dy2) < 10
            isRotating =
                !isOneFixedAndOneMoving && dx1 != 0f && dy1 != 0f && dx2 != 0f && dy2 != 0f && rotationVector[2] != 0f
        }
        if (pointerCount == 1 && simpleTouch) {
            val scene: CarregadorDeCena = view.visualizadorActivity.getScene()!!
            scene.processTouch(x1, y1)
        }
        val max: Int = mRenderer.getWidth().coerceAtLeast(mRenderer.getHeight())
        if (touchDelay > 1) {
            // INFO: Process gesture
            val scene: CarregadorDeCena = view.visualizadorActivity.getScene()!!
            scene.processMove()
            val camera = scene.camera
            if (pointerCount == 1) {
                touchStatus = TOUCH_STATUS_MOVING_WORLD
                // Log.d(TAG, "Translating camera (dx,dy) '" + dx1 + "','" + dy1 + "'...");
                dx1 = (dx1 / max * Math.PI * 2).toFloat()
                dy1 = (dy1 / max * Math.PI * 2).toFloat()
                camera!!.translateCamera(dx1, dy1)
            } else if (pointerCount == 2) {
                if (fingersAreClosing) {
                    touchStatus = TOUCH_STATUS_ZOOMING_CAMERA
                    val zoomFactor: Float = (length - previousLength) / max * mRenderer.getFar()
                    Log.i(TAG, "Zooming '$zoomFactor'...")
                    camera!!.MoveCameraZ(zoomFactor)
                }
                if (isRotating) {
                    touchStatus = TOUCH_STATUS_ROTATING_CAMERA
                    Log.i(
                        TAG, "Rotating camera '" + sign(
                            rotationVector[2]
                        ) + "'..."
                    )
                    camera!!.Rotate((sign(rotationVector[2]) / Math.PI).toFloat() / 4)
                }
            }

        }
        previousX1 = x1
        previousY1 = y1
        previousX2 = x2
        previousY2 = y2
        previousRotationSquare = currentSquare.toFloat()
        System.arraycopy(vector, 0, previousVector, 0, vector.size)
        if (gestureChanged && touchDelay > 1) {
            gestureChanged = false
            Log.v(TAG, "Fin")
        }
        view.requestRender()
        return true
    }

    companion object {
        private val TAG = ControladorDeToque::class.java.name
        private const val TOUCH_STATUS_ZOOMING_CAMERA: Int = 1
        private const val TOUCH_STATUS_ROTATING_CAMERA: Int = 4
        private const val TOUCH_STATUS_MOVING_WORLD: Int = 5
    }
}

internal class TouchScreen {
    // these matrices will be used to move and zoom image
    private val matrix = android.graphics.Matrix()
    private val savedMatrix = android.graphics.Matrix()
    private var mode = NONE

    // remember some things for zooming
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var d = 0f
    private var newRot = 0f
    private var lastEvent: FloatArray? = null
    fun onTouch(v: View, event: MotionEvent): Boolean {
        // handle touch events here
        val view = v as ImageView
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start[event.x] = event.y
                mode = DRAG
                lastEvent = null
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
                lastEvent = FloatArray(4)
                lastEvent!![0] = event.getX(0)
                lastEvent!![1] = event.getX(1)
                lastEvent!![2] = event.getY(0)
                lastEvent!![3] = event.getY(1)
                d = getRotation(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                lastEvent = null
            }

            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                matrix.set(savedMatrix)
                val dx = event.x - start.x
                val dy = event.y - start.y
                matrix.postTranslate(dx, dy)
            } else if (mode == ZOOM) {
                val newDist = spacing(event)
                if (newDist > 10f) {
                    matrix.set(savedMatrix)
                    val scale = newDist / oldDist
                    matrix.postScale(scale, scale, mid.x, mid.y)
                }
                if (lastEvent != null && event.pointerCount == 3) {
                    newRot = getRotation(event)
                    val r = newRot - d
                    val values = FloatArray(9)
                    matrix.getValues(values)
                    val tx = values[2]
                    val ty = values[5]
                    val sx = values[0]
                    val xc = view.width / 2 * sx
                    val yc = view.height / 2 * sx
                    matrix.postRotate(r, tx + xc, ty + yc)
                }
            }
        }
        view.imageMatrix = matrix
        return true
    }

    /**
     * Determine the space between the first two fingers
     */
    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    /**
     * Calculate the mid point of the first two fingers
     */
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }

    companion object {
        // we can be in one of these 3 states
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2

        /**
         * Calculate the degree to be rotated by.
         *
         * @param event
         * @return Degrees
         */
        fun getRotation(event: MotionEvent): Float {
            val dx = (event.getX(0) - event.getX(1)).toDouble()
            val dy = (event.getY(0) - event.getY(1)).toDouble()
            val radians = atan2(abs(dy), abs(dx))
            val degrees = Math.toDegrees(radians)
            return degrees.toFloat()
        }

        fun getRotation360(event: MotionEvent): Float {
            val dx = (event.getX(0) - event.getX(1)).toDouble()
            val dy = (event.getY(0) - event.getY(1)).toDouble()
            val radians = atan2(abs(dy), abs(dx))
            var degrees = Math.toDegrees(radians)
            if (dx == 0.0 && dy < 0) {
                degrees = 180 - degrees
            } else if (dx < 0 && dy < 0) {
                degrees = 180 - degrees
            } else if (dx < 0 && dy == 0.0) {
                degrees += 180
            } else if (dx < 0 && dy > 0) {
                degrees += 180
            } else if (dx == 0.0 && dy > 0) {
                degrees = 360 - degrees
            } else if (dx > 0 && dy > 0) {
                degrees = 360 - degrees
            }

            return degrees.toFloat()
        }

        fun getSquare(event: MotionEvent): Int {
            val dx = (event.getX(0) - event.getX(1)).toDouble()
            val dy = (event.getY(0) - event.getY(1)).toDouble()
            var square = 1
            if (dx > 0 && dy == 0.0) {
                square = 1
            } else if (dx > 0 && dy < 0) {
                square = 1
            } else if (dx == 0.0 && dy < 0) {
                square = 2
            } else if (dx < 0 && dy < 0) {
                square = 2
            } else if (dx < 0 && dy == 0.0) {
                square = 3
            } else if (dx < 0 && dy > 0) {
                square = 3
            } else if (dx == 0.0 && dy > 0) {
                square = 4
            } else if (dx > 0 && dy > 0) {
                square = 4
            }
            return square
        }
    }
}