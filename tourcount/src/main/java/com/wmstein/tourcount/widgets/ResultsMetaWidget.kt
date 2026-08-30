package com.wmstein.tourcount.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Section

import java.util.Objects

/*************************************************************
 * ResultsMetaWidget.kt used by ShowResultsActivity.kt
 *
 * Created by wmstein for com.wmstein.tourcount on 2016-04-19,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-08-03
 */
class ResultsMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widgetDateHead: TextView
    private val widgetDatum: TextView

    // start_tm
    private val widgetStartTimeHead: TextView
    private val widgetStartTime: TextView

    // end_tm
    private val widgetEndTimeHead: TextView
    private val widgetEndTime: TextView

    // temperature
    private val widgetTempHead: TextView
    private val widgetstarttemp2: TextView
    private val widgetendtemp2: TextView

    // wind
    private val widgetWindHead: TextView
    private val widgetstartwind2: TextView
    private val widgetendwind2: TextView

    // clouds
    private val widgetCloudsHead: TextView
    private val widgetstartclouds2: TextView
    private val widgetendclouds2: TextView

    private val widgetnotes1: TextView
    private val widgetnotes2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_list_meta, this, true)

        widgetDateHead = findViewById(R.id.widgetLDate1)
        widgetDatum = findViewById(R.id.widgetLDate2)

        widgetStartTimeHead = findViewById(R.id.widgetLStartTm1)
        widgetStartTime = findViewById(R.id.widgetLStartTm2)

        widgetEndTimeHead = findViewById(R.id.widgetLEndTm1)
        widgetEndTime = findViewById(R.id.widgetLEndTm2)

        widgetTempHead = findViewById(R.id.widgetLTemp1)
        widgetstarttemp2 = findViewById(R.id.widgetLStartTemp2)
        widgetendtemp2 = findViewById(R.id.widgetLEndTemp2)

        widgetWindHead = findViewById(R.id.widgetLWind1)
        widgetstartwind2 = findViewById(R.id.widgetLStartWind2)
        widgetendwind2 = findViewById(R.id.widgetLEndWind2)

        widgetCloudsHead = findViewById(R.id.widgetLClouds1)
        widgetstartclouds2 = findViewById(R.id.widgetLStartClouds2)
        widgetendclouds2 = findViewById(R.id.widgetLEndClouds2)

        widgetnotes1 = findViewById(R.id.widgetNotes1)
        widgetnotes2 = findViewById(R.id.widgetNotes2)
    }

    // Following the SETS
    @SuppressLint("SetTextI18n")
    fun setListMetaWidget(section: Section) {
        widgetDateHead.text = context.getString(R.string.date) + ":"
        widgetDatum.text = section.date

        widgetStartTimeHead.setText(R.string.starttm)
        widgetStartTime.text = section.start_tm

        widgetEndTimeHead.setText(R.string.endtm)
        widgetEndTime.text = section.end_tm

        // temperature
        widgetTempHead.text = context.getString(R.string.temperature) + ":"
        if (section.tmp > 0) widgetstarttemp2.text = section.tmp.toString()
        if (section.tmp_end > 0) widgetendtemp2.text = section.tmp_end.toString()

        // wind
        widgetWindHead.text = context.getString(R.string.wind) + ":"
        if (section.wind > 0) widgetstartwind2.text = section.wind.toString()
        if (section.wind_end > 0) widgetendwind2.text = section.wind_end.toString()

        // clouds
        widgetCloudsHead.text = context.getString(R.string.clouds) + ":"
        if (section.clouds > 0) widgetstartclouds2.text = section.clouds.toString()
        if (section.clouds_end > 0) widgetendclouds2.text = section.clouds_end.toString()
    }

    fun setWidgetNotes1(name: String) {
        widgetnotes1.text = name
    }

    fun setWidgetNotes2(name: String) {
        widgetnotes2.text = name
    }

}
