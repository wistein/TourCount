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
 * Interface for widget_counting_species_select.xml
 * used by CountingActivity.kt
 *
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import com.wmstein.tourcount.R

class CountingHead1Widget(
    context: Context,
    private val idArray: Array<String?>,
    private val nameArray: Array<String?>,
    private val nameArrayG: Array<String?>,
    private val codeArray: Array<String?>,
    private val imageArray: Array<Int>
) : ArrayAdapter<String?>(
    context, R.layout.widget_counting_species_select,
    R.id.countName, nameArray) {

    private val inflater: LayoutInflater = context
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    // Shows Spinner list
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    // Shows rest of counting page
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    private fun getCustomView(position: Int, parent: ViewGroup): View {
        val head1 = inflater.inflate(
            R.layout.widget_counting_species_select, parent, false)

        val countId = head1.findViewById<TextView>(R.id.countId)
        countId.text = idArray[position]

        val countName = head1.findViewById<TextView>(R.id.countName)
        countName.text = nameArray[position]

        val countNameg = head1.findViewById<TextView>(R.id.countNameg)
        countNameg.text = nameArrayG[position]

        val countCode = head1.findViewById<TextView>(R.id.countCode)
        countCode.text = codeArray[position]

        val pSpecies = head1.findViewById<ImageView>(R.id.pSpecies)
        pSpecies.setImageResource(imageArray[position])

        return head1
    }

}
