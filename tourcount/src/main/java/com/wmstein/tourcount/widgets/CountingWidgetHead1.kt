/*
 * Copyright © 2018-2024. Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.tourcount.widgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.wmstein.tourcount.R

/****************************************************
 * Interface for widget_counting_head1.xml
 * used by CountingActivity
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-07-16
 */
class CountingWidgetHead1(
    context: Context,
    private val idArray: Array<String?>,
    private val nameArray: Array<String?>,
    private val nameArrayG: Array<String?>,
    private val codeArray: Array<String?>,
    private val imageArray: Array<Int>
) : ArrayAdapter<String?>(context, R.layout.widget_counting_head1, R.id.countName, nameArray) {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    // Shows Spinner list
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    // Shows rest of counting page
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    private fun getCustomView(position: Int, parent: ViewGroup): View {
        val head1 = inflater.inflate(R.layout.widget_counting_head1, parent, false)
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
