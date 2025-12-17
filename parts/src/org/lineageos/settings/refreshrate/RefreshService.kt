/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.refreshrate

import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

class RefreshService : Service() {

    companion object {
        private const val TAG = "RefreshService"
        private const val DEBUG = true
    }

    private var mPreviousApp: String = ""
    private lateinit var mRefreshUtils: RefreshUtils
    private var mActivityTaskManager = ActivityTaskManager.getService()

    private val mIntentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mPreviousApp = ""
        }
    }

    override fun onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service")
        try {
            mActivityTaskManager = ActivityTaskManager.getService()
            mActivityTaskManager.registerTaskStackListener(mTaskListener)
        } catch (e: RemoteException) {
            // Do nothing
        }
        mRefreshUtils = RefreshUtils(this)
        registerReceiver()
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "Starting service")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(mIntentReceiver, filter)
    }

    private val mTaskListener = object : TaskStackListener() {
        override fun onTaskStackChanged() {
            try {
                val info = mActivityTaskManager.focusedRootTaskInfo ?: return
                val topActivity = info.topActivity ?: return
                val foregroundApp = topActivity.packageName
                val state = mRefreshUtils.getStateForPackage(foregroundApp)

                if (!RefreshUtils.isAppInList) {
                    mRefreshUtils.getOldRate()
                }

                if (foregroundApp != mPreviousApp) {
                    mRefreshUtils.setRefreshRate(foregroundApp)
                    mPreviousApp = foregroundApp
                }

                if (state == RefreshUtils.STATE_60_LAND && RefreshUtils.isAppInList) {
                    mRefreshUtils.checkOrientationAndSetRate(foregroundApp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
