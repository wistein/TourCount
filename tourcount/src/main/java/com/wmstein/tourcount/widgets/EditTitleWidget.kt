package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/*************************************
 * EditTitleWidget used by EditMetaActivity
 * Adopted by wmstein for TourCount 2016-02-18,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05
 */
class EditTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget_title: TextView
    private val section_name: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_title, this, true)
        widget_title = findViewById(R.id.widgetTitle)
        section_name = findViewById(R.id.sectionName)
    }

    fun setWidgetTitle(title: String?) {
        widget_title.text = title
    }

    fun setHint(hint: String?) {
        section_name.hint = hint
    }

    var sectionName: String?
        get() = section_name.text.toString()
        set(name) {
            section_name.setText(name)
        }
}