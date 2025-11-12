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
 * EditTourNotesWidget used by EditTourNotesActivity
 * Created by wmstein on 2025-09-16,
 * last edited on 2025-09-16
 */
class EditTourNotesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val tourNotesTitle: TextView
    private val tourNotesName: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_tour_notes, this, true)
        tourNotesTitle = findViewById(R.id.trNotesTitle)
        tourNotesName = findViewById(R.id.trNotesName)
    }

    fun setTrNotesTitle(title: String?) {
        tourNotesTitle.text = title
    }

    fun setHint(hint: String?) {
        tourNotesName.hint = hint
    }

    var trNotesName: String?
        get() = tourNotesName.text.toString()
        set(name) {
            tourNotesName.setText(name)
        }
		
}
