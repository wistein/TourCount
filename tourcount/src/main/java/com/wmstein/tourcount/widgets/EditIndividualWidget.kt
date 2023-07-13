package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/***************************************************************
 * EditIndividualWidget.java used by EditIndividualActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-05-15.
 * Last edited in Java on 2022-03-26,
 * converted to Kotlin on 2023-07-09
 */
class EditIndividualWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    // locality
    private val widget_locality1: TextView
    private val widget_locality2: EditText

    //height
    private val widget_zcoord1: TextView
    private val widget_zcoord2: TextView

    // stadium
    private val widget_stadium1: TextView
    private val widget_stadium2: TextView

    // state_1-6
    private val widget_state1: TextView
    private val widget_state2: EditText

    // number of individuals
    private val widget_count1: TextView
    private val widget_count2: EditText

    // note
    private val widget_indivnote1: TextView
    private val widget_indivnote2: EditText

    // x-coord
    private val widget_xcoord1: TextView
    private val widget_xcoord2: TextView

    // y-coord
    private val widget_ycoord1: TextView
    private val widget_ycoord2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_individual, this, true)
        widget_locality1 = findViewById(R.id.widgetLocality1) // Locality
        widget_locality2 = findViewById(R.id.widgetLocality2)
        widget_zcoord1 = findViewById(R.id.widgetZCoord1) // Height
        widget_zcoord2 = findViewById(R.id.widgetZCoord2)
        widget_stadium1 = findViewById(R.id.widgetStadium1) // Stadium
        widget_stadium2 = findViewById(R.id.widgetStadium2)
        widget_state1 = findViewById(R.id.widgetState1) // State_1-6
        widget_state2 = findViewById(R.id.widgetState2)
        widget_count1 = findViewById(R.id.widgetCount1) // number of individuals
        widget_count2 = findViewById(R.id.widgetCount2)
        widget_indivnote1 = findViewById(R.id.widgetIndivNote1) // Note
        widget_indivnote2 = findViewById(R.id.widgetIndivNote2)
        widget_xcoord1 = findViewById(R.id.widgetXCoord1) // X-Coord
        widget_xcoord2 = findViewById(R.id.widgetXCoord2)
        widget_ycoord1 = findViewById(R.id.widgetYCoord1) // Y-Coord
        widget_ycoord2 = findViewById(R.id.widgetYCoord2)
    }

    // Following the SETS
    // locality
    fun setWidgetLocality1(title: String?) {
        widget_locality1.text = title
    }

    // stadium
    fun setWidgetStadium1(title: String?) {
        widget_stadium1.text = title
    }

    // state
    fun setWidgetState1(title: String?) {
        widget_state1.text = title
    }

    // number of individuals
    fun setWidgetCount1(title: String?) {
        widget_count1.text = title
    }

    // note
    fun setWidgetIndivNote1(title: String?) {
        widget_indivnote1.text = title
    }

    // x-coord
    fun setWidgetXCoord1(title: String?) {
        widget_xcoord1.text = title
    }

    fun setWidgetXCoord2(name: String?) {
        widget_xcoord2.text = name
    }

    // y-coord
    fun setWidgetYCoord1(title: String?) {
        widget_ycoord1.text = title
    }

    fun setWidgetYCoord2(name: String?) {
        widget_ycoord2.text = name
    }

    // z-coord
    fun setWidgetZCoord1(title: String?) {
        widget_zcoord1.text = title
    }

    fun setWidgetZCoord2(name: String?) {
        widget_zcoord2.text = name
    }

    // following the GETS
    // get locality
    var widgetLocality2: String
        get() = widget_locality2.text.toString()
        set(name) {
            widget_locality2.setText(name)
        }

    // get stadium with plausi
    var widgetStadium2: String?
        get() = widget_stadium2.text.toString()
        set(name) {
            widget_stadium2.text = name
        }

    fun widgetState1(enabled: Boolean) {
        if (enabled) widget_state1.visibility = VISIBLE else widget_state1.visibility = INVISIBLE
    }

    // get state number with plausi
    val widgetState2: String
        get() {
            var text = widget_state2.text.toString()
            if (text == "-") text = "0"
            val regEx = Regex("^[0-9]*$")
            return if (text == "") "0" else if (!text.trim { it <= ' ' }
                    .matches(regEx)) "100" else {
                try {
                    text.replace("\\D".toRegex(), "")
                } catch (nfe: NumberFormatException) {
                    "0"
                }
            }
        }

    fun widgetState2(enabled: Boolean) {
        if (enabled) widget_state2.visibility = VISIBLE else widget_state2.visibility = INVISIBLE
    }

    fun setWidgetState2(name: Int) {
        widget_state2.setText(name.toString())
    }

    fun setWidgetState2(name: String?) {
        widget_state2.setText(name)
    }// count < 0 or text has no digit// value >= 0

    // get number of individuals
    var widgetCount2: Int
        get() {
            val text = widget_count2.text.toString()
            return try {
                // value >= 0
                text.replace("\\D".toRegex(), "").toInt()
            } catch (nfe: NumberFormatException) {
                -1 // count < 0 or text has no digit
            }
        }
        set(name) {
            widget_count2.setText(name.toString())
        }

    // get note of individual
    var widgetIndivNote2: String?
        get() = widget_indivnote2.text.toString()
        set(name) {
            widget_indivnote2.setText(name)
        }
}