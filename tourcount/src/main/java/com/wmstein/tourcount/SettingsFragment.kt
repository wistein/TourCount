package com.wmstein.tourcount

/***************************************************************************************************
 * Copyright © 2020-2026, Wilhelm Stein, Bonn, Germany
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
 * SettingsFragment.kt used by SettingsActivity.kt
 *
 * Load the preferences from preferences.xml and sets
 * - proximity option visible
 * - vibrator option visible
 * - icon for the data language
 *
 * Created by wmstein on 2020-04-17
 * last edited in Java on 2020-04-18,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat

import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs

import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private var prefs = getPrefs()
    var langPref: ListPreference? = findPreference("pref_sel_data_lang")
    private var sysLanguage = ""
    private var hasDataLang = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "54, onCreatePreferences")

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
    // End of onCreatePreferences

    override fun onResume() {
        super.onResume()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "82, onResume")

        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)

        // Set sysLanguage icon
        sysLanguage = Locale.getDefault().toString().substring(0, 2)
        langPref = findPreference("pref_sel_data_lang")

        // Set data language option visible to select
        //   if DB or species were imported in an unknown language
        hasDataLang = prefs.getBoolean("has_data_lang", true)
        langPref?.isEnabled = !hasDataLang // enabled only after import of old DB or species list
        if (hasDataLang)
            langPref?.title = getString(R.string.pref_data_language_ok) // automaticly detected
        else
            langPref?.title = getString(R.string.pref_data_language)

        if (hasDataLang) { // Imported a new DB or species list: option is grayed as language is automatically set
            when (sysLanguage) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de_gr)
                "en" -> langPref?.setIcon(R.drawable.alpha_en_gr)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr_gr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it_gr)
                "es" -> langPref?.setIcon(R.drawable.alpha_es_gr)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        } else { // Imported old DB or species list: option is on to select
            when (sysLanguage) {
                "de" -> langPref?.setIcon(R.drawable.alpha_de)
                "en" -> langPref?.setIcon(R.drawable.alpha_en)
                "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
                "it" -> langPref?.setIcon(R.drawable.alpha_it)
                "es" -> langPref?.setIcon(R.drawable.alpha_es)
                else -> langPref?.setIcon(R.drawable.alpha_xx)
            }
        }
    }
    // End of onResume

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "123, onSharedPreferenceChanged, hasDataLang: $hasDataLang")

        // Change language icon
        if (key == "pref_sel_data_lang") {
            langPref = findPreference("pref_sel_data_lang")

            if (hasDataLang) {
                when (sysLanguage) {
                    "de" -> langPref?.setIcon(R.drawable.alpha_de_gr)
                    "en" -> langPref?.setIcon(R.drawable.alpha_en_gr)
                    "fr" -> langPref?.setIcon(R.drawable.alpha_fr_gr)
                    "it" -> langPref?.setIcon(R.drawable.alpha_it_gr)
                    "es" -> langPref?.setIcon(R.drawable.alpha_es_gr)
                    else -> langPref?.setIcon(R.drawable.alpha_xx)
                }
            } else {
                when (sysLanguage) {
                    "de" -> langPref?.setIcon(R.drawable.alpha_de)
                    "en" -> langPref?.setIcon(R.drawable.alpha_en)
                    "fr" -> langPref?.setIcon(R.drawable.alpha_fr)
                    "it" -> langPref?.setIcon(R.drawable.alpha_it)
                    "es" -> langPref?.setIcon(R.drawable.alpha_es)
                    else -> langPref?.setIcon(R.drawable.alpha_xx)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    companion object {
        private const val TAG = "SettingsFragm"
    }

}
