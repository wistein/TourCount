package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/***********************************************************
 * EditSpeciesListHeadWidget used by EditSpeciesListActivity
 *
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2020-09-19,
 * converted to Kotlin on 2024-05-11,
 * last edited on 2026-03-02
 */
class EditSpeciesListHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val splistTitle: TextView
    private val splistName: EditText
    private val notestitle: TextView
    private val notesname: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_head, this, true)
        splistTitle = findViewById(R.id.spListTitle)
        splistName = findViewById(R.id.spListName)
        notestitle = findViewById(R.id.notesTitle)
        notesname = findViewById(R.id.notesName)
    }

}
