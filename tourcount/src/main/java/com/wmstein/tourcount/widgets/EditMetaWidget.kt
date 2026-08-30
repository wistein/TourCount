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
 *
 * Created by wmstein for com.wmstein.tourcount on 2016-04-02,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2026-08-21
 */
class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widgetDateHead: TextView
    private val widgetDatum: TextView

    // start time
    private val widgetStartTimeHead: TextView
    private val widgetStartTime: TextView

    // end time
    private val widgetEndTimeHead: TextView
    private val widgetEndTime: TextView

    // temperature
    private val widgetTempHead: TextView
    private val widgetStartTemp: EditText
    private val widgetEndTemp: EditText

    // wind
    private val widgetWindHead: TextView
    private val widgetStartWind: EditText
    private val widgetEndWind: EditText

    // clouds
    private val widgetCloudsHead: TextView
    private val widgetStartClouds: EditText
    private val widgetEndClouds: EditText

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_meta, this, true)

        widgetDateHead = findViewById(R.id.widgetDateTitle) // date
        widgetDatum = findViewById(R.id.widgetDateValue)

        widgetStartTimeHead = findViewById(R.id.widgetStartTimeTitle)
        widgetStartTime = findViewById(R.id.widgetStartTimeValue)

        widgetEndTimeHead = findViewById(R.id.widgetEndTimeTitle)
        widgetEndTime = findViewById(R.id.widgetEndTimeValue)

        widgetTempHead = findViewById(R.id.widgetTempTitle) // temperature title
        widgetStartTemp = findViewById(R.id.widgetStartTempValue)
        widgetEndTemp = findViewById(R.id.widgetEndTempValue)

        widgetWindHead = findViewById(R.id.widgetWindTitle) // wind title
        widgetStartWind = findViewById(R.id.widgetStartWindValue)
        widgetEndWind = findViewById(R.id.widgetEndWindValue)

        widgetCloudsHead = findViewById(R.id.widgetCloudsTitle) // clouds
        widgetStartClouds = findViewById(R.id.widgetStartCloudsValue)
        widgetEndClouds = findViewById(R.id.widgetEndCloudsValue)
    }

    // Following the SETS of heads
    // date
    fun setWidgetDate1(title: String) {
        widgetDateHead.text = title
    }

    // start_tm
    fun setWidgetStartTimeTitle(title: String) {
        widgetStartTimeHead.text = title
    }

    // end_tm
    fun setWidgetEndTimeTitle(title: String) {
        widgetEndTimeHead.text = title
    }

    // temperature
    fun setWidgetTemp1(title: String) {
        widgetTempHead.text = title
    }

    // wind
    fun setWidgetWind1(title: String) {
        widgetWindHead.text = title
    }

    // clouds
    fun setWidgetClouds1(title: String) {
        widgetCloudsHead.text = title
    }

    // Plausi for numeric input
    private val regEx = "^[0-9]*$"

    // Following the GETS and SETS
    var widgetDate2: String
        get() = widgetDatum.text.toString()
        set(name) {
            widgetDatum.text = name
        }
    var widgetStartTm2: String
        get() = widgetStartTime.text.toString()
        set(name) {
            widgetStartTime.text = name
        }
    var widgetEndTm2: String
        get() = widgetEndTime.text.toString()
        set(name) {
            widgetEndTime.text = name
        }

    // Get temperature with plausi
    var widgetTempS: Int
        get() {
            val text = widgetStartTemp.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100 // value must be out of allowed range
            }
        }
        set(value) {
            if (value > 0) widgetStartTemp.setText(value.toString())
        }

    var widgetTempE: Int
        get() {
            val text = widgetEndTemp.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }
        }
        set(value) {
            if (value > 0) widgetEndTemp.setText(value.toString())
        }

    // Get wind with plausi
    var widgetWindS: Int
        get() {
            val text = widgetStartWind.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }

        }

        set(value) {
            if (value > 0) widgetStartWind.setText(value.toString())
        }

    var widgetWindE: Int
        get() {
            val text = widgetEndWind.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 100
            else try {
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                100
            }
        }
        set(value) {
            if (value > 0) widgetEndWind.setText(value.toString())
        }

    // Get start clouds with plausi
    var widgetCloudsS: Int
        get() {
            val text = widgetStartClouds.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 200
            else try {
                val number = text.replace("\\D".toRegex(), "").toInt()
                // Check plausi and round integer value to decade (number+5)/10*10
                if (number in 0 .. 100)
                    return (number + 5) / 10 * 10
                else 200
            } catch (_: NumberFormatException) {
                200
            }
        }
        set(value) {
            if (value > 0) {
                // Round integer value to decade by (number+5)/10*10
                val value1 = (value + 5) / 10 * 10
                widgetStartClouds.setText(value1.toString())
            }
        }

    // Get end clouds with plausi
    var widgetCloudsE: Int
        get() {
            val text = widgetEndClouds.text.toString()
            return if (text.isEmpty()) 0
            else if (!text.trim { it <= ' ' }.matches(regEx.toRegex())) 200
            else try {
                val number = text.replace("\\D".toRegex(), "").toInt()
                // Check plausi and round integer value to decade (number+5)/10*10
                if (number in 0 .. 100)
                    return (number + 5) / 10 * 10
                else 200
            } catch (_: NumberFormatException) {
                200
            }
        }
        set(value) {
            if (value > 0) {
                // Round integer value to decade by (number+5)/10*10
                val value1 = (value + 5) / 10 * 10
                widgetEndClouds.setText(value1.toString())
            }
        }

}
