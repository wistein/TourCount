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
 * AddSpeciesHintWidget is used by AddSpeciesActivity,
 * shows single hint line with search field
 *
 * Created by wmstein on 2024-12-17
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout

import com.wmstein.tourcount.R

import java.util.Objects

class AddSpeciesHintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchA: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_add_hint, this, true)

        searchA = findViewById(R.id.searchA)
    }

    // Initially show hint
    fun setSearchA(name: String) {
        searchA.hint = name
    }

    // When searchChars exist, show editable searchChars
    fun setSearchA1(name: String) {
        searchA.setText(name)
    }

}
