package com.wmstein.tourcount.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.AutoFitEditText
import com.wmstein.tourcount.R
import java.util.Objects

/***************************************************************
 * EditIndividualWidget.kt used by EditIndividualActivity.kt
 * Created by wmstein for com.wmstein.tourcount on 2016-05-15.
 * Last edited in Java on 2022-03-26,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-11-19
 */
class EditIndividualWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // locality
    private val widgetLoc1: TextView
    private val widgetLoc2: EditText

    // stadium
    private val widgetStad1: TextView
    private val widgetStad2: TextView

    // state_1-6
    private val widgetStat1: TextView
    private val widgetStat2: EditText

    // number of individuals
    private val widgetCnt1: TextView
    private val widgetCnt2: AutoFitEditText

    // note
    private val widgetIndNote1: TextView
    private val widgetIndNote2: EditText

    // x-coord
    private val widgetXcoord1: TextView
    private val widgetXcoord2: TextView

    // y-coord
    private val widgetYcoord1: TextView
    private val widgetYcoord2: TextView

    // height
    private val widgetZcoord1: TextView
    private val widgetZcoord2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_individual, this, true)
        widgetLoc1 = findViewById(R.id.widget_Locality1) // Locality
        widgetLoc2 = findViewById(R.id.widget_Locality2)
        widgetStad1 = findViewById(R.id.widget_Stadium1) // Stadium
        widgetStad2 = findViewById(R.id.widget_Stadium2)
        widgetStat1 = findViewById(R.id.widget_State1) // State_1-6
        widgetStat2 = findViewById(R.id.widget_State2)
        widgetCnt1 = findViewById(R.id.widget_Count1) // number of individuals
        widgetCnt2 = findViewById(R.id.widget_Count2)
        widgetIndNote1 = findViewById(R.id.widget_IndivNote1) // Note
        widgetIndNote2 = findViewById(R.id.widget_IndivNote2)
        widgetXcoord1 = findViewById(R.id.widget_XCoord1) // X-Coord (lat)
        widgetXcoord2 = findViewById(R.id.widget_XCoord2)
        widgetYcoord1 = findViewById(R.id.widget_YCoord1) // Y-Coord (lon)
        widgetYcoord2 = findViewById(R.id.widget_YCoord2)
        widgetZcoord1 = findViewById(R.id.widget_ZCoord1) // Height
        widgetZcoord2 = findViewById(R.id.widget_ZCoord2)
    }

    // Following the SETS
    // locality
    fun setWidgetLocality1(title: String?) {
        widgetLoc1.text = title
    }

    // stadium
    fun setWidgetStadium1(title: String?) {
        widgetStad1.text = title
    }

    // state
    fun setWidgetState1(title: String?) {
        widgetStat1.text = title
    }

    // number of individuals
    fun setWidgetCount1(title: String?) {
        widgetCnt1.text = title
    }

    // note
    fun setWidgetIndivNote1(title: String?) {
        widgetIndNote1.text = title
    }

    // x-coord
    fun setWidgetXCoord1(title: String?) {
        widgetXcoord1.text = title
    }

    fun setWidgetXCoord2(name: String?) {
        widgetXcoord2.text = name
    }

    // y-coord
    fun setWidgetYCoord1(title: String?) {
        widgetYcoord1.text = title
    }

    fun setWidgetYCoord2(name: String?) {
        widgetYcoord2.text = name
    }

    // z-coord
    fun setWidgetZCoord1(title: String?) {
        widgetZcoord1.text = title
    }

    fun setWidgetZCoord2(name: String?) {
        widgetZcoord2.text = name
    }

    // following the GETS
    // get locality
    var widgetLocality2: String
        get() = widgetLoc2.text.toString()
        set(name) {
            widgetLoc2.setText(name)
        }

    // get stadium with plausi
    var widgetStadium2: String?
        get() = widgetStad2.text.toString()
        set(name) {
            widgetStad2.text = name
        }

    fun widgetState1(enabled: Boolean) {
        if (enabled) widgetStat1.visibility = VISIBLE else widgetStat1.visibility = INVISIBLE
    }

    // get state number with plausi
    val widgetState2: String
        get() {
            var text = widgetStat2.text.toString()
            if (text == "-") text = "0"
            val regEx = Regex("^[0-9]*$")
            return if (text == "") "0" else if (!text.trim { it <= ' ' }
                    .matches(regEx)) "100" else {
                try {
                    text.replace("\\D".toRegex(), "")
                } catch (_: NumberFormatException) {
                    "0"
                }
            }
        }

    fun widgetState2(enabled: Boolean) {
        if (enabled) widgetStat2.visibility = VISIBLE else widgetStat2.visibility = INVISIBLE
    }

    fun setWidgetState2(name: String?) {
        widgetStat2.setText(name)
    }

    // get number of individuals
    var widgetCount2: Int
        get() {
            val text = widgetCnt2.text.toString()
            return try {
                // value >= 0
                text.replace("\\D".toRegex(), "").toInt()
            } catch (_: NumberFormatException) {
                -1 // count < 0 or text has no digit
            }
        }
        @SuppressLint("SetTextI18n")
        set(name) {
            widgetCnt2.setText(name.toString())
        }

    // get note of individual
    var widgetIndivNote2: String?
        get() = widgetIndNote2.text.toString()
        set(name) {
            widgetIndNote2.setText(name)
        }

}
