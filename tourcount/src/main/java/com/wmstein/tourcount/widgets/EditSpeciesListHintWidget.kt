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
 * EditSpeciesListHintWidget is used by EditSpeciesListActivity
 * shows single Hint line
 *
 * Created for TourCount by wmstein on 2023-05-16,
 * last edited in java on 2023-05-16,
 * converted to Kotlin on 2023-12-07,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout

import com.wmstein.tourcount.R

import java.util.Objects

class EditSpeciesListHintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchE: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_hint, this, true)

        searchE = findViewById(R.id.searchE)
    }

    // Initially show hint
    fun setSearchE(name: String) {
        searchE.hint = name
    }

    // When searchChars exist, show editable searchChars
    fun setSearchE1(name: String) {
        searchE.setText(name)
    }

}
