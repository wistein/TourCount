/*
 * Copyright © 2016-2024. Wilhelm Stein, Bonn, Germany.
 */
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

/****************************************************
 * Interface for widget_counting_species_notes.xml
 * controls line (below spinner) with edit button and species notes
 * used by CountingActivity
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2024-10-21
 */
class CountingWidgetSpeciesNotes(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val countHead2: TextView
    var count: Count? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_species_notes, this, true)
        countHead2 = findViewById(R.id.countHead2)
    }

    fun setCountHead2(count: Count) {
        // set TextView countHead2
        countHead2.text = count.notes
        // set ImageButton Edit
        val editButton = findViewById<ImageButton>(R.id.buttonEdit)
        editButton.tag = count.id
    }

}