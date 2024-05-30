/*
 * Copyright (c) 2018 - 2023. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.tourcount.AutoFitText
import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Count
import java.util.Objects

/****************************************************
 * Interface for widget_counting_i.xml
 * Created by wmstein 2016-12-18,
 * modified for TourCount on 2018-03-31,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2024-05-14
 */
class CountingWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val namef1i: TextView
    private val namef2i: TextView
    private val namef3i: TextView
    private val namepi: TextView
    private val nameli: TextView
    private val nameei: TextView

    private val countCountf1i: AutoFitText
    private val countCountf2i: AutoFitText
    private val countCountf3i: AutoFitText
    private val countCountpi: AutoFitText
    private val countCountli: AutoFitText
    private val countCountei: AutoFitText
    @JvmField
    var count: Count? = null

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_i, this, true)
        namef1i = findViewById(R.id.f1iName)
        namef2i = findViewById(R.id.f2iName)
        namef3i = findViewById(R.id.f3iName)
        namepi = findViewById(R.id.piName)
        nameli = findViewById(R.id.liName)
        nameei = findViewById(R.id.eiName)

        countCountf1i = findViewById(R.id.countCountf1i)
        countCountf2i = findViewById(R.id.countCountf2i)
        countCountf3i = findViewById(R.id.countCountf3i)
        countCountpi = findViewById(R.id.countCountpi)
        countCountli = findViewById(R.id.countCountli)
        countCountei = findViewById(R.id.countCountei)
    }

    fun setCount(newcount: Count?) {
        count = newcount
        namef1i.text = context.getString(R.string.countImagomfHint)
        namef2i.text = context.getString(R.string.countImagomHint)
        namef3i.text = context.getString(R.string.countImagofHint)
        namepi.text = context.getString(R.string.countPupaHint)
        nameli.text = context.getString(R.string.countLarvaHint)
        nameei.text = context.getString(R.string.countOvoHint)

        countCountf1i.text = count!!.count_f1i.toString()
        countCountf2i.text = count!!.count_f2i.toString()
        countCountf3i.text = count!!.count_f3i.toString()
        countCountpi.text = count!!.count_pi.toString()
        countCountli.text = count!!.count_li.toString()
        countCountei.text = count!!.count_ei.toString()

        val countUpf1iButton = findViewById<ImageButton>(R.id.buttonUpf1i)
        countUpf1iButton.tag = count!!.id
        val countUpf2iButton = findViewById<ImageButton>(R.id.buttonUpf2i)
        countUpf2iButton.tag = count!!.id
        val countUpf3iButton = findViewById<ImageButton>(R.id.buttonUpf3i)
        countUpf3iButton.tag = count!!.id
        val countUppiButton = findViewById<ImageButton>(R.id.buttonUppi)
        countUppiButton.tag = count!!.id
        val countUpliButton = findViewById<ImageButton>(R.id.buttonUpli)
        countUpliButton.tag = count!!.id
        val countUpeiButton = findViewById<ImageButton>(R.id.buttonUpei)
        countUpeiButton.tag = count!!.id

        val countDownf1iButton = findViewById<ImageButton>(R.id.buttonDownf1i)
        countDownf1iButton.tag = count!!.id
        val countDownf2iButton = findViewById<ImageButton>(R.id.buttonDownf2i)
        countDownf2iButton.tag = count!!.id
        val countDownf3iButton = findViewById<ImageButton>(R.id.buttonDownf3i)
        countDownf3iButton.tag = count!!.id
        val countDownpiButton = findViewById<ImageButton>(R.id.buttonDownpi)
        countDownpiButton.tag = count!!.id
        val countDownliButton = findViewById<ImageButton>(R.id.buttonDownli)
        countDownliButton.tag = count!!.id
        val countDowneiButton = findViewById<ImageButton>(R.id.buttonDownei)
        countDowneiButton.tag = count!!.id
    }

    // Count up/down and set value on screen
    fun countUpf1i() {
        // increase count_f1i
        countCountf1i.text = count!!.increase_f1i().toString()
    }

    fun countDownf1i() {
        countCountf1i.text = count!!.safe_decrease_f1i().toString()
    }

    fun countUpf2i() {
        countCountf2i.text = count!!.increase_f2i().toString()
    }

    fun countDownf2i() {
        countCountf2i.text = count!!.safe_decrease_f2i().toString()
    }

    fun countUpf3i() {
        countCountf3i.text = count!!.increase_f3i().toString()
    }

    fun countDownf3i() {
        countCountf3i.text = count!!.safe_decrease_f3i().toString()
    }

    fun countUppi() {
        countCountpi.text = count!!.increase_pi().toString()
    }

    fun countDownpi() {
        countCountpi.text = count!!.safe_decrease_pi().toString()
    }

    fun countUpli() {
        countCountli.text = count!!.increase_li().toString()
    }

    fun countDownli() {
        countCountli.text = count!!.safe_decrease_li().toString()
    }

    fun countUpei() {
        countCountei.text = count!!.increase_ei().toString()
    }

    fun countDownei() {
        countCountei.text = count!!.safe_decrease_ei().toString()
    }

}
