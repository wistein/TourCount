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
 * ListMetaWidget.kt used by ShowResultsActivity.kt
 * Created by wmstein for com.wmstein.tourcount on 2016-04-19,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2025-03-04
 */
class ListMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widget_ldate1: TextView
    private val widget_ldate2: TextView

    // start_tm
    private val widget_lstartTm1: TextView
    private val widget_lstartTm2: TextView

    // end_tm
    private val widget_lendTm1: TextView
    private val widget_lendTm2: TextView

    // temperature
    private val widget_ltemp1: TextView
    private val widget_lstarttemp2: TextView
    private val widget_lendtemp2: TextView

    // wind
    private val widget_lwind1: TextView
    private val widget_lstartwind2: TextView
    private val widget_lendwind2: TextView

    // clouds
    private val widget_lclouds1: TextView
    private val widget_lstartclouds2: TextView
    private val widget_lendclouds2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true)
        widget_ldate1 = findViewById(R.id.widgetLDate1)
        widget_ldate2 = findViewById(R.id.widgetLDate2)

        widget_lstartTm1 = findViewById(R.id.widgetLStartTm1)
        widget_lstartTm2 = findViewById(R.id.widgetLStartTm2)

        widget_lendTm1 = findViewById(R.id.widgetLEndTm1)
        widget_lendTm2 = findViewById(R.id.widgetLEndTm2)

        widget_ltemp1 = findViewById(R.id.widgetLTemp1)
        widget_lstarttemp2 = findViewById(R.id.widgetLStartTemp2)
        widget_lendtemp2 = findViewById(R.id.widgetLEndTemp2)

        widget_lwind1 = findViewById(R.id.widgetLWind1)
        widget_lstartwind2 = findViewById(R.id.widgetLStartWind2)
        widget_lendwind2 = findViewById(R.id.widgetLEndWind2)

        widget_lclouds1 = findViewById(R.id.widgetLClouds1)
        widget_lstartclouds2 = findViewById(R.id.widgetLStartClouds2)
        widget_lendclouds2 = findViewById(R.id.widgetLEndClouds2)
    }

    // Following the SETS
    @SuppressLint("SetTextI18n")
    fun setListMetaWidget(section: Section) {
        widget_ldate1.setText(R.string.date)
        widget_ldate2.text = section.date

        widget_lstartTm1.setText(R.string.starttm)
        widget_lstartTm2.text = section.start_tm

        widget_lendTm1.setText(R.string.endtm)
        widget_lendTm2.text = section.end_tm

        // temperature
        widget_ltemp1.setText(R.string.temperature)
        widget_lstarttemp2.text = section.tmp.toString()
        widget_lendtemp2.text = section.tmp_end.toString()

        // wind
        widget_lwind1.setText(R.string.wind)
        widget_lstartwind2.text = section.wind.toString()
        widget_lendwind2.text = section.wind_end.toString()

        // clouds
        widget_lclouds1.setText(R.string.clouds)
        widget_lstartclouds2.text = section.clouds.toString()
        widget_lendclouds2.text = section.clouds_end.toString()
    }

}
