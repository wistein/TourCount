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
 * EditMetaTitleWidget is used by EditMetaActivity
 *
 * Adopted by wmstein for TourCount 2016-02-18,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05,
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

class EditMetaTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetEditTitle: TextView
    private val widgetEditName: EditText
    private val widgetEditOName1: TextView
    private val widgetEditOName2: EditText
    private val widgetEditONotes1: TextView
    private val widgetEditONotes2: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_edit_title, this, true)

        widgetEditTitle = findViewById(R.id.widgetTitle)
        widgetEditName = findViewById(R.id.widgetName)

        widgetEditOName1 = findViewById(R.id.widgetOName1)
        widgetEditOName2 = findViewById(R.id.widgetOName2)

        widgetEditONotes1 = findViewById(R.id.widgetONotes1)
        widgetEditONotes2 = findViewById(R.id.widgetONotes2)
    }

    // Tour name headline
    fun setWidgetTitle(title: String) {
        widgetEditTitle.text = title
    }

    // Tour name edittext
    var widgetName: String
        get() = widgetEditName.text.toString()
        set(name) {
            widgetEditName.setText(name)
        }

    // Observer name
    fun setWidgetOName1(title: String) {
        widgetEditOName1.text = title
    }

    var widgetOName2: String
        get() = widgetEditOName2.text.toString()
        set(name) {
            widgetEditOName2.setText(name)
        }

    // Notes
    fun setWidgetONotes1(title: String) {
        widgetEditONotes1.text = title
    }

    var widgetONotes2: String
        get() = widgetEditONotes2.text.toString()
        set(name) {widgetEditONotes2.setText(name)
        }

}
