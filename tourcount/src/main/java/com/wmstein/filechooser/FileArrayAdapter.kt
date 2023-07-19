package com.wmstein.filechooser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Locale

/**
 * FileArrayAdapter is part of filechooser.
 * It will be called within AdvFileChooser.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Modifications by wmstein on 2020-04-17,
 * converted to Kotlin on 2023-07-05
 */
internal class FileArrayAdapter(
    private val c: Context,
    private val id: Int,
    private val items: List<Option>
) : ArrayAdapter<Option>(
    c, id, items
) {
    override fun getItem(i: Int): Option {
        return items[i]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        if (v == null) {
            val vi = (c
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            v = vi.inflate(id, null)
        }
        val o = items[position]
        val im = v!!.findViewById<ImageView>(R.id.img1)
        val t1 = v.findViewById<TextView>(R.id.TextView01)
        val t2 = v.findViewById<TextView>(R.id.TextView02)
        val name = o.name?.lowercase(Locale.getDefault())
        if (name != null) {
            if (name.endsWith(".db")) im.setImageResource(R.drawable.db) else im.setImageResource(R.drawable.whitepage)
        }
        if (t1 != null) t1.text = o.name
        if (t2 != null) t2.text = o.data
        return v
    }
}