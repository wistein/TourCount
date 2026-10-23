package com.wmstein.tourcount

/***************************************************************************************************
 * Copyright © 2025-2026, Wilhelm Stein, Bonn, Germany
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
 * ShowTextDialog.kt
 * displays the html Help and License Dialogs
 *
 * Created by wmstein for TourCount and TransektCount on 2025-07-08,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.util.Linkify
import android.view.MenuItem
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.widget.TextView

import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import com.wmstein.tourcount.Utils.fromHtml

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

class ShowTextDialog : AppCompatActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme_Dark)

        val dialog = intent.getStringExtra("dialog")
        val language = Locale.getDefault().toString().substring(0, 2)

        setContentView(R.layout.activity_dialog)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.show_dialog))
        { vc, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
            // if that's more appropriate.
            vc.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            setStatusBarColor(window, "#303030".toColorInt()) // DarkerGray
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val tvHead = findViewById<TextView>(R.id.help_head)
        val tvText = findViewById<TextView>(R.id.help_text)

        // Help dialog
        if (dialog == "help") {
            supportActionBar!!.setTitle(R.string.title_help_dialog)
            when (language) {
                "de" -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.help_head_de, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.help_de, this))
                }
                else -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.help_head, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.help, this))
                }
            }
        }
        // License dialog
        else if (dialog == "license") {
            supportActionBar!!.setTitle(R.string.title_license_dialog)
            when (language) {
                "de" -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.license_head_de, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.license_de, this))
                }
                "fr" -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.license_head_fr, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.license_fr, this))
                }
                "it" -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.license_head_it, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.license_it, this))
                }
                "es" -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.license_head_es, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.license_es, this))
                }
                else -> {
                    tvHead.text = fromHtml(readRawTextFile(R.raw.license_head, this))
                    tvText.text = fromHtml(readRawTextFile(R.raw.license, this))
                }
            }
        }
        tvText.setLinkTextColor("#4068ff".toColorInt()) // format the links within the text SkyBlue
        Linkify.addLinks(tvText, Linkify.WEB_URLS)
    }

    fun setStatusBarColor(window: Window, color: Int) {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun readRawTextFile(id: Int, context: Context): String? {
        val inputStream = context.resources.openRawResource(id)
        val bReader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        val text = StringBuilder()
        try {
            while ((bReader.readLine().also { line = it }) != null) text.append(line)
        } catch (_: IOException) {
            return null
        }
        return text.toString()
    }

}
