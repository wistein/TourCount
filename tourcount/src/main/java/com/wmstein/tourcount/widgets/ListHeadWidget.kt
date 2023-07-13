package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/***************************************************
 * EditHeadWidget.java used by EditMetaActivity.java
 * Created by wmstein for com.wmstein.tourcount on 2016-04-03,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05
 */
class ListHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget_lco // used for country title
            : TextView
    private val widget_lco1 // used for country
            : TextView
    private val widget_lname // used for observer title
            : TextView
    private val widget_lname1 // used for observer
            : TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_head, this, true)
        widget_lco = findViewById(R.id.widgetLCo)
        widget_lco1 = findViewById(R.id.widgetLCo1)
        widget_lname = findViewById(R.id.widgetLName)
        widget_lname1 = findViewById(R.id.widgetLName1)
    }

    fun setWidgetLCo(title: String?) {
        widget_lco.text = title
    }

    fun setWidgetLCo1(name: String?) {
        widget_lco1.text = name
    }

    fun setWidgetLName(title: String?) {
        widget_lname.text = title
    }

    fun setWidgetLName1(name: String?) {
        widget_lname1.text = name
    }
}