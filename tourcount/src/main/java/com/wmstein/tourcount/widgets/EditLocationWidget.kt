package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/*************************************************************
 * EditLocationWidget.kt used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2025-11-01
 */
class EditLocationWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // country
    private val widgetCoT: TextView
    private val widgetCoN: TextView

    // state
    private val widgetStateT: TextView
    private val widgetStateN: TextView

    // city
    private val widgetcity1: TextView
    private val widgetcity2: TextView

    // place
    private val widgetplace1: TextView
    private val widgetplace2: TextView

    // locality
    private val widgetLocalityT: TextView
    private val widgetLocalityN: TextView

    // plz
    private val widgetplz1: TextView
    private val widgetplz2: TextView

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_location, this, true)
        widgetCoT = findViewById(R.id.widgetCountryTitle)    // country
        widgetCoN = findViewById(R.id.widgetCountryName)
        widgetStateT = findViewById(R.id.widgetStateTitle)   // state
        widgetStateN = findViewById(R.id.widgetStateName)
        widgetcity1 = findViewById(R.id.widgetCity1)         // city
        widgetcity2 = findViewById(R.id.widgetCity2)
        widgetplace1 = findViewById(R.id.widgetPlace1)       // place
        widgetplace2 = findViewById(R.id.widgetPlace2)
        widgetLocalityT = findViewById(R.id.widgetLocalityTitle) // locality
        widgetLocalityN = findViewById(R.id.widgetLocalityName)
        widgetplz1 = findViewById(R.id.widgetPlz1)           // plz
        widgetplz2 = findViewById(R.id.widgetPlz2)
    }

    // The SETS
    // Country
    fun setWidgetCo1(title: String?) {
        widgetCoT.text = title
    }
    var widgetCo2: String?
        get() = widgetCoN.text.toString()
        set(name) {
            widgetCoN.text = name
        }

    // State
    fun setWidgetState1(title: String?) {
        widgetStateT.text = title
    }
    var widgetState2: String?
        get() = widgetStateN.text.toString()
        set(name) {
            widgetStateN.text = name
        }

    // City
    fun setWidgetCity1(title: String?) {
        widgetcity1.text = title
    }
    var widgetCity2: String?
        get() = widgetcity2.text.toString()
        set(name) {
            widgetcity2.text = name
        }

    // Place
    fun setWidgetPlace1(title: String?) {
        widgetplace1.text = title
    }
    var widgetPlace2: String?
        get() = widgetplace2.text.toString()
        set(name) {
            widgetplace2.text = name
        }

    // Locality
    fun setWidgetLocality1(title: String?) {
        widgetLocalityT.text = title
    }
    var widgetLocality2: String?
        get() = widgetLocalityN.text.toString()
        set(name) {
            widgetLocalityN.text = name
        }

    // PLZ
    fun setWidgetPlz1(title: String?) {
        widgetplz1.text = title
    }
    var widgetPlz2: String?
        get() = widgetplz2.text.toString()
        set(name) {
            widgetplz2.text = name
        }

}
