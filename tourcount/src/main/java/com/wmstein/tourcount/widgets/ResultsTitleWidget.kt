package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/************************************************
 * ResultsTitleWidget used by ShowResultsActivity
 *
 * Created by wmstein on 2016-06-06,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-03-02
 */
class ResultsTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val listtitle: TextView
    private val listname: TextView
    private val widgetname1: TextView
    private val widgetname2: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_title, this, true)
        listtitle = findViewById(R.id.listTitle)
        listname = findViewById(R.id.listName)
        widgetname1 = findViewById(R.id.widgetName1)
        widgetname2 = findViewById(R.id.widgetName2)
    }

    fun setListTitle(title: String?) {
        listtitle.text = title
    }

    fun setListName(name: String?) {
        listname.text = name
    }

    fun setWidgetName1(name: String?) {
        widgetname1.text = name
    }

    fun setWidgetName2(name: String?) {
        widgetname2.text = name
    }

}
