package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited in Java on 2023-06-09
 * converted to Kotlin on 2023-07-09
 * last edited on 2024-07-23
 */
class SettingsActivity : AppCompatActivity() {
    private var editor: SharedPreferences.Editor? = null
    private var prefs = TourCountApplication.getPrefs()

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //add Preferences From Resource (R.xml.preferences)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        editor = prefs.edit() // will be committed on pause

        // new onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    NavUtils.navigateUpFromSameTask(this@SettingsActivity)
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()

        var ringtone: String

        val buttonSoundUri =
            Uri.parse("android.resource://com.wmstein.tourcount/" + R.raw.button)
        ringtone = buttonSoundUri.toString()
        editor?.putString("button_sound", ringtone)

        val buttonSoundUriM =
            Uri.parse("android.resource://com.wmstein.tourcount/" + R.raw.button_minus)
        ringtone = buttonSoundUriM.toString()
        editor?.putString("button_sound_minus", ringtone)
        editor?.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        if (item.itemId == android.R.id.home) {
            super.finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        NavUtils.navigateUpFromSameTask(this)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

}
