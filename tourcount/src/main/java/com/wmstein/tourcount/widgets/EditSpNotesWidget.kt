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
 * EditNotesWidget used by CountOptionsActivity
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2020-09-19,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-05-11
 */
class EditSpNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val sp_notes_title: TextView
    private val sp_notes_name: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_spnotes, this, true)
        sp_notes_title = findViewById(R.id.spNotesTitle)
        sp_notes_name = findViewById(R.id.spNotesName)
    }

    fun setSpNotesTitle(title: String?) {
        sp_notes_title.text = title
    }

    fun setHint(hint: String?) {
        sp_notes_name.hint = hint
    }

    var spNotesName: String?
        get() = sp_notes_name.text.toString()
        set(name) {
            sp_notes_name.setText(name)
        }
		
}
