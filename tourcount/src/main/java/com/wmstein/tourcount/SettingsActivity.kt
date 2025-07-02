package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited in Java on 2023-06-09
 * converted to Kotlin on 2023-07-09
 * last edited on 2025-06-30
 */
class SettingsActivity : AppCompatActivity() {
    private var editor: SharedPreferences.Editor? = null
    private var prefs = TourCountApplication.getPrefs()

    @SuppressLint("CommitPrefEdits", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_container))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            setStatusBarColor(window, ContextCompat.getColor(applicationContext,
                    R.color.DarkerGray))
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        //Add preferences from resource (R.xml.preferences)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
        editor = prefs.edit() // will be committed on pause
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun setStatusBarColor(window: Window, color: Int) {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            insets
        }
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
