package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.MyDebug
import com.wmstein.tourcount.R
import java.util.Objects

/**********************************
 * NotesWidget used by CountingActivity
 * shows scalable notes line
 * Created by milo on 26/05/2014.
 * Adopted for TourCount by wmstein on 2016-02-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2024-07-11
 */
class NotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val textView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_notes, this, true)
        textView = findViewById(R.id.notes_text)
    }

    fun setNotes(notes: String?) {
        textView.text = notes
    }

    fun setFont(large: Boolean) {
        if (large) {
            if (MyDebug.LOG) Log.d(TAG, "Setzt große Schrift.")
            textView.textSize = 16f
        } else {
            if (MyDebug.LOG) Log.d(TAG, "Setzt kleine Schrift.")
            textView.textSize = 14f
        }
    }

    companion object {
        private const val TAG = "TourCntNotesWidget"
    }

}
