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

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.widget.Toast
import org.lineageos.settings.R

object TileUtils {

    @JvmStatic
    fun requestAddTileService(
        context: Context,
        tileServiceClass: Class<*>,
        labelResId: Int,
        iconResId: Int
    ) {
        val componentName = ComponentName(context, tileServiceClass)
        val label = context.getString(labelResId)
        val icon = Icon.createWithResource(context, iconResId)

        val sbm = context.getSystemService(Context.STATUS_BAR_SERVICE) as? StatusBarManager

        sbm?.requestAddTileService(
            componentName,
            label,
            icon,
            context.mainExecutor
        ) { result -> handleResult(context, result) }
    }

    private fun handleResult(context: Context, result: Int?) {
        result ?: return
        val messageResId = when (result) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> R.string.tile_added
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> R.string.tile_not_added
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> R.string.tile_already_added
            else -> return
        }
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    }
}
