package com.wmstein.tourcount

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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.tourcount.Utils.fromHtml
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

/****************************************************************
 * ShowTextDialog.kt
 * Custom class for displaying the Help and License Dialogs
 *
 * Adopted 2025 by wistein for TourCount and TransektCount,
 * last edited on 2025-07-15
 */
class ShowTextDialog : AppCompatActivity () {
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
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
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
            setStatusBarColor(window, ContextCompat.getColor(applicationContext,
                R.color.DarkerGray))
        }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val tvHead = findViewById<TextView>(R.id.help_head)
        val tvText = findViewById<TextView>(R.id.help_text)
        // Help dialog
        if (dialog == "help") {
            supportActionBar!!.setTitle(R.string.title_help_dialog)
            if (language == "de") {
                tvHead.text = fromHtml(readRawTextFile(R.raw.help_head_de, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.help_de, this))
            } else {
                tvHead.text = fromHtml(readRawTextFile(R.raw.help_head, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.help, this))
            }
        }
        // License dialog
        else if (dialog == "license") {
            supportActionBar!!.setTitle(R.string.title_license_dialog)
            if (language == "de") {
                tvHead.text = fromHtml(readRawTextFile(R.raw.license_head_de, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.license_de, this))
            } else {
                tvHead.text = fromHtml(readRawTextFile(R.raw.license_head, this))
                tvText.text = fromHtml(readRawTextFile(R.raw.license, this))
            }
        }
        tvText.setLinkTextColor(getColor(R.color.SkyBlue)) // format the links within the text
        Linkify.addLinks(tvText, Linkify.WEB_URLS)
    }

    @RequiresApi(Build.VERSION_CODES.R)
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

    companion object {
        private fun readRawTextFile(id: Int, context: Context): String? {
            val inputStream = context.resources.openRawResource(id)
            val `in` = InputStreamReader(inputStream)
            val buf = BufferedReader(`in`)
            var line: String?
            val text = StringBuilder()
            try {
                while ((buf.readLine().also { line = it }) != null) text.append(line)
            } catch (_: IOException) {
                return null
            }
            return text.toString()
        }
    }

}
