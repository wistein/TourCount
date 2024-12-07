package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import com.wmstein.tourcount.R
import java.util.Objects

/********************************************
 * HintEditWidget used by EditSpeciesActivity
 * shows single Hint line
 * Created for TourCount by wmstein on 2023-05-16,
 * last edited in java on 2023-05-16,
 * converted to Kotlin on 2023-12-07,
 * last edited on 2024-10-17.
 */
class HintEditWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchE: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_hint, this, true)
        searchE = findViewById(R.id.searchE)
    }

    fun setSearchE(name: String?) {
        searchE.hint = name
    }

}
