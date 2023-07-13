package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/****************************************************
 * ListSumWidget shows count totals area for
 * ListSpeciesActivity that shows the result page
 * Created for TourCount by wmstein on 2017-05-27,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-05
 */
class ListSumWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val sumSpecies: TextView
    private val sumIndividuals: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_sum_species, this, true)
        sumSpecies = findViewById(R.id.sumSpecies)
        sumIndividuals = findViewById(R.id.sumIndividuals)
    }

    fun setSum(sumsp: Int, sumind: Int) {
        sumSpecies.text = sumsp.toString()
        sumIndividuals.text = sumind.toString()
    }
}