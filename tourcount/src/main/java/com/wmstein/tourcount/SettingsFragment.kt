package com.wmstein.tourcount

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs

/**
 * SettingsFragment
 * Created by wmstein on 2020-04-17
 * last edited in Java on 2020-04-18,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2025-04-29
 */
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from preferences.xml
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Get preferences
        prefs = getPrefs()

        // Set proximity option visible if available in device
        val prefProx: Boolean = prefs!!.getBoolean("enable_prox", false)
        val proxPref: ListPreference? = findPreference("pref_prox")
        if (prefProx) {
            proxPref?.isEnabled = true
        }
        else {
            proxPref?.isEnabled = false
        }

    }

}
