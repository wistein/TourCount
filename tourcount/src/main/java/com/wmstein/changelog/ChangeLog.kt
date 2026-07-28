package com.wmstein.changelog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.webkit.WebView

import androidx.preference.PreferenceManager

import com.wmstein.tourcount.BuildConfig
import com.wmstein.tourcount.IsRunningOnEmulator
import com.wmstein.tourcount.R
import com.wmstein.tourcount.TourCountApplication.Companion.getPrefs

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import androidx.core.graphics.toColorInt

/**********************************************************************
 * Based on ChangeLog.java, copyright © 2011-2013, Karsten Priegnitz
 * 
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 * 
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 * 
 * Author: Karsten Priegnitz
 * See: https://code.google.com/p/android-change-log/
 * 
 * App newly installed: Shows the history of TourCount.
 * App updated: Shows the last changes of TourCount.
 * 
 * Therefore, retrieves the version names and stores the new version name in SharedPreferences
 * 
 * Adopted for TourCount by wmstein on 2016-04-18,
 * last edited in Java on 2026-05-16,
 * converted to Kotlin on 2026-07-18,
 * last edited on 2026-07-19.
 */
class ChangeLog(private val context: Context, prefs: SharedPreferences) {
    // Get version numbers of lastVersion and thisVersion to compare
    private val lastVersion = prefs.getString(VERSION_KEY, NO_VERSION)!!
    private var thisVersion = ""

    private var listMode: ListMode? = ListMode.NONE
    private var sb: StringBuffer? = null

    private var prefs = getPrefs()
    private var editor = prefs.edit()

    init {
        try {
            thisVersion = context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionName!!
        } catch (e: PackageManager.NameNotFoundException) {
            thisVersion = NO_VERSION

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG,"69, Could not get version name from PackageManager! ",e)
        }
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG,"72, init, last Version: $lastVersion, curr. Version: $thisVersion")
    }

    /**
     * Return true if this version of TourCount is started the first time
     */
    fun firstRun(): Boolean {
        return lastVersion != thisVersion
    }

    /**
     * Return true if TourCount including ChangeLog is started the first time ever.
     * Return also true if TourCount was deinstalled and reinstalled again.
     */
    private fun firstRunEver(): Boolean {
        return NO_VERSION == lastVersion
    }

    /**
     * Return an AlertDialog displaying the changes since the previous installed
     * version of TourCount (what's new). But when this is the first run of TourCount
     * including ChangeLog then the full log dialog is show.
     */
    val logDialog: AlertDialog?
        get() = getDialog(firstRunEver())

    /**
     * Return an AlertDialog with a full change log displayed
     */
    val fullLogDialog: AlertDialog?
        get() = getDialog(true)

    private fun getDialog(full: Boolean): AlertDialog? {
        val wv = WebView(context)

        wv.setBackgroundColor("#1a1a1a".toColorInt()) // DarkGray
        wv.loadDataWithBaseURL(
            null, getLog(full), "text/html",
            "UTF-8", null
        )

        val fullTitle = (context.resources.getString(R.string.changelog_full_title)
                + " " + thisVersion + ")\n")
        val changeTitle = ("Ver. " + thisVersion + ": "
                + context.resources.getString(R.string.changelog_title))

        val builder = AlertDialog.Builder(
            ContextThemeWrapper(context, android.R.style.Theme_Material_Dialog)
        )
        builder
            .setView(wv)
            .setTitle(if (full) fullTitle else changeTitle)
            .setCancelable(false) // OK button
            .setPositiveButton(
                context.resources.getString(R.string.ok_button))
            { _: DialogInterface?, _: Int -> updateVersionInPreferences() }

        if (!full) {
            // "more ..." button
            builder.setNegativeButton(R.string.changelog_show_full)
            { _: DialogInterface?, _: Int -> this.fullLogDialog!!.show() }
        }
        return builder.create()
    }

    // Save new version number to preferences
    private fun updateVersionInPreferences() {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        editor.putString(VERSION_KEY, thisVersion)
        editor.apply()
    }

    /**
     * Return HTML displaying the changes since the previous installed version
     * of TourCount (what's new)
     */
    val log: String
        get() = getLog(false)

    private fun getLog(full: Boolean): String {
        // read changelog.txt file
        sb = StringBuffer()
        try {
            val language = Locale.getDefault().toString().substring(0, 2)
            val ins = if (language == "de") {
                context.resources.openRawResource(R.raw.changelog_de)
            } else {
                context.resources.openRawResource(R.raw.changelog)
            }

            val br = BufferedReader(InputStreamReader(ins))
            var advanceToEOVS = false // if true: ignore further version sections
            var line: String?
            while ((br.readLine().also { line = it }) != null) {
                line = line!!.trim { it <= ' ' }
                val marker = if (!line.isEmpty()) line[0] else 0.toChar()

                // begin of a version section
                if (marker == '$') {
                    closeList()
                    val version = line.substring(1).trim { it <= ' ' }
                    // stop output?
                    if (!full) {
                        if (lastVersion == version) advanceToEOVS = true
                        else if (version == EOCL) advanceToEOVS = false
                    }
                } else if (!advanceToEOVS) {
                    when (marker) {
                        '%' -> {
                            // line contains version title
                            closeList()
                            sb!!.append("<div class='title'>")
                                .append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }

                        '_' -> {
                            // line contains version subtitle
                            closeList()
                            sb!!.append("<div class='subtitle'>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }

                        '!' -> {
                            // line contains free text
                            closeList()
                            sb!!.append("<div class='freetext'>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }

                        ')' -> {
                            // line contains normal text
                            closeList()
                            sb!!.append("<div class='normaltext'>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }

                        '+' -> {
                            // line contains normal text with left margin
                            closeList()
                            sb!!.append("<div class='margtext'>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }

                        '&' -> {
                            // line contains bold text
                            closeList()
                            sb!!.append("<div class='boldtext'>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</div>\n")
                        }

                        '.' -> {
                            // empty line
                            closeList()
                            sb!!.append("<div class='freetext'>")
                            sb!!.append(line.substring(1)).append("<br></div>\n")
                        }

                        '#' -> {
                            // line contains numbered list item
                            openList(ListMode.ORDERED)
                            sb!!.append("<li>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</li>\n")
                        }

                        '*' -> {
                            // line contains bullet list item
                            openList(ListMode.UNORDERED)
                            sb!!.append("<li>")
                            sb!!.append(line.substring(1).trim { it <= ' ' }).append("</li>\n")
                        }

                        else -> {
                            // no special character: just use line as is
                            closeList()
                            sb!!.append(line).append("\n")
                        }
                    }
                }
            }
            closeList()
            br.close()
            ins.close()
        } catch (e: IOException) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG,"256, could not read changelog.",e)
        }

        return sb.toString()
    }

    private fun openList(listMode: ListMode?) {
        if (this.listMode != listMode) {
            closeList()
            if (listMode == ListMode.ORDERED) sb!!.append("<div class='list'><ol>\n")
            else if (listMode == ListMode.UNORDERED) sb!!.append("<div class='list'><ul>\n")
            this.listMode = listMode
        }
    }

    private fun closeList() {
        if (this.listMode == ListMode.ORDERED) sb!!.append("</ol></div>\n")
        else if (this.listMode == ListMode.UNORDERED) sb!!.append("</ul></div>\n")
        this.listMode = ListMode.NONE
    }

    /**
     * Modes for HTML-Lists (none, numbered, bullet)
     */
    private enum class ListMode {
        NONE, ORDERED, UNORDERED,
    }

    companion object {
        private const val TAG = "ChangeLog"

        // key for storing the version name in SharedPreferences
        private const val VERSION_KEY = "PREFS_VERSION_KEY"
        private const val NO_VERSION = "-"
        private const val EOCL = "END_OF_CHANGE_LOG"
    }

}
