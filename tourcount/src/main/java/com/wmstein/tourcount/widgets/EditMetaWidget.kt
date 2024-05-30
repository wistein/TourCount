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
 * last edited on 2024-05-28
 */
class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widget_date1: TextView
    private val widget_date2: TextView

    // start time
    private val widget_startTm1: TextView
    private val widget_startTm2: TextView

    // end time
    private val widget_endTm1: TextView
    private val widget_endTm2: TextView

    // temperature
    private val widget_temp1: TextView
    private val widget_temp2: EditText
    private val widget_temp3: EditText

    // wind
    private val widget_wind1: TextView
    private val widget_wind2: EditText
    private val widget_wind3: EditText

    // clouds
    private val widget_clouds1: TextView
    private val widget_clouds2: EditText
    private val widget_clouds3: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_meta, this, true)
        widget_date1 = findViewById(R.id.widgetDate1)
        widget_date2 = findViewById(R.id.widgetDate2)
        widget_startTm1 = findViewById(R.id.widgetStartTm1)
        widget_startTm2 = findViewById(R.id.widgetStartTm2)
        widget_endTm1 = findViewById(R.id.widgetEndTm1)
        widget_endTm2 = findViewById(R.id.widgetEndTm2)

        widget_temp1 = findViewById(R.id.widgetTemp1) // temperature
        widget_temp2 = findViewById(R.id.widgetStartTemp)
        widget_temp3 = findViewById(R.id.widgetEndTemp)
        widget_wind1 = findViewById(R.id.widgetWind1) // wind
        widget_wind2 = findViewById(R.id.widgetStartWind)
        widget_wind3 = findViewById(R.id.widgetEndWind)
        widget_clouds1 = findViewById(R.id.widgetClouds1) // clouds
        widget_clouds2 = findViewById(R.id.widgetStartClouds)
        widget_clouds3 = findViewById(R.id.widgetEndClouds)
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
            if (name == 0)
                widget_temp2.setText("")
            else
                widget_temp2.setText(name.toString())
        }

    var widgetTemp3: Int
        get() {
            val text = widget_temp3.text.toString()
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
            if (name == 0)
                widget_temp3.setText("")
            else
                widget_temp3.setText(name.toString())
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
            if (name == 0)
                widget_wind2.setText("")
            else
                widget_wind2.setText(name.toString())
        }

    var widgetWind3: Int
        get() {
            val text = widget_wind3.text.toString()
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
            if (name == 0)
                widget_wind3.setText("")
            else
                widget_wind3.setText(name.toString())
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
            if (name == 0)
                widget_clouds2.setText("")
            else
                widget_clouds2.setText(name.toString())
        }

    var widgetClouds3: Int
        get() {
            val text = widget_clouds3.text.toString()
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
            if (name == 0)
                widget_clouds3.setText("")
            else
                widget_clouds3.setText(name.toString())
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
