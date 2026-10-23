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
 * ResultsIndivNoteWidget.kt is used by ShowResultsActivity
 *
 * Created by wmstein on 2018-03-21.
 * Last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView

import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Individuals

import java.util.Objects

class ResultsIndivNoteWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val txtBemInd: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_list_indiv_rem, this, true)

        txtBemInd = findViewById(R.id.txtBemInd)
    }

    fun setRem(individuals: Individuals) {
        txtBemInd.text = individuals.notes
    }

}
