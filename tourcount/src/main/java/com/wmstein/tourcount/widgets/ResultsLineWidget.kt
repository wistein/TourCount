package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.wmstein.tourcount.R
import java.util.Objects

/*****************************************************
 * ResultsLineWidget.kt is used by ShowResultsActivity
 *
 * Created by wmstein on 2018-02-24,
 * last edited in Java on 2019-01-27,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-03-02
 */
class ResultsLineWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_line, this, true)
    }

}
