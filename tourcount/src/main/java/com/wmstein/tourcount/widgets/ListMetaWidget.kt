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
 * ListMetaWidget.java used by ListSpeciesActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-19,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-05
 */
class ListMetaWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget_lmeta1 // temperature
            : TextView
    private val widget_litem1: TextView
    private val widget_lmeta2 // wind
            : TextView
    private val widget_litem2: TextView
    private val widget_lmeta3 // clouds
            : TextView
    private val widget_litem3: TextView
    private val widget_lplz1 // plz
            : TextView
    private val widget_lplz2: TextView
    private val widget_lcity // city
            : TextView
    private val widget_litem4: TextView
    private val widget_lplace // place
            : TextView
    private val widget_litem5: TextView
    private val widget_ldate1 // date
            : TextView
    private val widget_ldate2: TextView
    private val widget_lstartTm1 // start_tm
            : TextView
    private val widget_lstartTm2: TextView
    private val widget_lendTm1 // end_tm
            : TextView
    private val widget_lendTm2: TextView
    private val widget_dlo1 // average longitude
            : TextView
    private val widget_dlo2: TextView
    private val widget_dla1 // average latitude
            : TextView
    private val widget_dla2: TextView
    private val widget_muncert1 // mean uncertainty
            : TextView
    private val widget_muncert2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_meta, this, true)
        widget_lmeta1 = findViewById(R.id.widgetLMeta1)
        widget_litem1 = findViewById(R.id.widgetLItem1)
        widget_lmeta2 = findViewById(R.id.widgetLMeta2)
        widget_litem2 = findViewById(R.id.widgetLItem2)
        widget_lmeta3 = findViewById(R.id.widgetLMeta3)
        widget_litem3 = findViewById(R.id.widgetLItem3)
        widget_lplz1 = findViewById(R.id.widgetLPlz1)
        widget_lplz2 = findViewById(R.id.widgetLPlz2)
        widget_lcity = findViewById(R.id.widgetLCity)
        widget_litem4 = findViewById(R.id.widgetLItem4)
        widget_lplace = findViewById(R.id.widgetLPlace)
        widget_litem5 = findViewById(R.id.widgetLItem5)
        widget_ldate1 = findViewById(R.id.widgetLDate1)
        widget_ldate2 = findViewById(R.id.widgetLDate2)
        widget_lstartTm1 = findViewById(R.id.widgetLStartTm1)
        widget_lstartTm2 = findViewById(R.id.widgetLStartTm2)
        widget_lendTm1 = findViewById(R.id.widgetLEndTm1)
        widget_lendTm2 = findViewById(R.id.widgetLEndTm2)
        widget_dlo1 = findViewById(R.id.widgetdlo1)
        widget_dlo2 = findViewById(R.id.widgetdlo2)
        widget_dla1 = findViewById(R.id.widgetdla1)
        widget_dla2 = findViewById(R.id.widgetdla2)
        widget_muncert1 = findViewById(R.id.widgetmuncert1)
        widget_muncert2 = findViewById(R.id.widgetmuncert2)
    }

    // Following the SETS
    fun setMetaWidget(section: Section) {
        widget_lmeta1.setText(R.string.temperature)
        widget_litem1.text = section.tmp.toString()
        widget_lmeta2.setText(R.string.wind)
        widget_litem2.text = section.wind.toString()
        widget_lmeta3.setText(R.string.clouds)
        widget_litem3.text = section.clouds.toString()
        widget_lplz1.setText(R.string.plz)
        widget_lplz2.text = section.plz
        widget_lcity.setText(R.string.city)
        widget_litem4.text = section.city
        widget_lplace.setText(R.string.place)
        widget_litem5.text = section.place
        widget_ldate1.setText(R.string.date)
        widget_ldate2.text = section.date
        widget_lstartTm1.setText(R.string.starttm)
        widget_lstartTm2.text = section.start_tm
        widget_lendTm1.setText(R.string.endtm)
        widget_lendTm2.text = section.end_tm
        widget_dlo1.setText(R.string.dLo)
        widget_dla1.setText(R.string.dLa)
        widget_muncert1.setText(R.string.mUncert)
    }

    fun setWidget_dla2(name: Double) {
        val slen = name.toString().length
        if (slen > 8) {
            widget_dla2.text = name.toString().substring(0, 8)
        } else {
            widget_dla2.text = name.toString()
        }
    }

    fun setWidget_dlo2(name: Double) {
        val slen = name.toString().length
        if (slen > 8) {
            widget_dlo2.text = name.toString().substring(0, 8)
        } else {
            widget_dlo2.text = name.toString()
        }
    }

    fun setWidget_muncert2(name: Double) {
        widget_muncert2.text = String.format("%s m", name.roundToInt())
    }
}
