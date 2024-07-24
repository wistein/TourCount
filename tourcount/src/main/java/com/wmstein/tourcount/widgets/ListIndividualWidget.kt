package com.wmstein.tourcount.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import com.wmstein.tourcount.AutoFitText
import com.wmstein.tourcount.R
import com.wmstein.tourcount.database.Individuals
import java.util.Objects
import kotlin.math.roundToInt

/**********************************
 * Created by wmstein on 2018-02-22
 * used by ListSpeciesActivity
 * Last edited in Java on 2022-03-26,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-07-23
 */
class ListIndividualWidget(context: Context, attrs: AttributeSet?) :
    RelativeLayout(context, attrs) {
    private val txtIndLoc: TextView
    private val txtIndSex: TextView
    private val txtIndStad: TextView
    private val txtIndLa: TextView
    private val txtIndLo: TextView
    private val txtIndHt: TextView
    private val txtIndStat: TextView
    val txtIndCnt: AutoFitText
    private var phase123: Boolean = false // butterfly ♂♀, ♂ or ♀

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        Objects.requireNonNull(inflater).inflate(R.layout.widget_list_individual, this, true)
        txtIndLoc = findViewById(R.id.txtIndLoc)
        txtIndSex = findViewById(R.id.txtIndSex)
        txtIndStad = findViewById(R.id.txtIndStad)
        txtIndLa = findViewById(R.id.txtIndLa)
        txtIndLo = findViewById(R.id.txtIndLo)
        txtIndHt = findViewById(R.id.txtIndHt)
        txtIndStat = findViewById(R.id.txtIndStat)
        txtIndCnt = findViewById(R.id.txtIndCnt)
    }

    fun setIndividual(individual: Individuals) {
        // Locality
        txtIndLoc.text = individual.locality

        // Sexus
        txtIndSex.text = individual.sex

        // Stadium
        if (individual.stadium!!.isNotEmpty()) {
            txtIndStad.text = individual.stadium!!.substring(0, 1)
            val sta =
                individual.stadium!!.substring(0, 1) // Imago: F,B; Egg: E; Larva: R,C; Pupa: P
            phase123 = sta == "F" || sta == "B" // true for Imago
        } else txtIndStad.text = "-"

        // Latitude
        var slen: Int
        if (individual.coord_x == 0.0) {
            txtIndLa.text = ""
        } else {
            slen = individual.coord_x.toString().length
            if (slen > 8) {
                txtIndLa.text = individual.coord_x.toString().substring(0, 8)
            } else {
                txtIndLa.text = individual.coord_x.toString()
            }
        }

        // Longitude
        if (individual.coord_y == 0.0) {
            txtIndLo.text = ""
        } else {
            slen = individual.coord_y.toString().length
            if (slen > 8) {
                txtIndLo.text = individual.coord_y.toString().substring(0, 8)
            } else {
                txtIndLo.text = individual.coord_y.toString()
            }
        }

        // Height
        if (individual.coord_z == 0.0) {
            txtIndHt.text = ""
        } else {
            txtIndHt.text = String.format("%s m", individual.coord_z.roundToInt())
        }

        // State
        val indStat = individual.state_1_6.toString()
        if (phase123) {
            if (indStat == "0") txtIndStat.text = "-" else txtIndStat.text =
                individual.state_1_6.toString()
        } else txtIndStat.text = "-"

        // Individual count
        val text = individual.icount.toString()
        txtIndCnt.setText(text) // In Kotlin EditText requires text as Editable?, not as String
    }

    fun getIndNotes(individual: Individuals): String? {
        return individual.notes
    }

}
