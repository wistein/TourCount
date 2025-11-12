/*
 * Copyright © 2016-2025. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Section
import java.util.Objects

/****************************************************
 * Interface for widget_counting_tour_notes.xml
 * controls bottom line with edit button and tour notes
 * used by CountingActivity
 * Created by wmstein 2025-09-16,
 * last edited on 2025-09-16
 */
class CountingWidgetTourNotes(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val tourNotesEdit: TextView
    var section: Section? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_tour_notes, this, true)
        tourNotesEdit = findViewById(R.id.tourNotesEdit)
    }

    fun setTourNotes(section: Section) {
        // set TextView tourNotes
        tourNotesEdit.text = section.notes
        // set ImageButton Edit
        val editButton = findViewById<ImageButton>(R.id.buttonTourNotes)
        editButton.tag = section.id
    }

}