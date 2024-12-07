package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/******************************************
 * EditTitleWidget used by EditMetaActivity
 * Adopted by wmstein for TourCount 2016-02-18,
 * last edited in Java on 2019-02-12,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-11-25
 */
class EditTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widgetEditTitle: TextView
    private val widgetEditName: EditText
    private val widgetEditOName1: TextView
    private val widgetEditOName2: EditText
    private val widgetEditONotes1: TextView
    private val widgetEditONotes2: EditText

    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_title, this, true)
        widgetEditTitle = findViewById(R.id.widgetTitle)
        widgetEditName = findViewById(R.id.widgetName)

        widgetEditOName1 = findViewById(R.id.widgetOName1)
        widgetEditOName2 = findViewById(R.id.widgetOName2)

        widgetEditONotes1 = findViewById(R.id.widgetONotes1)
        widgetEditONotes2 = findViewById(R.id.widgetONotes2)
    }

    // List name headline
    fun setWidgetTitle(title: String?) {
        widgetEditTitle.text = title
    }

    // List name edittext
    var widgetName: String?
        get() = widgetEditName.text.toString()
        set(name) {
            widgetEditName.setText(name)
        }

    // Observer name headline
    fun setWidgetOName1(title: String?) {
        widgetEditOName1.text = title
    }

    var widgetOName2: String?
        get() = widgetEditOName2.text.toString()
        set(name) {
            widgetEditOName2.setText(name)
        }

    // notes
    fun setWidgetONotes1(title: String?) {
        widgetEditONotes1.text = title
    }

    fun setHintN(hint: String?) {
        widgetEditONotes2.hint = hint
    }

    var widgetONotes2: String?
        get() = widgetEditONotes2.text.toString()
        set(name) {widgetEditONotes2.setText(name)
        }

}
