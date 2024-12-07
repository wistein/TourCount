package com.wmstein.tourcount.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Count
import java.io.Serializable
import java.util.Objects

/************************************************
 * Used by EditSpecListActivity
 * shows line with species name, code and delete button
 * Adopted for TourCount by wmstein on 2016-02-18
 * last edited in Java on 2020-10-17,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-08-23
 */
class EditSpeciesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @JvmField
    var countId = 0

    @Transient
    private val countName: EditText

    @Transient
    private val countNameG: EditText

    @Transient
    private val countCode: EditText

    @Transient
    private val pSpecies: ImageView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_edit_count, this, true)
        countName = findViewById(R.id.countName)
        countNameG = findViewById(R.id.countNameG)
        countCode = findViewById(R.id.countCode)
        pSpecies = findViewById(R.id.pSpec)
    }

    fun getCountName(): String {
        return countName.text.toString()
    }

    fun setCountName(name: String?) {
        countName.setText(name)
    }

    fun getCountNameG(): String {
        return countNameG.text.toString()
    }

    fun setCountNameG(name: String?) {
        countNameG.setText(name)
    }

    fun getCountCode(): String {
        return countCode.text.toString()
    }

    fun setCountCode(name: String?) {
        countCode.setText(name)
    }

    fun setCountId(id: Int) {
        countId = id
    }

    @SuppressLint("DiscouragedApi")
    fun setPSpec(spec: Count) {
        val rName = "p" + spec.code // species picture resource name
        val resId = resources.getIdentifier(rName, "drawable", context.packageName)
        if (resId != 0) {
            pSpecies.setImageResource(resId)
        }
    }

}