package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.tourcount.R
import com.wmstein.tourcount.TourCountApplication
import com.wmstein.tourcount.database.Count
import java.util.Objects

/*******************************************************
 * ListSpeciesWidget shows count info area for a species
 * ListSpeciesActivity shows the result page
 * Created for TourCount by wmstein on 15.03.2016
 * Last edited in Java on 2020-10-18,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2023-11-24.
 */
class ListSpeciesWidget(context: Context, attrs: AttributeSet?) : RelativeLayout(context, attrs) {
    private val txtSpecName: TextView
    private val txtSpecNameG: TextView
    private val picSpecies: ImageView
    private val specCount: TextView
    private val specCountf1i: TextView
    private val specCountf2i: TextView
    private val specCountf3i: TextView
    private val specCountpi: TextView
    private val specCountli: TextView
    private val specCountei: TextView
    private val txtSpecRem: TextView
    private val txtSpecRemT: TextView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_species, this, true)
        txtSpecName = findViewById(R.id.txtSpecName)
        txtSpecNameG = findViewById(R.id.txtSpecNameG)
        txtSpecRemT = findViewById(R.id.txtSpecRemT)
        txtSpecRem = findViewById(R.id.txtSpecRem)
        specCount = findViewById(R.id.specCount)
        specCountf1i = findViewById(R.id.specCountf1i)
        specCountf2i = findViewById(R.id.specCountf2i)
        specCountf3i = findViewById(R.id.specCountf3i)
        specCountpi = findViewById(R.id.specCountpi)
        specCountli = findViewById(R.id.specCountli)
        specCountei = findViewById(R.id.specCountei)
        picSpecies = findViewById(R.id.picSpecies)
    }

    fun setCount(spec: Count) {
        val rname = "p" + spec.code // species picture resource name

        // make instance of class TransektCountApplication to reference non-static method 
        val tourCountApp = TourCountApplication()
        val resId = tourCountApp.getResId(rname)
        if (resId != 0) {
            picSpecies.setImageResource(resId)
        }
        val spCount = (spec.count_f1i + spec.count_f2i + spec.count_f3i + spec.count_pi
                + spec.count_li + spec.count_ei)
        txtSpecName.text = spec.name
        if (spec.name_g != null) {
            if (spec.name_g!!.isNotEmpty()) {
                txtSpecNameG.text = spec.name_g
            } else {
                txtSpecNameG.text = ""
            }
        }
        specCount.text = spCount.toString()
        if (spec.count_f1i > 0) specCountf1i.text = spec.count_f1i.toString()
        if (spec.count_f2i > 0) specCountf2i.text = spec.count_f2i.toString()
        if (spec.count_f3i > 0) specCountf3i.text = spec.count_f3i.toString()
        if (spec.count_pi > 0) specCountpi.text = spec.count_pi.toString()
        if (spec.count_li > 0) specCountli.text = spec.count_li.toString()
        if (spec.count_ei > 0) specCountei.text = spec.count_ei.toString()
        if (spec.notes != null) {
            if (spec.notes!!.isNotEmpty()) {
                txtSpecRem.text = spec.notes
                txtSpecRem.visibility = VISIBLE
                txtSpecRemT.visibility = VISIBLE
            }
        } else {
            txtSpecRem.visibility = GONE
            txtSpecRemT.visibility = GONE
        }
    }

    //Parameter specCnt* for use in ListSpeciesActivity
    fun getSpec_countf1i(spec: Count): Int {
        return spec.count_f1i
    }

    fun getSpec_countf2i(spec: Count): Int {
        return spec.count_f2i
    }

    fun getSpec_countf3i(spec: Count): Int {
        return spec.count_f3i
    }

    fun getSpec_countpi(spec: Count): Int {
        return spec.count_pi
    }

    fun getSpec_countli(spec: Count): Int {
        return spec.count_li
    }

    fun getSpec_countei(spec: Count): Int {
        return spec.count_ei
    }

    //Parameter spec_name for use in ListSpeciesActivity
    fun getSpec_name(newcount: Count): String? {
        return newcount.name
    }

}
