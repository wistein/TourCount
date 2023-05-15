package sheetrock.panda.changelog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import androidx.preference.PreferenceManager;

import com.wmstein.tourcount.MyDebug;
import com.wmstein.tourcount.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/************************************************************************
 * Copyright (C) 2011-2013, Karsten Priegnitz
 * <p/>
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 * <p/>
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 * <p/>
 * Adaptation for ViewHelp:
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany,
 * last edited on 2020-04-17
 */
public class ChangeLog
{
    private static final String TAG = "ChangeLog";
    // key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";
    private static final String NO_VERSION = "";
    private static final String EOCL = "END_OF_CHANGE_LOG";
    private final Context context;
    private final String lastVersion;
    private String thisVersion;
    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    /**
     * Constructor <p/>
     * Retrieves the version names and stores the new version name in SharedPreferences
     */
    public ChangeLog(Context context)
    {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * Constructor <p/>
     * Retrieves the version names and stores the new version name in SharedPreferences
     *
     * @param sp the shared preferences to store the last version name into
     */
    private ChangeLog(Context context, SharedPreferences sp)
    {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION);
        if (MyDebug.LOG)
            Log.d(TAG, "lastVersion: " + lastVersion);
        try
        {
            this.thisVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e)
        {
            this.thisVersion = NO_VERSION;
            if (MyDebug.LOG)
                Log.e(TAG, "could not get version name from manifest!", e);
        }
        if (MyDebug.LOG)
            Log.d(TAG, "appVersion: " + this.thisVersion);
    }

    /**
     * @return <code>true</code> if this version of your app is started the
     * first time
     */
    public boolean firstRun()
    {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return <code>true</code> if your app including ChangeLog is started the
     * first time ever. Also <code>true</code> if your app was
     * deinstalled and installed again.
     */
    private boolean firstRunEver()
    {
        return NO_VERSION.equals(this.lastVersion);
    }

    /**
     * @return An AlertDialog displaying the changes since the previous
     * installed version of your app (what's new). But when this is the
     * first run of your app including ChangeLog then the full log
     * dialog is show.
     */
    public AlertDialog getLogDialog()
    {
        return this.getDialog(this.firstRunEver());
    }

    /*********************************************************
     * @return an AlertDialog with a full change log displayed
     */
    public AlertDialog getFullLogDialog()
    {
        return this.getDialog(true);
    }

    private AlertDialog getDialog(boolean full)
    {
        WebView wv = new WebView(this.context);

        wv.setBackgroundColor(Color.BLACK);
        wv.loadDataWithBaseURL(null, this.getLog(full), "text/html", "UTF-8",
            null);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(
                this.context, android.R.style.Theme_Holo_Dialog));
        builder.setTitle(
                context.getResources().getString(
                    full ? R.string.changelog_full_title
                        : R.string.changelog_title))
            .setView(wv)
            .setCancelable(false)
            // OK button
            .setPositiveButton(
                context.getResources().getString(
                    R.string.ok_button),
                (dialog, which) -> updateVersionInPreferences());

        if (!full)
        {
            // "more ..." button
            builder.setNegativeButton(R.string.changelog_show_full,
                (dialog, id) -> getFullLogDialog().show());
        }

        return builder.create();
    }

    @SuppressLint("CommitPrefEdits")
    private void updateVersionInPreferences()
    {
        // save new version number to preferences
        SharedPreferences sp = PreferenceManager
            .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, thisVersion);
        editor.apply();
    }

    /**************************************************************************
     * @return HTML displaying the changes since the previous installed version
     *         of your app (what's new)
     */
    public String getLog()
    {
        return this.getLog(false);
    }

    private String getLog(boolean full)
    {
        // read changelog.txt file
        sb = new StringBuffer();
        try
        {
            String language = Locale.getDefault().toString().substring(0, 2);
            InputStream ins;
            if (language.equals("de"))
            {
                ins = context.getResources().openRawResource(R.raw.changelog_de);
            }
            else
            {
                ins = context.getResources().openRawResource(R.raw.changelog);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            boolean advanceToEOVS = false; // if true: ignore further version sections
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$')
                {
                    // begin of a version section
                    this.closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full)
                    {
                        if (this.lastVersion.equals(version))
                        {
                            advanceToEOVS = true;
                        }
                        else if (version.equals(EOCL))
                        {
                            advanceToEOVS = false;
                        }
                    }
                }
                else if (!advanceToEOVS)
                {
                    switch (marker)
                    {
                        case '%':
                            // line contains version title
                            this.closeList();
                            sb.append("<div class='title'>").append(line.substring(1).trim()).append("</div>\n");
                            break;
                        case '&':
                            // line contains bold red text
                            this.closeList();
                            sb.append("<div class='boldredtext'>");
                            sb.append(line.substring(1).trim());
                            sb.append("</div>\n");
                            break;
                        case '_':
                            // line contains version title
                            this.closeList();
                            sb.append("<div class='subtitle'>").append(line.substring(1).trim()).append("</div>\n");
                            break;
                        case '!':
                            // line contains free text
                            this.closeList();
                            sb.append("<div class='freetext'>").append(line.substring(1).trim()).append("</div>\n");
                            break;
                        case '#':
                            // line contains numbered list item
                            this.openList(Listmode.ORDERED);
                            sb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                            break;
                        case '*':
                            // line contains bullet list item
                            this.openList(Listmode.UNORDERED);
                            sb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                            break;
                        default:
                            // no special character: just use line as is
                            this.closeList();
                            sb.append(line).append("\n");
                    }
                }
            }
            this.closeList();
            br.close();
        } catch (IOException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "could not read changelog.", e);
        }

        return sb.toString();
    }

    private void openList(Listmode listMode)
    {
        if (this.listMode != listMode)
        {
            closeList();
            if (listMode == Listmode.ORDERED)
            {
                sb.append("<div class='list'><ol>\n");
            }
            else if (listMode == Listmode.UNORDERED)
            {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList()
    {
        if (this.listMode == Listmode.ORDERED)
        {
            sb.append("</ol></div>\n");
        }
        else if (this.listMode == Listmode.UNORDERED)
        {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }

    /**
     * modes for HTML-Lists (bullet, numbered)
     */
    private enum Listmode
    {
        NONE, ORDERED, UNORDERED,
    }

}
