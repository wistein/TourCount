package com.wmstein.tourcount.widgets

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/*************************************************************
 * EditLocationWidget.kt used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-11-25
 */
class EditLocationWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // country
    private val widget_co1: TextView
    private val widget_co2: TextView

    // state
    private val widget_state1: TextView
    private val widget_state2: TextView

    // city
    private val widget_city1: TextView
    private val widget_city2: TextView

    // place
    private val widget_place1: TextView
    private val widget_place2: TextView

    // locality
    private val widget_locality1: TextView
    private val widget_locality2: TextView

    // plz
    private val widget_plz1: TextView
    private val widget_plz2: TextView

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_location, this, true)
        widget_co1 = findViewById(R.id.widgetCountryTitle)    // country
        widget_co2 = findViewById(R.id.widgetCountryName)
        widget_state1 = findViewById(R.id.widgetStateTitle)   // state
        widget_state2 = findViewById(R.id.widgetStateName)
        widget_city1 = findViewById(R.id.widgetCity1)         // city
        widget_city2 = findViewById(R.id.widgetCity2)
        widget_place1 = findViewById(R.id.widgetPlace1)       // place
        widget_place2 = findViewById(R.id.widgetPlace2)
        widget_locality1 = findViewById(R.id.widgetLocalityTitle) // locality
        widget_locality2 = findViewById(R.id.widgetLocalityName)
        widget_plz1 = findViewById(R.id.widgetPlz1)           // plz
        widget_plz2 = findViewById(R.id.widgetPlz2)
    }

    // The SETS
    // Country
    fun setWidgetCo1(title: String?) {
        widget_co1.text = title
    }
    var widgetCo2: String?
        get() = widget_co2.text.toString()
        set(name) {
            widget_co2.text = name
        }

    // State
    fun setWidgetState1(title: String?) {
        widget_state1.text = title
    }
    var widgetState2: String?
        get() = widget_state2.text.toString()
        set(name) {
            widget_state2.text = name
        }

    // City
    fun setWidgetCity1(title: String?) {
        widget_city1.text = title
    }
    var widgetCity2: String?
        get() = widget_city2.text.toString()
        set(name) {
            widget_city2.text = name
        }

    // Place
    fun setWidgetPlace1(title: String?) {
        widget_place1.text = title
    }
    var widgetPlace2: String?
        get() = widget_place2.text.toString()
        set(name) {
            widget_place2.text = name
        }

    // Locality
    fun setWidgetLocality1(title: String?) {
        widget_locality1.text = title
    }
    var widgetLocality2: String?
        get() = widget_locality2.text.toString()
        set(name) {
            widget_locality2.text = name
        }

    // PLZ
    fun setWidgetPlz1(title: String?) {
        widget_plz1.text = title
    }
    var widgetPlz2: String?
        get() = widget_plz2.text.toString()
        set(name) {
            widget_plz2.text = name
        }

}
