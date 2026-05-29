package com.wmstein.tourcount

import android.os.Bundle

import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs

import java.util.Locale

/**********************************************
 * SettingsFragment used by SettingsActivity.kt
 *
 * Created by wmstein on 2020-04-17
 * last edited in Java on 2020-04-18,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2026-05-26
 */
// Load the preferences from preferences.xml
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs = getPrefs()
    private var language = ""
    private var hasDataLang = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Set language icon
        language = Locale.getDefault().toString().substring(0, 2)
        val langPref: ListPreference? = findPreference("pref_sel_data_lang") // key

        // Set data language option visible to select if DB or species were imported in unknown language
        hasDataLang = prefs.getBoolean("has_data_lang", true)
        langPref?.isEnabled = !hasDataLang // enabled only after import of old DB or species list
        if (hasDataLang)
            langPref?.title = getString(R.string.pref_data_language_ok)
        else
            langPref?.title = getString(R.string.pref_data_language)

        if (!hasDataLang) { // imported old DB or species list: option on to select
            when (language) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de)
                "en" -> langPref?.setIcon(R.drawable.alpha_en)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it)
                "es" -> langPref?.setIcon(R.drawable.alpha_es)
                else -> langPref?.setIcon(R.drawable.alpha_xx) //
            }
        } else { // imported new DB or species list: option off as language is automatically set
            when (language) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de_gr)
                "en" -> langPref?.setIcon(R.drawable.alpha_en_gr)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr_gr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it_gr)
                "es" -> langPref?.setIcon(R.drawable.alpha_es_gr)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
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

        val langPref: ListPreference? = findPreference("pref_sel_data_lang") // key

        if (!hasDataLang) {
            when (language) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de)
                "en" -> langPref?.setIcon(R.drawable.alpha_en)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it)
                "es" -> langPref?.setIcon(R.drawable.alpha_es)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        } else {
            when (language) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de_gr)
                "en" -> langPref?.setIcon(R.drawable.alpha_en_gr)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr_gr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it_gr)
                "es" -> langPref?.setIcon(R.drawable.alpha_es_gr)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        }
    }

}
