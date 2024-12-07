package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/*************************************************************
 * EditMetaWidget.kt used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-11-25
 */
class EditLocationWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // country
    private val widget_co1: TextView
    private val widget_co2: EditText

    // plz
    private val widget_plz1: TextView
    private val widget_plz2: TextView

    // city
    private val widget_city1: TextView
    private val widget_city2: TextView

    // place
    private val widget_place1: TextView
    private val widget_place2: TextView

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_location, this, true)
        widget_co1 = findViewById(R.id.widgetCountryTitle) // Country
        widget_co2 = findViewById(R.id.widgetCountryName)
        widget_plz1 = findViewById(R.id.widgetPlz1) // plz
        widget_plz2 = findViewById(R.id.widgetPlz2)
        widget_city1 = findViewById(R.id.widgetCity1) // city
        widget_city2 = findViewById(R.id.widgetCity2)
        widget_place1 = findViewById(R.id.widgetPlace1) // place
        widget_place2 = findViewById(R.id.widgetPlace2)
    }

    // Following the SETS
    // Country
    fun setWidgetCo1(title: String?) {
        widget_co1.text = title
    }

    fun setWidgetCo2(name: String?) {
        widget_co2.setText(name)
    }

    // PLZ
    fun setWidgetPlz1(title: String?) {
        widget_plz1.text = title
    }

    // city
    fun setWidgetCity1(title: String?) {
        widget_city1.text = title
    }

    // place
    fun setWidgetPlace1(title: String?) {
        widget_place1.text = title
    }

    // plausi for numeric input
//    private val pattern = Regex("^[0-9]*$")

    // following the GETS
    // get country
    fun setWidgetCo2(): String {
        return widget_co2.text.toString()
    }

    // get PLZ with plausi
    var widgetPlz2: String?
        get() = widget_plz2.text.toString()
        set(name) {
            widget_plz2.text = name
        }

    // get city with plausi
    var widgetCity2: String?
        get() = widget_city2.text.toString()
        set(name) {
            widget_city2.text = name
        }

    // get place with plausi
    var widgetPlace2: String?
        get() = widget_place2.text.toString()
        set(name) {
            widget_place2.text = name
        }

}
