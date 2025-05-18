package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited in Java on 2023-06-09
 * converted to Kotlin on 2023-07-09
 * last edited on 2025-05-01
 */
class SettingsActivity : AppCompatActivity() {
    private var editor: SharedPreferences.Editor? = null
    private var prefs = TourCountApplication.getPrefs()

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Add preferences from resource (R.xml.preferences)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        editor = prefs.edit() // will be committed on pause
    }

    override fun onPause() {
        super.onPause()

        var ringtone: String

        val buttonSoundUri =
            ("android.resource://com.wmstein.tourcount/" + R.raw.button).toUri()
        ringtone = buttonSoundUri.toString()
        editor?.putString("button_sound", ringtone)

        val buttonSoundUriM =
            ("android.resource://com.wmstein.tourcount/" + R.raw.button_minus).toUri()
        ringtone = buttonSoundUriM.toString()
        editor?.putString("button_sound_minus", ringtone)
        editor?.commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
