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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberAmbientLux(enabled: Boolean): State<Float?> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val value = remember { mutableStateOf<Float?>(null) }
    DisposableEffect(enabled, lifecycle, context) {
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
        fun register() {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        fun unregister() {
            manager.unregisterListener(listener)
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> register()
                Lifecycle.Event.ON_STOP -> unregister()
                else -> Unit
            }
        }
        lifecycle.addObserver(lifecycleObserver)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) register()
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            unregister()
        }
    }
    return value
}
