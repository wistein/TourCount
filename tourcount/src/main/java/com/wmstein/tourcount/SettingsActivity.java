package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.Objects;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited on 2020-04-20
 */
public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean screenOrientL; // option for screen orientation
    
    final private static int REQUEST_CODE_ASK_PERMISSIONS = 123;

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
        screenOrientL = prefs.getBoolean("screen_Orientation", false);

        if (screenOrientL)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        editor = prefs.edit(); // will be committed on pause

        // permission to read db
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasReadStoragePermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (hasReadStoragePermission != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
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
            editor.putString("button_sound", ringtone);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }

}
