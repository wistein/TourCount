package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.wmstein.tourcount.R
import java.util.Objects

/**********************************
 * Created by wmstein on 2018-02-24
 * used by ListSpeciesActivity
 * last edited in Java on 2019-01-27,
 * converted to Kotlin on 2023-07-05
 */
class ListLineWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_line, this, true)
    }
}