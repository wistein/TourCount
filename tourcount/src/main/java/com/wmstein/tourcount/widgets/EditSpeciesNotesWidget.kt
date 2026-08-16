package com.wmstein.tourcount.widgets

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.tourcount.R

import java.util.Objects

/*********************************************************
 * EditSpeciesNotesWidget used by EditSpeciesNotesActivity
 *
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2020-09-19,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-08-13
 */
class EditSpeciesNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val specNotesTitle: TextView
    private val specNotesNotes: AutoCompleteTextView

    val suggestions = ArrayList(listOf(*resources.getStringArray(R.array.spec_notes_options)))
    val specNotesAdapter = ArrayAdapter(context, android.R.layout.select_dialog_item,
        suggestions)

    var selectedValue = ""

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_species_notes, this, true)

        specNotesTitle = findViewById(R.id.spNotesTitle) // Notes head
        specNotesNotes = findViewById(R.id.spNotesNotes) // Notes text
        specNotesNotes.setAdapter(specNotesAdapter)
        specNotesNotes.setOnFocusChangeListener {
            _, hasFocus ->
            if (hasFocus)
                specNotesNotes.showDropDown()
        }
        specNotesNotes.setOnItemClickListener {
                parent, _, position, _ ->
            selectedValue = parent.getItemAtPosition(position).toString()
        }

        setSpecNotes(selectedValue)
    }

    fun setSpNotesTitle(title: String) {
        specNotesTitle.text = title
    }

    fun setSpecNotes(note: String) {
        specNotesNotes.text = Editable.Factory.getInstance().newEditable(note)
    }

    var spNotesNotes: String
        get() = specNotesNotes.text.toString()
        set(name) {
            specNotesNotes.setText(name)
        }
		
}
