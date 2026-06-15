package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView

import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Count

import java.util.Objects

/******************************************************************
 * Interface for widget_counting_species_notes.xml
 * controls line (below spinner) with edit button and species notes
 * used by CountingActivity
 *
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2026-06-08
 */
class CountingSpeciesNotesWidget(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val speciesNotesEdit: TextView
    var count: Count? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_species_notes, this, true)
        speciesNotesEdit = findViewById(R.id.speciesNotesEdit)
    }

    fun setCountHead2(count: Count) {
        // set TextView speciesNotesEdit
        speciesNotesEdit.text = count.notes
        // set ImageButton Edit
        val editButton = findViewById<ImageButton>(R.id.buttonSpeciesNotes)
        editButton.tag = count.id
    }

}
