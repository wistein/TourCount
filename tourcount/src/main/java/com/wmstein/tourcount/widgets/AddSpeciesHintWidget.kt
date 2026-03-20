package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import com.wmstein.tourcount.R
import java.util.Objects

/*****************************************************
 * AddSpeciesHintWidget is used by AddSpeciesActivity,
 * shows single hint line with search field
 *
 * Created by wmstein on 2024-12-17
 * last edited on 2026-03-02.
 */
class AddSpeciesHintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchA: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_add_hint, this, true)
        searchA = findViewById(R.id.searchA)
    }

    fun setSearchA(name: String?) {
        searchA.hint = name
    }

}
