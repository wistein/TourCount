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
 * last edited on 2024-11-25
 */
class EditSpNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val specNotesTitle: TextView
    private val specNotesName: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_spnotes, this, true)
        specNotesTitle = findViewById(R.id.spNotesTitle)
        specNotesName = findViewById(R.id.spNotesName)
    }

    fun setSpNotesTitle(title: String?) {
        specNotesTitle.text = title
    }

    fun setHint(hint: String?) {
        specNotesName.hint = hint
    }

    var spNotesName: String?
        get() = specNotesName.text.toString()
        set(name) {
            specNotesName.setText(name)
        }
		
}
