package com.blossom.foldstand.ui.standby

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberAmbientLux(enabled: Boolean): State<Float?> {
    val context = LocalContext.current
    val value = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(enabled, context) {
        if (!enabled) {
            value.value = null
            return@DisposableEffect onDispose { }
        }
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (manager == null || sensor == null) {
            value.value = null
            return@DisposableEffect onDispose { }
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                value.value = event.values.firstOrNull()?.coerceAtLeast(0f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        // The cover WindowArea ComposeView is a separate view tree and may not
        // expose a LifecycleOwner. Register for the lifetime of this composition
        // so the same sensor code works on both the main and presented displays.
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose {
            manager.unregisterListener(listener)
        }
    }
    return value
}
