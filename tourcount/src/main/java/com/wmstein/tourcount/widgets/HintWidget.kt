package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import java.util.Objects

/****************************************
 * HintWidget used by EditSpeciesActivity
 * shows single Hint line
 * Created for TourCount by wmstein on 2023-05-16,
 * last edited in java on 2023-05-16,
 * converted to Kotlin on 2023-12-07,
 * last edited on 2024-09-06.
 */
class HintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private val textView: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_hint, this, true)
        textView = findViewById(R.id.hint_text)
    }

    // todo fun ist überflüssig
    fun setHint1(notes: String?) {
        textView.text = notes
    }

}
