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
 * EditTourNotesWidget is used by EditTourNotesActivity
 *
 * Created by wmstein on 2025-09-16,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.tourcount.R

import java.util.Objects

class EditTourNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val tourNotesTitle: TextView
    private val tourNotesName: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_tour_notes, this, true)

        tourNotesTitle = findViewById(R.id.trNotesTitle)
        tourNotesName = findViewById(R.id.trNotesName)
    }

    fun setTrNotesTitle(title: String) {
        tourNotesTitle.text = title
    }

    fun setHint(hint: String) {
        tourNotesName.hint = hint
    }

    var trNotesName: String
        get() = tourNotesName.text.toString()
        set(name) {
            tourNotesName.setText(name)
        }
		
}
