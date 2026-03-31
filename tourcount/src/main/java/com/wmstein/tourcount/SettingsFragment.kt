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
 * last edited on 2026-03-31
 */
// Load the preferences from preferences.xml
class SettingsFragment : PreferenceFragmentCompat() {
    private var prefs = getPrefs()
    private var dataLanguage: String = ""
    private var newList372: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from preferences.xml
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Set language icon
        dataLanguage = prefs.getString("pref_sel_data_lang", "").toString()
        val langPref: ListPreference? = findPreference("pref_sel_data_lang") // key

        // Set data language option visible if species in unknown language was imported
        newList372 = prefs.getBoolean("new_list_372", true)
        langPref?.isEnabled = !newList372 // enabled only fpr imported old species list
        if (newList372)
            langPref?.title = getString(R.string.pref_data_language_ok)
        else
            langPref?.title = getString(R.string.pref_data_language)

        if (!newList372) { // old species list: option on to select
            when (dataLanguage) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de)
                "en" -> langPref?.setIcon(R.drawable.alpha_en)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it)
                "es" -> langPref?.setIcon(R.drawable.alpha_es)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        } else { // new species list: option off; language automatically set
            when (dataLanguage) {
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

        dataLanguage = prefs.getString("pref_sel_data_lang", "").toString()
        val langPref: ListPreference? = findPreference("pref_sel_data_lang") // key

        if (!newList372) {
            when (dataLanguage) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de)
                "en" -> langPref?.setIcon(R.drawable.alpha_en)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it)
                "es" -> langPref?.setIcon(R.drawable.alpha_es)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        } else {
            when (dataLanguage) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de_gr)
                "en" -> langPref?.setIcon(R.drawable.alpha_en_gr)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr_gr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it_gr)
                "es" -> langPref?.setIcon(R.drawable.alpha_es_gr)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

}
