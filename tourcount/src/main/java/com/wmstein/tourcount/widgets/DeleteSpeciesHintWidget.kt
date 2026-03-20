package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import com.wmstein.tourcount.R
import java.util.Objects

/********************************************************
 * DeleteSpeciesHintWidget is used by DelSpeciesActivity,
 * shows single hint line with search field
 *
 * Created by wmstein on 2024-12-17
 * last edited on 2026-03-02.
 */
class DeleteSpeciesHintWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var searchD: EditText

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_del_hint, this, true)
        searchD = findViewById(R.id.searchD)
    }

    fun setSearchD(name: String?) {
        searchD.hint = name
    }

}
