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
 * Created by wmstein for com.wmstein.tourcount on 2016-04-19,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2025-11-15
 */
class ResultsMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widgetdate1: TextView
    private val widgetdate2: TextView

    // start_tm
    private val widgetstartTm1: TextView
    private val widgetstartTm2: TextView

    // end_tm
    private val widgetendTm1: TextView
    private val widgetendTm2: TextView

    // temperature
    private val widgettemp1: TextView
    private val widgetstarttemp2: TextView
    private val widgetendtemp2: TextView

    // wind
    private val widgetwind1: TextView
    private val widgetstartwind2: TextView
    private val widgetendwind2: TextView

    // clouds
    private val widgetclouds1: TextView
    private val widgetstartclouds2: TextView
    private val widgetendclouds2: TextView

    private val widgetnotes1: TextView
    private val widgetnotes2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true)
        widgetdate1 = findViewById(R.id.widgetLDate1)
        widgetdate2 = findViewById(R.id.widgetLDate2)

        widgetstartTm1 = findViewById(R.id.widgetLStartTm1)
        widgetstartTm2 = findViewById(R.id.widgetLStartTm2)

        widgetendTm1 = findViewById(R.id.widgetLEndTm1)
        widgetendTm2 = findViewById(R.id.widgetLEndTm2)

        widgettemp1 = findViewById(R.id.widgetLTemp1)
        widgetstarttemp2 = findViewById(R.id.widgetLStartTemp2)
        widgetendtemp2 = findViewById(R.id.widgetLEndTemp2)

        widgetwind1 = findViewById(R.id.widgetLWind1)
        widgetstartwind2 = findViewById(R.id.widgetLStartWind2)
        widgetendwind2 = findViewById(R.id.widgetLEndWind2)

        widgetclouds1 = findViewById(R.id.widgetLClouds1)
        widgetstartclouds2 = findViewById(R.id.widgetLStartClouds2)
        widgetendclouds2 = findViewById(R.id.widgetLEndClouds2)

        widgetnotes1 = findViewById(R.id.widgetNotes1)
        widgetnotes2 = findViewById(R.id.widgetNotes2)
    }

    // Following the SETS
    @SuppressLint("SetTextI18n")
    fun setListMetaWidget(section: Section) {
        widgetdate1.setText(R.string.date)
        widgetdate2.text = section.date

        widgetstartTm1.setText(R.string.starttm)
        widgetstartTm2.text = section.start_tm

        widgetendTm1.setText(R.string.endtm)
        widgetendTm2.text = section.end_tm

        // temperature
        widgettemp1.setText(R.string.temperature)
        if (section.tmp > 0) widgetstarttemp2.text = section.tmp.toString()
        if (section.tmp_end > 0) widgetendtemp2.text = section.tmp_end.toString()

        // wind
        widgetwind1.setText(R.string.wind)
        if (section.wind > 0) widgetstartwind2.text = section.wind.toString()
        if (section.wind_end > 0) widgetendwind2.text = section.wind_end.toString()

        // clouds
        widgetclouds1.setText(R.string.clouds)
        if (section.clouds > 0) widgetstartclouds2.text = section.clouds.toString()
        if (section.clouds_end > 0) widgetendclouds2.text = section.clouds_end.toString()
    }

    fun setWidgetNotes1(name: String?) {
        widgetnotes1.text = name
    }

    fun setWidgetNotes2(name: String?) {
        widgetnotes2.text = name
    }


}
