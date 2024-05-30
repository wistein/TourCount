/*
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.tourcount.MyDebug
import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Count
import java.util.Objects

/****************************************************
 * Interface for widget_counting_head2.xml
 * controls line (below spinner) with edit button and species notes
 * used by CountingActivity
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2024-05-14
 */
class CountingWidgetHead2(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val countHead2: TextView
    var count: Count? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_head2, this, true)
        countHead2 = findViewById(R.id.countHead2)
    }

    fun setCountHead2(count: Count) {
        // set TextView countHead2
        countHead2.text = count.notes
        // set ImageButton Edit
        val editButton = findViewById<ImageButton>(R.id.buttonEdit)
        editButton.tag = count.id
    }

    fun setFont(large: Boolean) {
        if (large) {
            if (MyDebug.LOG) Log.d(TAG, "Setzt große Schrift.")
            countHead2.textSize = 16f
        } else {
            if (MyDebug.LOG) Log.d(TAG, "Setzt kleine Schrift.")
            countHead2.textSize = 14f
        }
    }

    companion object {
        private const val TAG = "tourcountCntWidget_hd2"
    }

}