package com.wmstein.tourcount.widgets

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * DeleteSpeciesWidget is used by DelSpeciesActivity
 * shows list of selectable species with name, code, picture and add checkbox
 *
 * Created for TourCount by wmstein on 2024-08-22,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Count

import java.io.Serializable
import java.util.Objects

class DeleteSpeciesWidget(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs),
    Serializable {
    @Transient
    private val specName: TextView

    @Transient
    private val specNameG: TextView

    @Transient
    private val specCode: TextView

    @Transient
    private val spId: TextView

    @Transient
    private val specPic: ImageView
    private val markButton: CheckBox

    val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as LayoutInflater

    init {
        Objects.requireNonNull(inflater)
            .inflate(R.layout.widget_delete_spec, this, true)

        specName = findViewById(R.id.spName)
        specNameG = findViewById(R.id.spNameG)
        specCode = findViewById(R.id.spCode)
        spId = findViewById(R.id.spId)
        specPic = findViewById(R.id.spPic)
        markButton = findViewById(R.id.checkBoxDel)
        markButton.tag = 0
    }

    fun setSpecName(name: String) {
        specName.text = name
    }

    fun setSpecNameG(nameg: String) {
        specNameG.text = nameg
    }

    fun setSpecCode(code: String) {
        specCode.text = code
    }

    fun setSpecId(id: String) {
        spId.text = id
        markButton.tag = id.toInt() - 1
    }

    // Set picture of species
    @SuppressLint("DiscouragedApi")
    fun setPSpec(spec: Count) {
        val rName = "p" + spec.code // species picture resource name
        val resId = resources.getIdentifier(rName, "drawable", context.packageName)
        if (resId != 0) {
            specPic.setImageResource(resId)
        }
    }

    fun getSpecCode(): String {
        return specCode.text.toString()
    }

    // Get state of delete checkbox
    fun getMarkSpec(): Boolean {
        val checked: Boolean = markButton.isChecked
        return checked
    }

}
