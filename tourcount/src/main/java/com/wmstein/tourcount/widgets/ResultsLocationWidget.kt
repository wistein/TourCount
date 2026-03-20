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
import kotlin.math.roundToInt

/*************************************************************
 * ResultsLocationWidget.kt used by ShowResultsActivity.java
 *
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2026-03-02
 */
class ResultsLocationWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // country
    private val widgetCoT: TextView
    private val widgetCoN: TextView

    // state
    private val widgetstate1: TextView
    private val widgetstate2: TextView

    // plz
    private val widgetplz1: TextView
    private val widgetplz2: TextView

    // city
    private val widgetcity1: TextView
    private val widgetcity2: TextView

    // place
    private val widgetplace1: TextView
    private val widgetplace2: TextView

    // locality
    private val widgetlocality1: TextView
    private val widgetlocality2: TextView

    // average longitude
    private val widgetdlo1: TextView
    private val widgetdlo2: TextView

    // average latitude
    private val widgetdla1: TextView
    private val widgetdla2: TextView

    // mean uncertainty
    private val widgetuncert1: TextView
    private val widgetuncert2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_location, this, true)
        widgetCoT = findViewById(R.id.widgetLCountryTitle) // Country
        widgetCoN = findViewById(R.id.widgetLCountryName)
        widgetstate1 = findViewById(R.id.widgetState1)
        widgetstate2 = findViewById(R.id.widgetState2)
        widgetplz1 = findViewById(R.id.widgetLPlz1) // plz
        widgetplz2 = findViewById(R.id.widgetLPlz2)
        widgetcity1 = findViewById(R.id.widgetLCity1) // city
        widgetcity2 = findViewById(R.id.widgetLCity2)
        widgetplace1 = findViewById(R.id.widgetLPlace1) // place
        widgetplace2 = findViewById(R.id.widgetLPlace2)
        widgetlocality1 = findViewById(R.id.widgetLoc1)
        widgetlocality2 = findViewById(R.id.widgetLoc2)
        widgetdlo1 = findViewById(R.id.widgetLdlo1) // lon
        widgetdlo2 = findViewById(R.id.widgetLdlo2)
        widgetdla1 = findViewById(R.id.widgetLdla1) // lat
        widgetdla2 = findViewById(R.id.widgetLdla2)
        widgetuncert1 = findViewById(R.id.widgetLmuncert1) // uncert
        widgetuncert2 = findViewById(R.id.widgetLmuncert2)
    }

    // Following the SETS
    fun setLocationWidget(section: Section) {
        widgetCoT.setText(R.string.country)
        widgetCoN.text = section.country
        widgetstate1.setText(R.string.bstate)
        widgetstate2.text = section.b_state

        widgetcity1.setText(R.string.city)
        widgetcity2.text = section.city
        widgetplace1.setText(R.string.place)
        widgetplace2.text = section.place

        widgetlocality1.setText(R.string.slocality)
        widgetlocality2.text = section.st_locality
        widgetplz1.setText(R.string.plz)
        widgetplz2.text = section.plz

        widgetdlo1.setText(R.string.dLo)
        widgetdla1.setText(R.string.dLa)
        widgetuncert1.setText(R.string.mUncert)
    }

    @SuppressLint("SetTextI18n")
    fun setWidgetDla2(name: Double) {
        val slen = name.toString().length
        if (slen > 8) {
            widgetdla2.text = name.toString().substring(0, 8)
        } else {
            widgetdla2.text = name.toString()
        }
    }

    @SuppressLint("SetTextI18n")
    fun setWidgetDlo2(name: Double) {
        val slen = name.toString().length
        if (slen > 8) {
            widgetdlo2.text = name.toString().substring(0, 8)
        } else {
            widgetdlo2.text = name.toString()
        }
    }

    fun setWidgetMuncert2(name: Double) {
        widgetuncert2.text = String.format("%s m", name.roundToInt())
    }

}
