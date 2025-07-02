package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.DbHelper
import com.wmstein.tourcount.database.Head
import com.wmstein.tourcount.database.HeadDataSource
import com.wmstein.tourcount.database.Individuals
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.ListIndivNoteWidget
import com.wmstein.tourcount.widgets.ListIndividualWidget
import com.wmstein.tourcount.widgets.ListLineWidget
import com.wmstein.tourcount.widgets.ListLocationWidget
import com.wmstein.tourcount.widgets.ListMetaWidget
import com.wmstein.tourcount.widgets.ListSpeciesWidget
import com.wmstein.tourcount.widgets.ListSumWidget
import com.wmstein.tourcount.widgets.ListTitleWidget
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/*********************************************************
 * ShowResultsActivity shows list of counting results
 * Created by wmstein on 2016-03-15 as ListSpeciesActivity,
 * last edited in Java on 2022-05-21,
 * converted to Kotlin on 2023-07-09,
 * renamed to ShowResultsActivity on 2025-02-25,
 * last edited on 2025-06-29
 */
class ShowResultsActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    private var specArea: LinearLayout? = null

    // Data
    private var countDataSource: CountDataSource? = null
    private var sectionDataSource: SectionDataSource? = null
    private var headDataSource: HeadDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null
    var head: Head? = null
    private var lsw: ListSumWidget? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var awakePref = false
    private var outPref: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "68, onCreate")

        tourCount = application as TourCountApplication

        awakePref = prefs.getBoolean("pref_awake", true)
        outPref = prefs.getString("pref_sort_output", "names") // sort mode output

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_list_species)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.listSpecScreen)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. This solution sets
            // only the bottom, left, and right dimensions, but you can apply whichever
            // insets are appropriate to your layout. You can also update the view padding
            // if that's more appropriate.
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                leftMargin = insets.left
                bottomMargin = insets.bottom
                rightMargin = insets.right
            }

            // Return CONSUMED if you don't want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        countDataSource = CountDataSource(this)
        sectionDataSource = SectionDataSource(this)
        headDataSource = HeadDataSource(this)
        individualsDataSource = IndividualsDataSource(this)

        supportActionBar!!.title = getString(R.string.viewSpecTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        specArea = findViewById(R.id.listSpecLayout)

        // new onBackPressed logic
        val callback = object :  OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (MyDebug.DLOG) Log.i(TAG, "125, handleOnBackPressed")
                finish()
                remove()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.DLOG) Log.i(TAG, "137, onResume")

        if (awakePref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // setup the data sources
        headDataSource!!.open()
        sectionDataSource!!.open()
        countDataSource!!.open()
        individualsDataSource!!.open()

        // build Show Results screen
        specArea!!.removeAllViews()
        loadData()
    }
    // End of onResume()

    // fill ListSpeciesWidget with relevant counts and sections data
    private fun loadData() {
        var summf = 0
        var summ = 0
        var sumf = 0
        var sump = 0
        var suml = 0
        var sumo = 0
        var sumsp = 0  // sum of different species
        var sumind = 0 // sum of counted individuals
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

        //load head and meta data from DB
        head = headDataSource!!.head
        val section = sectionDataSource!!.section

        // Build the screen
        // 1. Display list name and observer name by ListTitleWidget
        val ltw = ListTitleWidget(this, null)
        ltw.setListTitle(getString(R.string.titleEdit))
        ltw.setListName(section.name)

        ltw.setWidgetName1(getString(R.string.inspector))
        ltw.setWidgetName2(head!!.observer)
        specArea!!.addView(ltw)

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
        dbHandler.close()

        lo = (loMax + loMin) / 2 // average longitude
        la = (laMax + laMin) / 2 // average latitude

        // Simple distance calculation between 2 coordinates within the temperate zone in meters (Pythagoras):
        //   uc = (((loMax-loMin)*71500)² + ((laMax-laMin)*111300)²)½ 
        uc = sqrt(
            ((loMax - loMin) * 71500).pow(2.0) + ((laMax - laMin) * 111300).pow(2.0)
        )
        uc = round(uc / 2) + 20 // average uncertainty radius + default gps uncertainty
        if (uc <= uncer1) uc = uncer1

        // 2. Display the location data
        val llw = ListLocationWidget(this, null)
        llw.setLocationWidget(section)
        llw.setWidgetDla2(la)
        llw.setWidgetDlo2(lo)
        llw.setWidgetMuncert2(uc)
        specArea!!.addView(llw)

        // 3. Display the date, time, temperature, wind and clouds data
        val lmw = ListMetaWidget(this, null)
        lmw.setListMetaWidget(section)

        lmw.setWidgetNotes1(getString(R.string.notesHere))
        lmw.setWidgetNotes2(section.notes)
        specArea!!.addView(lmw)

        // load the species data
        val specs: List<Count> // List of sorted species
        if (outPref.equals("names")) {
            // sort criteria are name and code
            specs = countDataSource!!.cntSpeciesSrtName
        } else {
            // sort criteria are name and section
            specs = countDataSource!!.cntSpeciesSrtCode
        }

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

        // display line and the totals
        lsw = ListSumWidget(this, null)
        lsw!!.setSum(sumsp, sumind)
        specArea!!.addView(lsw)

        // display all individuals by adding them to listSpecies layout
        var specCnt: Int
        var indivs: List<Individuals> // List of individuals
        var iwidget: ListIndividualWidget

        for (spec in specs) {
            val widget = ListSpeciesWidget(this, null)
            widget.setCount(spec)
            specCntf1i = widget.getSpec_countf1i(spec)
            specCntf2i = widget.getSpec_countf2i(spec)
            specCntf3i = widget.getSpec_countf3i(spec)
            specCntpi = widget.getSpec_countpi(spec)
            specCntli = widget.getSpec_countli(spec)
            specCntei = widget.getSpec_countei(spec)
            specCnt = (specCntf1i + specCntf2i + specCntf3i + specCntpi + specCntli + specCntei)

            // fill widget only for counted species
            if (specCnt > 0) {
                specArea!!.addView(widget)
                val iName = widget.getSpec_name(spec)
                indivs = individualsDataSource!!.getIndividualsByName(iName!!)
                for (indiv in indivs) {
                    iwidget = ListIndividualWidget(this, null)

                    // load the individuals data
                    iwidget.setIndividual(indiv)
                    specArea!!.addView(iwidget)

                    // show individual notes only when provided
                    val rwidget = ListIndivNoteWidget(this, null)
                    val tRem: String? =
                        if (iwidget.getIndNotes(indiv) == null) ""
                        else iwidget.getIndNotes(indiv)
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
    // end of loadData()

    override fun onPause() {
        super.onPause()

        if (MyDebug.DLOG) Log.i(TAG, "339, onPause")

        // close the data sources
        headDataSource!!.close()
        sectionDataSource!!.close()
        countDataSource!!.close()
        individualsDataSource!!.close()

        if (awakePref) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        specArea = null
    }

    companion object {
        private const val TAG = "ListSpeciesAct"
    }

}
