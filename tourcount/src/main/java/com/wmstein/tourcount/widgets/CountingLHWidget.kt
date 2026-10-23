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
 * Interface for widget_counting_lh_*.xml
 * used by CountingActivity
 *
 * Created by wmstein 2016-12-18,
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
class CountingLHWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
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

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        if (screenRatioBig) // > 2
            Objects.requireNonNull(inflater)
                .inflate(R.layout.widget_counting_lh_big,this, true)
        else
            Objects.requireNonNull(inflater)
                .inflate(R.layout.widget_counting_lh_small,this, true)

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
        countDownf1iButton.tag = count!!.id
        val countDownf2iButton = findViewById<ImageButton>(R.id.buttonDownLHf2i)
        countDownf2iButton.tag = count!!.id
        val countDownf3iButton = findViewById<ImageButton>(R.id.buttonDownLHf3i)
        countDownf3iButton.tag = count!!.id
        val countDownpiButton = findViewById<ImageButton>(R.id.buttonDownLHpi)
        countDownpiButton.tag = count!!.id
        val countDownliButton = findViewById<ImageButton>(R.id.buttonDownLHli)
        countDownliButton.tag = count!!.id
        val countDowneiButton = findViewById<ImageButton>(R.id.buttonDownLHei)
        countDowneiButton.tag = count!!.id
    }

    // Count up/down and set value on lefthanded screen
    fun countUpLHf1i() {
        // increase count_f1i
        countCountf1i.text = count!!.increaseF1i().toString()
    }

    fun countDownLHf1i() {
        countCountf1i.text = count!!.safeDecreaseF1i().toString()
    }

    fun countUpLHf2i() {
        countCountf2i.text = count!!.increaseF2i().toString()
    }

    fun countDownLHf2i() {
        countCountf2i.text = count!!.safeDecreaseF2i().toString()
    }

    fun countUpLHf3i() {
        countCountf3i.text = count!!.increaseF3i().toString()
    }

    fun countDownLHf3i() {
        countCountf3i.text = count!!.safeDecreaseF3i().toString()
    }

    fun countUpLHpi() {
        countCountpi.text = count!!.increasePi().toString()
    }

    fun countDownLHpi() {
        countCountpi.text = count!!.safeDecreasePi().toString()
    }

    fun countUpLHli() {
        countCountli.text = count!!.increaseLi().toString()
    }

    fun countDownLHli() {
        countCountli.text = count!!.safeDecreaseLi().toString()
    }

    fun countUpLHei() {
        countCountei.text = count!!.increaseEi().toString()
    }

    fun countDownLHei() {
        countCountei.text = count!!.safeDecreaseEi().toString()
    }

}
