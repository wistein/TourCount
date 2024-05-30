/*
 * Copyright (c) 2018. Wilhelm Stein, Bonn, Germany.
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
import com.wmstein.tourcount.database.Count

/****************************************************
 * Interface for widget_counting_head1.xml
 * used by CountingActivity
 * Created by wmstein 2016-12-18,
 * last edited in Java on 2022-04-25,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2024-05-14
 */
class CountingWidgetHead1(
    context: Context,
    private val idArray: Array<String?>,
    private val nameArray: Array<String?>,
    private val codeArray: Array<String?>,
    private val imageArray: Array<Int?>,
    private val nameArrayG: Array<String?>
) : ArrayAdapter<String?>(context, R.layout.widget_counting_head1, R.id.countName, nameArray) {
    var count: Count? = null
    private val inflater: LayoutInflater

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

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
        pSpecies.setImageResource(imageArray[position]!!)
        return head1
    }

}
