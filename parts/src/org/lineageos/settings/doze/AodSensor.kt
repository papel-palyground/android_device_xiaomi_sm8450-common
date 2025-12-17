/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.doze

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.concurrent.Executors

class AodSensor(context: Context) : SensorEventListener {

    companion object {
        private const val DEBUG = false
        private const val TAG = "AodSensor"
    }

    private val sensorManager: SensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = DozeUtils.getSensor(sensorManager, "xiaomi.sensor.aod")
    private val executorService = Executors.newSingleThreadExecutor()

    override fun onSensorChanged(event: SensorEvent) {
        if (DEBUG) Log.d(TAG, "Got sensor event: ${event.values[0]}")

        when (event.values[0]) {
            3f, 5f -> DozeUtils.setDozeMode(DozeUtils.DOZE_MODE_LBM)
            4f -> DozeUtils.setDozeMode(DozeUtils.DOZE_MODE_HBM)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Empty
    }

    fun enable() {
        if (DEBUG) Log.d(TAG, "Enabling")
        executorService.submit {
            sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    fun disable() {
        if (DEBUG) Log.d(TAG, "Disabling")
        executorService.submit {
            sensor?.let { sensorManager.unregisterListener(this, it) }
        }
    }
}
