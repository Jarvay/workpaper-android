package jarvay.workpaper.wallpaper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ParallaxSensorManager(context: Context) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gameRotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    @Volatile
    var tiltX = 0f; private set

    @Volatile
    var tiltY = 0f; private set

    var sensitivity = 1.0f
    var invertDirection = true

    private var useGameRotationVector = false

    private var targetTiltX = 0f
    private var targetTiltY = 0f
    private var velocityX = 0f
    private var velocityY = 0f

    private val stiffness = 300f
    private val damping = 26f
    private val maxDisplacement = 1.0f

    private var lastTimestamp = 0L
    private var hasGyroscopeData = false
    private var gyroIntegratedX = 0f
    private var gyroIntegratedY = 0f
    private var accelTiltX = 0f
    private var accelTiltY = 0f

    fun start() {
        lastTimestamp = 0L
        targetTiltX = 0f
        targetTiltY = 0f
        velocityX = 0f
        velocityY = 0f
        gyroIntegratedX = 0f
        gyroIntegratedY = 0f
        hasGyroscopeData = false

        if (gameRotationVector != null) {
            useGameRotationVector = true
            sensorManager.registerListener(this, gameRotationVector, SensorManager.SENSOR_DELAY_GAME)
        } else {
            useGameRotationVector = false
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        tiltX = 0f
        tiltY = 0f
        targetTiltX = 0f
        targetTiltY = 0f
        velocityX = 0f
        velocityY = 0f
    }

    fun update() {
        val now = System.nanoTime()
        if (lastTimestamp == 0L) {
            lastTimestamp = now
            return
        }
        val dt = ((now - lastTimestamp) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastTimestamp = now

        val springForceX = stiffness * (targetTiltX - tiltX)
        val springForceY = stiffness * (targetTiltY - tiltY)

        val dampForceX = damping * velocityX
        val dampForceY = damping * velocityY

        velocityX += (springForceX - dampForceX) * dt
        velocityY += (springForceY - dampForceY) * dt

        tiltX = (tiltX + velocityX * dt).coerceIn(-maxDisplacement, maxDisplacement)
        tiltY = (tiltY + velocityY * dt).coerceIn(-maxDisplacement, maxDisplacement)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val direction = if (invertDirection) -1f else 1f

        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.remapCoordinateSystem(
                    rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    remappedMatrix
                )
                SensorManager.getOrientation(remappedMatrix, orientationValues)

                val pitch = orientationValues[1]
                val roll = orientationValues[2]

                targetTiltX = direction * (roll / (Math.PI.toFloat() / 4f)).coerceIn(-1f, 1f)
                targetTiltY = direction * (-pitch / (Math.PI.toFloat() / 4f)).coerceIn(-1f, 1f)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val rawX = (event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val rawY = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                accelTiltX = direction * rawX
                accelTiltY = direction * rawY

                if (!hasGyroscopeData && gyroscope == null) {
                    targetTiltX = accelTiltX
                    targetTiltY = accelTiltY
                } else if (hasGyroscopeData) {
                    val alpha = 0.02f
                    gyroIntegratedX = gyroIntegratedX * (1f - alpha) + accelTiltX * alpha
                    gyroIntegratedY = gyroIntegratedY * (1f - alpha) + accelTiltY * alpha
                    targetTiltX = gyroIntegratedX
                    targetTiltY = gyroIntegratedY
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                hasGyroscopeData = true
                val angularSpeedX = event.values[1]
                val angularSpeedY = event.values[0]

                if (event.timestamp - lastTimestamp > 0) {
                    val dt = (event.timestamp - lastTimestamp) / 1_000_000_000f
                    gyroIntegratedX += direction * angularSpeedY * dt * 0.5f
                    gyroIntegratedY += direction * (-angularSpeedX) * dt * 0.5f
                }

                gyroIntegratedX = gyroIntegratedX.coerceIn(-1f, 1f)
                gyroIntegratedY = gyroIntegratedY.coerceIn(-1f, 1f)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
