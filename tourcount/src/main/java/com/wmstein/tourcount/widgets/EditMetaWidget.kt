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
 * EditMetaWidget.java used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09
 */
class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // temperature
    private val widget_temp1: TextView
    private val widget_temp2: EditText

    // wind
    private val widget_wind1: TextView
    private val widget_wind2: EditText

    // clouds
    private val widget_clouds1: TextView
    private val widget_clouds2: EditText

    // plz
    private val widget_plz1: TextView
    private val widget_plz2: TextView

    // city
    private val widget_city1: TextView
    private val widget_city2: TextView

    // place
    private val widget_place1: TextView
    private val widget_place2: TextView

    // date
    private val widget_date1: TextView
    private val widget_date2: TextView

    // start_tm
    private val widget_startTm1: TextView
    private val widget_startTm2: TextView

    // end_tm
    private val widget_endTm1: TextView
    private val widget_endTm2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_meta, this, true)
        widget_temp1 = findViewById(R.id.widgetTemp1) // temperature
        widget_temp2 = findViewById(R.id.widgetTemp2)
        widget_wind1 = findViewById(R.id.widgetWind1) // wind
        widget_wind2 = findViewById(R.id.widgetWind2)
        widget_clouds1 = findViewById(R.id.widgetClouds1) // clouds
        widget_clouds2 = findViewById(R.id.widgetClouds2)
        widget_plz1 = findViewById(R.id.widgetPlz1) // plz
        widget_plz2 = findViewById(R.id.widgetPlz2)
        widget_city1 = findViewById(R.id.widgetCity) // city
        widget_city2 = findViewById(R.id.widgetItem4)
        widget_place1 = findViewById(R.id.widgetPlace) // place
        widget_place2 = findViewById(R.id.widgetItem5)
        widget_date1 = findViewById(R.id.widgetDate1)
        widget_date2 = findViewById(R.id.widgetDate2)
        widget_startTm1 = findViewById(R.id.widgetStartTm1)
        widget_startTm2 = findViewById(R.id.widgetStartTm2)
        widget_endTm1 = findViewById(R.id.widgetEndTm1)
        widget_endTm2 = findViewById(R.id.widgetEndTm2)
    }

    // Following the SETS
    // temperature
    fun setWidgetTemp1(title: String?) {
        widget_temp1.text = title
    }

    // wind
    fun setWidgetWind1(title: String?) {
        widget_wind1.text = title
    }

    // clouds
    fun setWidgetClouds1(title: String?) {
        widget_clouds1.text = title
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

    // date
    fun setWidgetDate1(title: String?) {
        widget_date1.text = title
    }

    // start_tm
    fun setWidgetStartTm1(title: String?) {
        widget_startTm1.text = title
    }

    // end_tm
    fun setWidgetEndTm1(title: String?) {
        widget_endTm1.text = title
    }

    // plausi for numeric input
    private val pattern = Regex("^[0-9]*$")

    // following the GETS
    // get temperature with plausi
    var widgetTemp2: Int
        get() {
            val text = widget_temp2.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(pattern)) 100
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (nfe: NumberFormatException) {
                    100
                }
            }
        }
        set(name) {
            widget_temp2.setText(name.toString())
        }

    // get wind with plausi
    var widgetWind2: Int
        get() {
            val text = widget_wind2.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(pattern)) 100
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (nfe: NumberFormatException) {
                    100
                }
            }
        }
        set(name) {
            widget_wind2.setText(name.toString())
        }

    // get clouds with plausi
    var widgetClouds2: Int
        get() {
            val text = widget_clouds2.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(pattern)) 200
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (nfe: NumberFormatException) {
                    100
                }
            }
        }
        set(name) {
            widget_clouds2.setText(name.toString())
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
    var widgetDate2: String?
        get() = widget_date2.text.toString()
        set(name) {
            widget_date2.text = name
        }
    var widgetStartTm2: String?
        get() = widget_startTm2.text.toString()
        set(name) {
            widget_startTm2.text = name
        }
    var widgetEndTm2: String?
        get() = widget_endTm2.text.toString()
        set(name) {
            widget_endTm2.text = name
        }

    companion object {
        /**
         * Checks if a CharSequence is empty ("") or null.
         *
         *
         * isEmpty(null)      = true
         * isEmpty("")        = true
         * isEmpty(" ")       = false
         * isEmpty("bob")     = false
         * isEmpty("  bob  ") = false
         *
         * @param cs the CharSequence to check, may be null
         * @return `true` if the CharSequence is empty or null
         */
        private fun isEmpty(cs: CharSequence?): Boolean {
            return cs == null || cs.length == 0
        }
    }
}