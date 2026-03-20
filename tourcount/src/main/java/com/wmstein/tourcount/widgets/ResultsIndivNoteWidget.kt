package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Individuals
import java.util.Objects

/**********************************************************
 * ResultsIndivNoteWidget.kt is used by ShowResultsActivity
 *
 * Created by wmstein on 2018-03-21.
 * Last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-03-02
 */
class ResultsIndivNoteWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val txtBemInd: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_indiv_rem, this, true)
        txtBemInd = findViewById(R.id.txtBemInd)
    }

    fun setRem(individuals: Individuals) {
        txtBemInd.text = individuals.notes
    }

}
