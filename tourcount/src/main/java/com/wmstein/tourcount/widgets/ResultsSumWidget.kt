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
 * ResultsSumWidget shows count totals area for
 * ShowResultsActivity that shows the results page
 *
 * Created for TourCount by wmstein on 2017-05-27,
 * last edited in Java on 2021-01-26,
 * converted to Kotlin on 2025-03-04,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.tourcount.R

import java.util.Objects

class ResultsSumWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val sumSpecies: TextView
    private val sumIndividuals: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_sum_species, this, true)

        sumSpecies = findViewById(R.id.sumSpecies)
        sumIndividuals = findViewById(R.id.sumIndividuals)
    }

    @SuppressLint("SetTextI18n")
    fun setSum(sumsp: Int, sumind: Int) {
        sumSpecies.text = sumsp.toString()
        sumIndividuals.text = sumind.toString()
    }

}
