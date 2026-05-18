package com.wmstein.tourcount

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log

import androidx.core.net.toUri

/*****************************************************************************************
 * SoundService produces the button sounds for CountingActivity and EditIndividualActivity
 * in background.
 *
 * Created for TourCount (and TransektCount) by wmstein on 2026-02-24,
 * last edited on 2026-05-19
 */
class SoundService : Service {
    private var mContext: Context? = null
    private var audioAttributionContext: Context? = null
    private var uriM: Uri? = null
    private var uriP: Uri? = null
    private var rToneM: MediaPlayer? = null
    private var rToneP: MediaPlayer? = null

    // prefs
    private var buttonSoundMinus: String = ""
    private var buttonSoundPlus: String = ""
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
            Log.i(TAG, "46, onCreate")

        audioAttributionContext = if (Build.VERSION.SDK_INT >= 30)
            mContext!!.createAttributionContext("ringSound")
        else mContext

        buttonSoundPref = prefs.getBoolean("pref_button_sound", false) // make button sound
        buttonSoundMinus = prefs.getString("button_sound_minus", null).toString() // use deeper button sound
        buttonSoundPlus = prefs.getString("button_sound", null).toString() // use button sound
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "60, onStartCommand")

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
            Log.i(TAG, "119, onDestroy")

        releaseSoundM()
        releaseSoundP()

        super.onDestroy()
    }

    companion object {
        private const val TAG = "SoundService"
    }
}
