package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.DbHelper
import com.wmstein.tourcount.database.HeadDataSource
import com.wmstein.tourcount.database.Individuals
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.ListHeadWidget
import com.wmstein.tourcount.widgets.ListIndivRemWidget
import com.wmstein.tourcount.widgets.ListIndividualWidget
import com.wmstein.tourcount.widgets.ListLineWidget
import com.wmstein.tourcount.widgets.ListMetaWidget
import com.wmstein.tourcount.widgets.ListSpeciesWidget
import com.wmstein.tourcount.widgets.ListSumWidget
import com.wmstein.tourcount.widgets.ListTitleWidget
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/****************************************************
 * ListSpeciesActivity shows list of counting results
 * Created by wmstein on 2016-03-15,
 * last edited in Java on 2022-05-21,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2023-11-24
 */
class ListSpeciesActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var tourCount: TourCountApplication? = null

    private var specArea: LinearLayout? = null

    // preferences
    private var prefs = TourCountApplication.getPrefs()
    private var awakePref = false
    private var sortPref: String? = null

    // the actual data
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var headDataSource: HeadDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null
    private var lsw: ListSumWidget? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication
        prefs.registerOnSharedPreferenceChangeListener(this)
        awakePref = prefs.getBoolean("pref_awake", true)
        sortPref = prefs.getString("pref_sort_sp", "none") // sorted species list

        setContentView(R.layout.activity_list_species)
        countDataSource = CountDataSource(this)
        sectionDataSource = SectionDataSource(this)
        headDataSource = HeadDataSource(this)
        individualsDataSource = IndividualsDataSource(this)
        val resultsScreen = findViewById<ScrollView>(R.id.listSpecScreen)
        resultsScreen.background = tourCount!!.background
        supportActionBar!!.title = getString(R.string.viewSpecTitle)
        specArea = findViewById(R.id.listSpecLayout)
        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onResume() {
        super.onResume()
        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // clear existing views
        specArea!!.removeAllViews()
        loadData()
    }

    // fill ListSpeciesWidget with relevant counts and sections data
    private fun loadData() {
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sumo = 0
        var sumsp = 0
        var sumind = 0 // sum of counted species, sum of counted individuals
        var longi: Double
        var lati: Double
        var uncer: Double
        var frst = 0
        val lo: Double
        val la: Double
        var loMin = 0.0
        var loMax = 0.0
        var laMin = 0.0
        var laMax = 0.0
        var uc: Double
        var uncer1 = 0.0
        headDataSource!!.open()
        sectionDataSource!!.open()

        //load head and meta data
        val head = headDataSource!!.head
        val section = sectionDataSource!!.section

        // display the list name
        val elw = ListTitleWidget(this, null)
        elw.setListTitle(getString(R.string.titleEdit))
        elw.setListName(section.name)
        specArea!!.addView(elw)

        // display the list remark
        val erw = ListTitleWidget(this, null)
        erw.setListTitle(getString(R.string.notesHere))
        erw.setListName(section.notes)
        specArea!!.addView(erw)

        // display the head data
        val ehw = ListHeadWidget(this, null)
        ehw.setWidgetLCo(getString(R.string.country))
        ehw.setWidgetLCo1(section.country)
        ehw.setWidgetLName(getString(R.string.inspector))
        ehw.setWidgetLName1(head.observer)
        specArea!!.addView(ehw)

        // setup the data sources
        countDataSource!!.open()
        individualsDataSource!!.open()

        // Calculate mean average values for coords and uncertainty
        val dbHandler = DbHelper(this)
        val database = dbHandler.writableDatabase
        val curAInd: Cursor = database.rawQuery("select * from " + DbHelper.INDIVIDUALS_TABLE, null)
        while (curAInd.moveToNext()) {
            longi = curAInd.getDouble(4)
            lati = curAInd.getDouble(3)
            uncer = round(curAInd.getDouble(6))
            if (longi != 0.0) // has coordinates
            {
                //Toast.makeText(getApplicationContext(), longi, Toast.LENGTH_SHORT).show();
                if (MyDebug.LOG) Log.d(TAG, "longi $longi")
                if (frst == 0) {
                    loMin = longi
                    loMax = longi
                    laMin = lati
                    laMax = lati
                    uncer1 = uncer
                    frst = 1 // just 1 with coordinates
                } else {
                    loMin = loMin.coerceAtMost(longi)
                    loMax = loMax.coerceAtLeast(longi)
                    laMin = laMin.coerceAtMost(lati)
                    laMax = laMax.coerceAtLeast(lati)
                    uncer1 = uncer1.coerceAtLeast(uncer)
                }
            }
        }
        curAInd.close()
        lo = (loMax + loMin) / 2 // average longitude
        la = (laMax + laMin) / 2 // average latitude

        // Simple distance calculation between 2 coordinates within the temperate zone in meters (Pythagoras):
        //   uc = (((loMax-loMin)*71500)² + ((laMax-laMin)*111300)²)½ 
        uc = sqrt(
            ((loMax - loMin) * 71500).pow(2.0) + ((laMax - laMin) * 111300).pow(2.0)
        )
        uc = round(uc / 2) + 20 // average uncertainty radius + default gps uncertainty
        if (uc <= uncer1) uc = uncer1

        // display the meta data
        val etw = ListMetaWidget(this, null)
        etw.setMetaWidget(section)
        etw.setWidget_dla2(la)
        etw.setWidget_dlo2(lo)
        etw.setWidget_muncert2(uc)
        specArea!!.addView(etw)

        // load the species data
        val specs = when (sortPref) {
            "names_alpha" -> countDataSource!!.cntSpeciesSrtName
            "codes" -> countDataSource!!.cntSpeciesSrtCode
            else -> countDataSource!!.cntSpecies
        } //List of species

        // calculate the totals
        var specCntf1i: Int
        var specCntf2i: Int
        var specCntf3i: Int
        var specCntpi: Int
        var specCntli: Int
        var specCntei: Int
        for (spec in specs) {
            val widget = ListSpeciesWidget(this, null)
            widget.setCount(spec)
            specCntf1i = widget.getSpec_countf1i(spec)
            specCntf2i = widget.getSpec_countf2i(spec)
            specCntf3i = widget.getSpec_countf3i(spec)
            specCntpi = widget.getSpec_countpi(spec)
            specCntli = widget.getSpec_countli(spec)
            specCntei = widget.getSpec_countei(spec)
            summf += specCntf1i
            summ += specCntf2i
            sumf += specCntf3i
            sump += specCntpi
            suml += specCntli
            sumo += specCntei
            sumind = (sumind + specCntf1i + specCntf2i + specCntf3i + specCntpi
                    + specCntli + specCntei) // sum of counted individuals
            sumsp += 1 // sum of counted species
        }

        // display the totals
        lsw = ListSumWidget(this, null)
        lsw!!.setSum(sumsp, sumind)
        specArea!!.addView(lsw)
        var specCnt: Int
        var indivs: List<Individuals> // List of individuals
        // display all the counts by adding them to listSpecies layout
        for (spec in specs) {
            val widget = ListSpeciesWidget(this, null)
            widget.setCount(spec)
            specCntf1i = widget.getSpec_countf1i(spec)
            specCntf2i = widget.getSpec_countf2i(spec)
            specCntf3i = widget.getSpec_countf3i(spec)
            specCntpi = widget.getSpec_countpi(spec)
            specCntli = widget.getSpec_countli(spec)
            specCntei = widget.getSpec_countei(spec)
            specCnt = (specCntf1i + specCntf2i + specCntf3i
                    + specCntpi + specCntli + specCntei)


            // fill widget only for counted species
            if (specCnt > 0) {
                specArea!!.addView(widget)
                val iName = widget.getSpec_name(spec)
                indivs = individualsDataSource!!.getIndividualsByName(iName!!)
                for (indiv in indivs) {
                    val iwidget = ListIndividualWidget(this, null)
                    //load the individuals data
                    iwidget.setIndividual(indiv)
                    specArea!!.addView(iwidget)

                    // show individual notes only when provided
                    val rwidget = ListIndivRemWidget(this, null)
                    val tRem: String? = if (iwidget.getIndNotes(indiv) == null) "" else iwidget.getIndNotes(indiv)
                    if (tRem!!.isNotEmpty()) {
                        rwidget.setRem(indiv)
                        specArea!!.addView(rwidget)
                    }
                }
            }
        }
        val lwidget = ListLineWidget(this, null)
        specArea!!.addView(lwidget)
    }

    override fun onPause() {
        super.onPause()

        // close the data sources
        headDataSource!!.close()
        countDataSource!!.close()
        sectionDataSource!!.close()
        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // puts up function to back button
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        NavUtils.navigateUpFromSameTask(this)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        val resultsScreen = findViewById<ScrollView>(R.id.listSpecScreen)
        resultsScreen.background = null
        resultsScreen.background = tourCount!!.setBackground()
        prefs?.registerOnSharedPreferenceChangeListener(this)
        awakePref = prefs!!.getBoolean("pref_awake", true)
        sortPref = prefs.getString("pref_sort_sp", "none") // sorted species list
    }

    companion object {
        private const val TAG = "tourcountListSpeciesAct"
    }

}