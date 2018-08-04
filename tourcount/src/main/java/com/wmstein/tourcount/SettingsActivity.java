package com.wmstein.tourcount;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

/**********************************************************
 * Set the Settings parameters for TourCount
 * Based on SettingsActivity created by milo on 05/05/2014.
 * Adapted for TourCount by wmstein on 2016-05-15,
 * last edited on 2018-08-04
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final int SELECT_PICTURE = 1; // requestCode 1
    private static final int GET_SOUND = 10; // requestCode 10
    private static final String TAG = "tourcountPrefAct";
    SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean screenOrientL; // option for screen orientation
    private Uri alert_button_uri;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    @SuppressLint("CommitPrefEdits")
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
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

        Preference button = findPreference("button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                getImage();
                return true;
            }
        });

        // Sound for keypresses
        String strButtonSoundPreference = prefs.getString("alert_button_sound", "DEFAULT_SOUND");
        alert_button_uri = Uri.parse(strButtonSoundPreference);

        Preference alert_button_sound = findPreference("alert_button_sound");
        alert_button_sound.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference arg0)
            {
                getSound(alert_button_uri, GET_SOUND);
                return true;
            }
        });

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
        editor.commit();
    }

    private void getImage()
    {
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(pickIntent, SELECT_PICTURE);
    }

    private void getSound(Uri tmp_alert_uri, int requestCode)
    {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.pref_button_sound));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, tmp_alert_uri);
        this.startActivityForResult(intent, requestCode);
    }

    @Override
    @SuppressLint({"CommitPrefEdits", "LongLogTag"})
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == SELECT_PICTURE && data != null && data.getData() != null)
        {
            Uri _uri = Uri.parse(data.getDataString());

            if (_uri != null) //User did pick an image.
            {
                /*
                 * The try is here because this action fails if the user uses a file manager; the gallery
                 * seems to work nicely, though.
                 */
                Cursor cursor = null;
                try
                {
                    cursor = getContentResolver().query(_uri, new String[]
                        {android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
                }
                catch (java.lang.SecurityException e)
                {
                    Toast.makeText(this, getString(R.string.permission_please), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                try
                {
                    cursor.moveToFirst(); // blows up here if file manager used
                } catch (Exception e)
                {
                    if (MyDebug.LOG)
                        Log.e(TAG, "Failed to select image: " + e.toString());
                    Toast.makeText(this, getString(R.string.image_error), Toast.LENGTH_LONG).show();
                    return;
                }

                //Link to the image
                String imageFilePath = cursor.getString(0);
                cursor.close();

                // save the image path
                editor.putString("imagePath", imageFilePath);
            }
        }
        else if (resultCode == Activity.RESULT_OK)
        {
            Uri uri = null;
            if (data != null)
            {
                uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            }
            String ringtone;
            if (uri != null)
            {
                ringtone = uri.toString();
                if (requestCode == GET_SOUND)
                {
                    editor.putString("alert_button_sound", ringtone);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case android.R.id.home:
            startActivity(new Intent(this, WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        screenOrientL = prefs.getBoolean("screen_Orientation", false);
    }

}
