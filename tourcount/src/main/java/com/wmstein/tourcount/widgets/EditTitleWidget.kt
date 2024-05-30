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
 * last edited on 2024-05-10
 */
class EditTitleWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val widget_title: TextView
    private val widget_name: EditText
    private val widget_oname1: TextView
    private val widget_oname2: EditText
    private val widget_onotes1: TextView
    private val widget_onotes2: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_title, this, true)
        widget_title = findViewById(R.id.widgetTitle)
        widget_name = findViewById(R.id.widgetName)

        widget_oname1 = findViewById(R.id.widgetOName1)
        widget_oname2 = findViewById(R.id.widgetOName2)

        widget_onotes1 = findViewById(R.id.widgetONotes1)
        widget_onotes2 = findViewById(R.id.widgetONotes2)
    }

    // List name headline
    fun setWidgetTitle(title: String?) {
        widget_title.text = title
    }

    // List name edittext
    var widgetName: String?
        get() = widget_name.text.toString()
        set(name) {
            widget_name.setText(name)
        }

    // Observer name headline
    fun setWidgetOName1(title: String?) {
        widget_oname1.text = title
    }

    var widgetOName2: String?
        get() = widget_oname2.text.toString()
        set(name) {
            widget_oname2.setText(name)
        }

    // notes
    fun setWidgetONotes1(title: String?) {
        widget_onotes1.text = title
    }

    fun setHintN(hint: String?) {
        widget_onotes2.hint = hint
    }

    var widgetONotes2: String?
        get() = widget_onotes2.text.toString()
        set(name) {widget_onotes2.setText(name)
        }

}
