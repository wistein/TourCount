package com.wmstein.tourcount.widgets

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
 * ListLocationWidget.kt used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-05-07
 */
class ListLocationWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // country
    private val widget_co1: TextView
    private val widget_co2: TextView

    // plz
    private val widget_plz1: TextView
    private val widget_plz2: TextView

    // city
    private val widget_city1: TextView
    private val widget_city2: TextView

    // place
    private val widget_place1: TextView
    private val widget_place2: TextView

    // average longitude
    private val widget_dlo1: TextView
    private val widget_dlo2: TextView

    // average latitude
    private val widget_dla1: TextView
    private val widget_dla2: TextView

    // mean uncertainty
    private val widget_muncert1: TextView
    private val widget_muncert2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_location, this, true)
        widget_co1 = findViewById(R.id.widgetLCountryTitle) // Country
        widget_co2 = findViewById(R.id.widgetLCountryName)
        widget_plz1 = findViewById(R.id.widgetLPlz1) // plz
        widget_plz2 = findViewById(R.id.widgetLPlz2)
        widget_city1 = findViewById(R.id.widgetLCity1) // city
        widget_city2 = findViewById(R.id.widgetLCity2)
        widget_place1 = findViewById(R.id.widgetLPlace1) // place
        widget_place2 = findViewById(R.id.widgetLPlace2)
        widget_dlo1 = findViewById(R.id.widgetLdlo1) // lon
        widget_dlo2 = findViewById(R.id.widgetLdlo2)
        widget_dla1 = findViewById(R.id.widgetLdla1) // lat
        widget_dla2 = findViewById(R.id.widgetLdla2)
        widget_muncert1 = findViewById(R.id.widgetLmuncert1) // uncert
        widget_muncert2 = findViewById(R.id.widgetLmuncert2)
    }

    // Following the SETS
    fun setLocationWidget(section: Section) {
        widget_co1.setText(R.string.country)
        widget_co2.text = section.country
        widget_plz1.setText(R.string.plz)
        widget_plz2.text = section.plz

        widget_city1.setText(R.string.city)
        widget_city2.text = section.city
        widget_place1.setText(R.string.place)
        widget_place2.text = section.place

        widget_dlo1.setText(R.string.dLo)
        widget_dla1.setText(R.string.dLa)
        widget_muncert1.setText(R.string.mUncert)
    }

    fun setWidgetDla2(name: Double) {
        val slen = name.toString().length
        if (slen > 8) {
            widget_dla2.text = name.toString().substring(0, 8)
        } else {
            widget_dla2.text = name.toString()
        }
    }

    fun setWidgetDlo2(name: Double) {
        val slen = name.toString().length
        if (slen > 8) {
            widget_dlo2.text = name.toString().substring(0, 8)
        } else {
            widget_dlo2.text = name.toString()
        }
    }

    fun setWidgetMuncert2(name: Double) {
        widget_muncert2.text = String.format("%s m", name.roundToInt())
    }

}
