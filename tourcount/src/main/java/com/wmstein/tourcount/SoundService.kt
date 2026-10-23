package com.wmstein.tourcount

/***************************************************************************************************
 * Copyright © 2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * SoundService produces the button sounds for CountingActivity and EditIndividualActivity
 * in background.
 *
 * Created for TourCount (and TransektCount) by wmstein on 2026-02-24,
 * last edited on 2026-10-19.
 **************************************************************************************************/

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.IBinder
import android.util.Log

import androidx.core.net.toUri

class SoundService : Service {
    private var mContext: Context? = null
    private var audioAttributionContext: Context? = null
    private var uriM: Uri? = null
    private var uriP: Uri? = null
    private var rToneM: MediaPlayer? = null
    private var rToneP: MediaPlayer? = null

    // prefs
    private var buttonSoundMinus = ""
    private var buttonSoundPlus = ""
    private var prefs = TourCountApplication.getPrefs()
    private var buttonSoundPref = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Default constructor is demanded for service declaration in Manifest
    constructor() // not to be removed!

    constructor(context: Context) {
        this.mContext = context
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "61, onCreate")

        audioAttributionContext = mContext!!.createAttributionContext("ringSound")

        buttonSoundPref = prefs.getBoolean("pref_button_sound", false) // make button sound
        buttonSoundMinus = prefs.getString("button_sound_minus", null).toString() // use deeper button sound
        buttonSoundPlus = prefs.getString("button_sound", null).toString() // use button sound
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "73, onStartCommand")

        return START_STICKY
    }

    fun soundMinusButtonSound() {
        if (buttonSoundPref) {
            uriM = if (buttonSoundMinus.isNotBlank())
                buttonSoundMinus.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneM = MediaPlayer.create(audioAttributionContext, uriM)

            if (rToneM!!.isPlaying) {
                rToneM!!.stop()
                rToneM!!.release()
            }
            rToneM!!.start()
        }
    }

    fun soundPlusButtonSound() {
        if (buttonSoundPref) {
            uriP = if (buttonSoundPlus.isNotBlank())
                buttonSoundPlus.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneP = MediaPlayer.create(audioAttributionContext, uriP)

            if (rToneP!!.isPlaying) {
                rToneP!!.stop()
                rToneP!!.release()
            }
            rToneP!!.start()
        }
    }

    // Release (-)-button sound, called by CountingActivity and WelcomeActivity
    fun releaseSoundM() {
        if (buttonSoundPref && rToneM != null) {
            rToneM!!.reset()
            rToneM!!.release()
            rToneM = null
        }
    }

    // Release (+)-button sound, called by EditIndividualActivity and WelcomeActivity
    fun releaseSoundP() {
        if (buttonSoundPref && rToneP != null) {
            rToneP!!.reset()
            rToneP!!.release()
            rToneP = null
        }
    }

    override fun onDestroy() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "132, onDestroy")

        releaseSoundM()
        releaseSoundP()

        super.onDestroy()
    }

    companion object {
        private const val TAG = "SoundService"
    }
}
