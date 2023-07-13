package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/***************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-03-31,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05
 */
class EditHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget_co1 // used for country title
            : TextView
    private val widget_co2 // used for country
            : EditText
    private val widget_name1 // used for observer title
            : TextView
    private val widget_name2 // used for observer
            : EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_head, this, true)
        widget_co1 = findViewById(R.id.widgetCo1)
        widget_co2 = findViewById(R.id.widgetCo2)
        widget_name1 = findViewById(R.id.widgetName1)
        widget_name2 = findViewById(R.id.widgetName2)
    }

    // Following the SETS
    // country
    fun setWidgetCo1(title: String?) {
        widget_co1.text = title
    }

    fun setWidgetCo2(name: String?) {
        widget_co2.setText(name)
    }

    // name
    fun setWidgetName1(title: String?) {
        widget_name1.text = title
    }

    fun setWidgetName2(name: String?) {
        widget_name2.setText(name)
    }

    // Following the GETS
    // country
    fun setWidgetCo2(): String {
        return widget_co2.text.toString()
    }

    // name
    fun setWidgetName2(): String {
        return widget_name2.text.toString()
    }
}