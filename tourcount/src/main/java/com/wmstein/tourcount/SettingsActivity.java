package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Objects;

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited on 2023-05-08
 */
public class SettingsActivity extends AppCompatActivity
{
    SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    @Override
    @SuppressLint({"CommitPrefEdits", "SourceLockedOrientationActivity"})
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.settings);

        //add Preferences From Resource (R.xml.preferences);
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.settings_container, new SettingsFragment())
            .commit();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit(); // will be committed on pause
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        String ringtone;
        boolean buttonSoundPref = prefs.getBoolean("pref_button_sound", false);

        if (buttonSoundPref)
        {
            Uri button_sound_uri = Uri.parse("android.resource://com.wmstein.tourcount/" + R.raw.button);
            ringtone = button_sound_uri.toString();
            editor.putString("alert_button_sound", ringtone);
        }

        editor.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == android.R.id.home)
        {
            startActivity(new Intent(this, WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
        else
        {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

}
