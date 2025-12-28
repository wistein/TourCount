package com.wmstein.tourcount.widgets

import android.annotation.SuppressLint
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
 * last edited on 2025-11-11
 */
class EditMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // date
    private val widgetdate1: TextView
    private val widgetdate2: TextView

    // start time
    private val widgetstartTm1: TextView
    private val widgetstartTm2: TextView

    // end time
    private val widgetendTm1: TextView
    private val widgetendTm2: TextView

    // temperature
    private val widgettemp1: TextView
    private val widgettemp2: EditText
    private val widgettemp3: EditText

    // wind
    private val widgetwind1: TextView
    private val widgetwind2: EditText
    private val widgetwind3: EditText

    // clouds
    private val widgetclouds1: TextView
    private val widgetclouds2: EditText
    private val widgetclouds3: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_meta, this, true)
        widgetdate1 = findViewById(R.id.widgetDate1)
        widgetdate2 = findViewById(R.id.widgetDate2)
        widgetstartTm1 = findViewById(R.id.widgetStartTm1)
        widgetstartTm2 = findViewById(R.id.widgetStartTm2)
        widgetendTm1 = findViewById(R.id.widgetEndTm1)
        widgetendTm2 = findViewById(R.id.widgetEndTm2)

        widgettemp1 = findViewById(R.id.widgetTemp1) // temperature
        widgettemp2 = findViewById(R.id.widgetStartTemp)
        widgettemp3 = findViewById(R.id.widgetEndTemp)
        widgetwind1 = findViewById(R.id.widgetWind1) // wind
        widgetwind2 = findViewById(R.id.widgetStartWind)
        widgetwind3 = findViewById(R.id.widgetEndWind)
        widgetclouds1 = findViewById(R.id.widgetClouds1) // clouds
        widgetclouds2 = findViewById(R.id.widgetStartClouds)
        widgetclouds3 = findViewById(R.id.widgetEndClouds)
    }

    // Following the SETS of heads
    // date
    fun setWidgetDate1(title: String?) {
        widgetdate1.text = title
    }

    // start_tm
    fun setWidgetStartTm1(title: String?) {
        widgetstartTm1.text = title
    }

    // end_tm
    fun setWidgetEndTm1(title: String?) {
        widgetendTm1.text = title
    }

    // temperature
    fun setWidgetTemp1(title: String?) {
        widgettemp1.text = title
    }

    // wind
    fun setWidgetWind1(title: String?) {
        widgetwind1.text = title
    }

    // clouds
    fun setWidgetClouds1(title: String?) {
        widgetclouds1.text = title
    }

    // plausi for numeric input
    private val regEx = "^[0-9]*$"

    // following the GETS and SETS
    // get temperature with plausi
    var widgetTemp2: Int
        get() {
            val text = widgettemp2.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(regEx.toRegex())) 100
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    100
                }
            }
        }
        @SuppressLint("SetTextI18n")
        set(name) {
            if (name > 0 ) widgettemp2.setText(name.toString())
        }

    var widgetTemp3: Int
        get() {
            val text = widgettemp3.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(regEx.toRegex())) 100
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    100
                }
            }
        }
        @SuppressLint("SetTextI18n")
        set(name) {
            if (name > 0 ) widgettemp3.setText(name.toString())
        }

    // get wind with plausi
    var widgetWind2: Int
        get() {
            val text = widgetwind2.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(regEx.toRegex())) 100
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    100
                }
            }
        }

        @SuppressLint("SetTextI18n")
        set(name) {
            if (name > 0 ) widgetwind2.setText(name.toString())
        }

    var widgetWind3: Int
        get() {
            val text = widgetwind3.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(regEx.toRegex())) 100
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    100
                }
            }
        }
        @SuppressLint("SetTextI18n")
        set(name) {
            if (name > 0 ) widgetwind3.setText(name.toString())
        }

    // get clouds with plausi
    var widgetClouds2: Int
        get() {
            val text = widgetclouds2.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(regEx.toRegex())) 200
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    200
                }
            }
        }
        @SuppressLint("SetTextI18n")
        set(name) {
            if (name > 0 ) widgetclouds2.setText(name.toString())
        }

    var widgetClouds3: Int
        get() {
            val text = widgetclouds3.text.toString()
            return if (isEmpty(text)) 0
            else if (!text.trim { it <= ' ' }
                    .matches(regEx.toRegex())) 200
            else {
                try {
                    text.replace("\\D".toRegex(), "").toInt()
                } catch (_: NumberFormatException) {
                    200
                }
            }
        }
        @SuppressLint("SetTextI18n")
        set(name) {
            if (name > 0 ) widgetclouds3.setText(name.toString())
        }

    var widgetDate2: String?
        get() = widgetdate2.text.toString()
        set(name) {
            widgetdate2.text = name
        }
    var widgetStartTm2: String?
        get() = widgetstartTm2.text.toString()
        set(name) {
            widgetstartTm2.text = name
        }
    var widgetEndTm2: String?
        get() = widgetendTm2.text.toString()
        set(name) {
            widgetendTm2.text = name
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
            return cs.isNullOrEmpty()
        }
    }

}
