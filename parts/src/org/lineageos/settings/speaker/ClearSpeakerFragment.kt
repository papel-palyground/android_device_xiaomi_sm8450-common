/*
 * Copyright (C) 2020 Paranoid Android
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

package org.lineageos.settings.speaker

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.preference.Preference
import androidx.preference.PreferenceFragment
import androidx.preference.SwitchPreferenceCompat

import org.lineageos.settings.R

import java.io.IOException

class ClearSpeakerFragment : PreferenceFragment(), Preference.OnPreferenceChangeListener {

    companion object {
        private val TAG = ClearSpeakerFragment::class.java.simpleName
        private const val PREF_CLEAR_SPEAKER = "clear_speaker_pref"
    }

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var clearSpeakerPref: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.clear_speaker_settings)

        clearSpeakerPref = findPreference(PREF_CLEAR_SPEAKER) as SwitchPreferenceCompat
        clearSpeakerPref.onPreferenceChangeListener = this

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference == clearSpeakerPref) {
            val value = newValue as Boolean
            if (value) {
                if (startPlaying()) {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed({ stopPlaying() }, 30000)
                    return true
                }
            }
        }
        return false
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
    }

    private fun startPlaying(): Boolean {
        audioManager.setParameters("status_earpiece_clean=on")
        mediaPlayer = MediaPlayer()
        activity.volumeControlStream = AudioManager.STREAM_MUSIC
        @Suppress("DEPRECATION")
        mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer?.isLooping = true
        
        return try {
            resources.openRawResourceFd(R.raw.clear_speaker_sound).use { file ->
                mediaPlayer?.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            }
            clearSpeakerPref.isEnabled = false
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
            true
        } catch (ioe: IOException) {
            Log.e(TAG, "Failed to play speaker clean sound!", ioe)
            false
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.reset()
                it.release()
            }
            mediaPlayer = null
        }
        audioManager.setParameters("status_earpiece_clean=off")
        clearSpeakerPref.isEnabled = true
        clearSpeakerPref.isChecked = false
    }
}
