package com.wmstein.tourcount;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

/**
 * SettingsFragment
 * Created by wmstein on 2020-04-17
 * last edited on 2020-04-18
 */
public class SettingsFragment extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        // Load the preferences from preferences.xml
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

}
