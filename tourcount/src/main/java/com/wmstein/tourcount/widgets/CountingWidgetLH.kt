/*
 * Copyright (c) 2018 2013. Wilhelm Stein, Bonn, Germany.
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

/**********************************
 * Interface for widget_counting_lhi.xml
 * used by CountingActivity
 * Created by wmstein 2016-12-18,
 * modified for TourCount on 2018-03-31,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-11
 */
class CountingWidgetLH(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val namef1i: TextView
    private val namef2i: TextView
    private val namef3i: TextView
    private val namepi: TextView
    private val nameli: TextView
    private val nameei: TextView
    // section internal counters
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
        Objects.requireNonNull(inflater).inflate(R.layout.widget_counting_lhi, this, true)
        namef1i = findViewById(R.id.f1iNameLH)
        namef2i = findViewById(R.id.f2iNameLH)
        namef3i = findViewById(R.id.f3iNameLH)
        namepi = findViewById(R.id.piNameLH)
        nameli = findViewById(R.id.liNameLH)
        nameei = findViewById(R.id.eiNameLH)
        countCountf1i = findViewById(R.id.countCountLHf1i)
        countCountf2i = findViewById(R.id.countCountLHf2i)
        countCountf3i = findViewById(R.id.countCountLHf3i)
        countCountpi = findViewById(R.id.countCountLHpi)
        countCountli = findViewById(R.id.countCountLHli)
        countCountei = findViewById(R.id.countCountLHei)
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
        val countUpf1iButton = findViewById<ImageButton>(R.id.buttonUpLHf1i)
        countUpf1iButton.tag = count!!.id
        val countUpf2iButton = findViewById<ImageButton>(R.id.buttonUpLHf2i)
        countUpf2iButton.tag = count!!.id
        val countUpf3iButton = findViewById<ImageButton>(R.id.buttonUpLHf3i)
        countUpf3iButton.tag = count!!.id
        val countUppiButton = findViewById<ImageButton>(R.id.buttonUpLHpi)
        countUppiButton.tag = count!!.id
        val countUpliButton = findViewById<ImageButton>(R.id.buttonUpLHli)
        countUpliButton.tag = count!!.id
        val countUpeiButton = findViewById<ImageButton>(R.id.buttonUpLHei)
        countUpeiButton.tag = count!!.id
        val countDownf1iButton = findViewById<ImageButton>(R.id.buttonDownLHf1i)
        val countDownf2iButton = findViewById<ImageButton>(R.id.buttonDownLHf2i)
        val countDownf3iButton = findViewById<ImageButton>(R.id.buttonDownLHf3i)
        val countDownpiButton = findViewById<ImageButton>(R.id.buttonDownLHpi)
        val countDownliButton = findViewById<ImageButton>(R.id.buttonDownLHli)
        val countDowneiButton = findViewById<ImageButton>(R.id.buttonDownLHei)
        countDownf1iButton.tag = count!!.id
        countDownf2iButton.tag = count!!.id
        countDownf3iButton.tag = count!!.id
        countDownpiButton.tag = count!!.id
        countDownliButton.tag = count!!.id
        countDowneiButton.tag = count!!.id
    }

    // Count up/down and set value on lefthanded screen
    fun countUpLHf1i() {
        // increase count_f1i
        countCountf1i.text = count!!.increase_f1i().toString()
    }

    fun countDownLHf1i() {
        countCountf1i.text = count!!.safe_decrease_f1i().toString()
    }

    fun countUpLHf2i() {
        countCountf2i.text = count!!.increase_f2i().toString()
    }

    fun countDownLHf2i() {
        countCountf2i.text = count!!.safe_decrease_f2i().toString()
    }

    fun countUpLHf3i() {
        countCountf3i.text = count!!.increase_f3i().toString()
    }

    fun countDownLHf3i() {
        countCountf3i.text = count!!.safe_decrease_f3i().toString()
    }

    fun countUpLHpi() {
        countCountpi.text = count!!.increase_pi().toString()
    }

    fun countDownLHpi() {
        countCountpi.text = count!!.safe_decrease_pi().toString()
    }

    fun countUpLHli() {
        countCountli.text = count!!.increase_li().toString()
    }

    fun countDownLHli() {
        countCountli.text = count!!.safe_decrease_li().toString()
    }

    fun countUpLHei() {
        countCountei.text = count!!.increase_ei().toString()
    }

    fun countDownLHei() {
        countCountei.text = count!!.safe_decrease_ei().toString()
    }
}