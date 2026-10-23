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
 * DeleteSpeciesHintWidget is used by DelSpeciesActivity,
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

class DeleteSpeciesHintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchD: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_del_hint, this, true)

        searchD = findViewById(R.id.searchD)
    }

    // Initially show hint
    fun setSearchD(name: String) {
        searchD.hint = name
    }

    // When searchChars exist, show editable searchChars
    fun setSearchD1(name: String) {
        searchD.setText(name)
    }

}
