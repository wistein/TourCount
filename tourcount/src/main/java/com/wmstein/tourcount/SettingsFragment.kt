package com.wmstein.tourcount

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs

/**********************************************
 * SettingsFragment used by SettingsActivity.kt
 *
 * Created by wmstein on 2020-04-17
 * last edited in Java on 2020-04-18,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2026-03-17
 */
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs = getPrefs()
    private var dataLanguage: String? = "de"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from preferences.xml
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Set language icon
        dataLanguage = prefs.getString("pref_sel_data_lang", "de")
        val langPref: ListPreference? = findPreference("pref_sel_data_lang") // key

        when (dataLanguage) {
            "de" -> langPref?.setIcon(R.drawable.alpha_de)
            "en" -> langPref?.setIcon(R.drawable.alpha_en)
            "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
            "it" -> langPref?.setIcon(R.drawable.alpha_it)
            "es" -> langPref?.setIcon(R.drawable.alpha_es)
        }

        // Set proximity option visible if available in device
        val prefProx: Boolean = prefs.getBoolean("enable_prox", false)
        val proxPref: ListPreference? = findPreference("pref_prox")
        proxPref?.isEnabled = prefProx

        if (prefProx)
            proxPref?.setIcon(R.drawable.ic_speaker_phone_black_48dp)
        else
            proxPref?.setIcon(R.drawable.ic_speaker_phone_gray_48dp)

        // Set vibrator option visible if available in device
        val prefVib: Boolean = prefs.getBoolean("enable_vib", false)
        val vibPref: SwitchPreferenceCompat? = findPreference("pref_button_vib")
        vibPref?.isEnabled = prefVib

        if (prefVib)
            vibPref?.setIcon(R.drawable.outline_vibration_48)
        else
            vibPref?.setIcon(R.drawable.outline_vibration_gray_48)
    }

    // Change language icon
    //   unfortunately works only after another PreferenceDialog has been used
    override fun onDisplayPreferenceDialog(langPref: Preference) {
        super.onDisplayPreferenceDialog(langPref)

        dataLanguage = prefs.getString("pref_sel_data_lang", "de")
        val langPref: ListPreference? = findPreference("pref_sel_data_lang") // key

        when (dataLanguage) {
            "de" -> langPref?.setIcon(R.drawable.alpha_de)
            "en" -> langPref?.setIcon(R.drawable.alpha_en)
            "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
            "it" -> langPref?.setIcon(R.drawable.alpha_it)
            "es" -> langPref?.setIcon(R.drawable.alpha_es)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

}
