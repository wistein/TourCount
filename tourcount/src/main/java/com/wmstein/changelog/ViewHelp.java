package com.wmstein.changelog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import com.wmstein.tourcount.MyDebug;
import com.wmstein.tourcount.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/************************************************************************
 * Based on ChangeLog.java
 * Copyright (C) 2011-2013, Karsten Priegnitz
 * <p>
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 * <p>
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 * <p>
 * author: Karsten Priegnitz
 * <a href="http://code.google.com/p/android-change-log/">...</a>
 * <p>
 * Adaptation for TourCount by wm.stein on 2016-04-18,
 * last edited on 2025-07-01
 */
public class ViewHelp
{
    private static final String TAG = "ViewHelp";
    private final Context context;
    private String thisVersion;
    private static final String NO_VERSION = "";
    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    public ViewHelp(Context context)
    {
        this.context = context;

        // get version name
        try
        {
            thisVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e)
        {
            thisVersion = NO_VERSION;
            if (MyDebug.DLOG)
                Log.e(TAG, "60, Could not get version name from manifest!", e);
        }
    }

    /**
     * Return an AlertDialog with a full change log displayed
     */

    public AlertDialog getDialog()
    {
        WebView wv = new WebView(this.context);

        wv.setBackgroundColor(Color.BLACK);
        wv.loadDataWithBaseURL(null, this.getLog(), "text/html", "UTF-8",
            null);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(this.context, android.R.style.Theme_Material_Dialog));
        builder
            .setTitle(context.getResources().getString(R.string.viewhelp_full_title)
                + " " + thisVersion + ")\n")
            .setView(wv)
            .setCancelable(false)
            // Just an OK button
            .setPositiveButton(
                context.getResources().getString(R.string.ok_button),
                (dialog, which) -> {});
        return builder.create();
    }

    private String getLog()
    {
        // Read viewhelp.txt file
        sb = new StringBuffer();
        try
        {
            String language = Locale.getDefault().toString().substring(0, 2);
            InputStream ins;
            if (language.equals("de"))
                ins = context.getResources().openRawResource(R.raw.viewhelp_de);
            else
                ins = context.getResources().openRawResource(R.raw.viewhelp);
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                char marker = !line.isEmpty() ? line.charAt(0) : 0;
                if (marker == '$')
                {
                    // begin of a version section
                    this.closeList();
                }
                switch (marker)
                {
                    case '%' ->
                    {
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='title'>").append(line.substring(1).trim())
                            .append("</div>\n");
                    }
                    case '_' ->
                    {
                        // line contains version title
                        this.closeList();
                        sb.append("<div class='subtitle'>").append(line.substring(1).trim())
                            .append("</div>\n");
                    }
                    case '!' ->
                    {
                        // line contains free text
                        this.closeList();
                        sb.append("<div class='freetext'>").append(line.substring(1).trim())
                            .append("</div>\n");
                    }
                    case '&' ->
                    {
                        // line contains bold text
                        this.closeList();
                        sb.append("<div class='boldtext'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case ']' ->
                    {
                        // line contains italic text
                        this.closeList();
                        sb.append("<div class='italictext'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case ')' ->
                    {
                        // line contains small text with top and bottom space
                        this.closeList();
                        sb.append("<div class='smalltext'>").append(line.substring(1).trim())
                            .append("</div>\n");
                    }
                    case '}' ->
                    {
                        // line contains small text with top space
                        this.closeList();
                        sb.append("<div class='smalltext1'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case '?' ->
                    {
                        // line contains small text with bottom space
                        this.closeList();
                        sb.append("<div class='textspace'>");
                        sb.append(line.substring(1).trim());
                        sb.append("</div>\n");
                    }
                    case '#' ->
                    {
                        // line contains numbered list item
                        this.openList(Listmode.ORDERED);
                        sb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                    }
                    case '*' ->
                    {
                        // line contains bullet list item
                        this.openList(Listmode.UNORDERED);
                        sb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                    }
                    default ->
                    {
                        // no special character: just use line as is
                        this.closeList();
                        sb.append(line).append(" \n");
                    }
                }
            }
            this.closeList();
            br.close();
            ins.close();
        } catch (IOException e)
        {
            if (MyDebug.DLOG)
                Log.e(TAG, "201, could not read help text.", e);
        }
        return sb.toString();
    }
    // End of getLog()

    private void openList(Listmode listMode)
    {
        if (this.listMode != listMode)
        {
            closeList();
            if (listMode == Listmode.ORDERED)
                sb.append("<div class='list'><ol>\n");
            else if (listMode == Listmode.UNORDERED)
                sb.append("<div class='list'><ul>\n");
            this.listMode = listMode;
        }
    }

    private void closeList()
    {
        if (this.listMode == Listmode.ORDERED)
            sb.append("</ol></div>\n");
        else if (this.listMode == Listmode.UNORDERED)
            sb.append("</ul></div>\n");
        this.listMode = Listmode.NONE;
    }

    /**
     * modes for HTML-Lists (none, numbered, bullet)
     */
    private enum Listmode
    {
        NONE, ORDERED, UNORDERED,
    }

}
