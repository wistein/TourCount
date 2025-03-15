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
 * Adopted by wmstein on 2016-06-18,
 * modifications for TourCount by wmstein on 2020-04-17,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2025-03-12
 */
internal class FileArrayAdapter(
    private val faaContext: Context,
    private val id: Int,
    private val items: List<Option>
) : ArrayAdapter<Option>(
    faaContext, id, items
) {
    override fun getItem(i: Int): Option {
        return items[i]
    }

    // Constructor of entries for the file list
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var fileListRow = convertView

        // If there is still no row for the file list
        if (fileListRow == null) {
            val vi = (faaContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            fileListRow = vi.inflate(id, null)
        }
        
        val fileItem = items[position]
        val im = fileListRow!!.findViewById<ImageView>(R.id.img1)
        val t1 = fileListRow.findViewById<TextView>(R.id.TextView01)
        val t2 = fileListRow.findViewById<TextView>(R.id.TextView02)
        val name = fileItem.name?.lowercase(Locale.getDefault())

        if (name != null) {
            if (name.endsWith(".db")) im.setImageResource(R.drawable.db)
            else if (name.endsWith(".csv")) im.setImageResource(R.drawable.outline_format_align_left_24)
            else im.setImageResource(R.drawable.baseline_insert_drive_file_24)
        }

        if (t1 != null) t1.text = fileItem.name
        if (t2 != null) t2.text = fileItem.data
        return fileListRow
    }

}
