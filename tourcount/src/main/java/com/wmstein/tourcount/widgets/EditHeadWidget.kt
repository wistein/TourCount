package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/**********************************
 * EditHeadWidget used by CountOptionsActivity
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2020-09-19,
 * converted to Kotlin on 2024-05-11,
 * last edited on 2024-11-25
 */
class EditHeadWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val sp_list_title: TextView
    private val sp_list_name: EditText
    private val notes_title: TextView
    private val notes_name: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_head, this, true)
        sp_list_title = findViewById(R.id.spListTitle)
        sp_list_name = findViewById(R.id.spListName)
        notes_title = findViewById(R.id.notesTitle)
        notes_name = findViewById(R.id.notesName)
    }

    fun setSpListTitle(title: String?) {
        sp_list_title.text = title
    }

    var spListName: String?
        get() = sp_list_name.text.toString()
        set(name) {
            sp_list_name.setText(name)
        }

    fun setNotesTitle(title: String?) {
        notes_title.text = title
    }

    var notesName: String?
        get() = notes_name.text.toString()
        set(name) {
            notes_name.setText(name)
        }

}
