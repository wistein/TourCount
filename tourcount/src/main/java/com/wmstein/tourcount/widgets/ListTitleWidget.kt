package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/**********************************
 * Created by wmstein on 2016-06-06,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05
 */
class ListTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val list_title: TextView
    private val list_name: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_title, this, true)
        list_title = findViewById(R.id.listTitle)
        list_name = findViewById(R.id.listName)
    }

    fun setListTitle(title: String?) {
        list_title.text = title
    }

    fun setListName(name: String?) {
        list_name.text = name
    }
}