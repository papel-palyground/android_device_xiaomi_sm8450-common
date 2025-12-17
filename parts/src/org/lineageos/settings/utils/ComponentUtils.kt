/*
 * Copyright (C) 2024 The LineageOS Project
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

package org.lineageos.settings.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object ComponentUtils {

    /**
     * Enables or disables a specified Android component dynamically at runtime.
     *
     * @param context The context from which the component will be enabled or disabled.
     * @param componentClass The class of the component to be enabled or disabled.
     * @param enable If true, the component will be enabled; if false, it will be disabled.
     */
    @JvmStatic
    fun toggleComponent(context: Context, componentClass: Class<*>, enable: Boolean) {
        val componentName = ComponentName(context, componentClass)
        val packageManager = context.packageManager
        val currentState = packageManager.getComponentEnabledSetting(componentName)
        val newState = if (enable) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        if (currentState != newState) {
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
