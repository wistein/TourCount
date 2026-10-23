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
 * EditSpeciesListHeadWidget is used by EditSpeciesListActivity
 *
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2020-09-19,
 * converted to Kotlin on 2024-05-11,
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

class EditSpeciesListHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val splistTitle: TextView
    private val splistName: EditText
    private val notestitle: TextView
    private val notesname: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_head, this, true)

        splistTitle = findViewById(R.id.spListTitle)
        splistName = findViewById(R.id.spListName)
        notestitle = findViewById(R.id.notesTitle)
        notesname = findViewById(R.id.notesName)
    }

}
