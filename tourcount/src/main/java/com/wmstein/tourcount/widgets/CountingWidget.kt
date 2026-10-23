package com.wmstein.tourcount.widgets

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Interface for widget_counting_*.xml
 * used by CountingActivity
 *
 * Created by wmstein for TransektCount on 2016-12-18,
 * modified for TourCount on 2018-03-31,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2023-07-11,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView

import com.wmstein.tourcount.AutoFitText
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.R
import com.wmstein.tourcount.TourCountApplication.Companion.screenRatioBig

import java.util.Objects

@SuppressLint("SetTextI18n")
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

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        if (screenRatioBig) // > 2
            Objects.requireNonNull(inflater)
                .inflate(R.layout.widget_counting_big,this, true)
        else
            Objects.requireNonNull(inflater)
                .inflate(R.layout.widget_counting_small,this, true)

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

        if (count!!.count_f1i > 0)
            countCountf1i.text = count!!.count_f1i.toString()
        if (count!!.count_f2i > 0)
            countCountf2i.text = count!!.count_f2i.toString()
        if (count!!.count_f3i > 0)
            countCountf3i.text = count!!.count_f3i.toString()
        if (count!!.count_pi > 0)
            countCountpi.text = count!!.count_pi.toString()
        if (count!!.count_li > 0)
            countCountli.text = count!!.count_li.toString()
        if (count!!.count_ei > 0)
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
        countCountf1i.text = count!!.increaseF1i().toString()
    }

    fun countDownf1i() {
        countCountf1i.text = count!!.safeDecreaseF1i().toString()
    }

    fun countUpf2i() {
        countCountf2i.text = count!!.increaseF2i().toString()
    }

    fun countDownf2i() {
        countCountf2i.text = count!!.safeDecreaseF2i().toString()
    }

    fun countUpf3i() {
        countCountf3i.text = count!!.increaseF3i().toString()
    }

    fun countDownf3i() {
        countCountf3i.text = count!!.safeDecreaseF3i().toString()
    }

    fun countUppi() {
        countCountpi.text = count!!.increasePi().toString()
    }

    fun countDownpi() {
        countCountpi.text = count!!.safeDecreasePi().toString()
    }

    fun countUpli() {
        countCountli.text = count!!.increaseLi().toString()
    }

    fun countDownli() {
        countCountli.text = count!!.safeDecreaseLi().toString()
    }

    fun countUpei() {
        countCountei.text = count!!.increaseEi().toString()
    }

    fun countDownei() {
        countCountei.text = count!!.safeDecreaseEi().toString()
    }

}
