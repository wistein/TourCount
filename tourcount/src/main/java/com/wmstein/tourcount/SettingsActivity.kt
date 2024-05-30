package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited in Java on 2023-06-09
 * converted to Kotlin on 2023-07-09
 * last edited on 2023-12-07
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
        if (item.itemId == android.R.id.home) {
            startActivity(
                Intent(
                    this,
                    WelcomeActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

}
