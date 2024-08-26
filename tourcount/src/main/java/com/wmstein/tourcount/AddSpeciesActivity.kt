package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.AddSpeciesWidget
import com.wmstein.tourcount.widgets.HintWidget

/**********************************************************************
 * AddSpeciesActivity lets you insert new species into the species list
 * AddSpeciesActivity is called from EditSpecListActivity
 * Uses AddSpeciesWidget.kt, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is
 * determined by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TourCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-05-26
 * last edited on 2024-08-23
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    private var addArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    // the actual count data
    private var countDataSource: CountDataSource? = null

    // ID-list of not yet included species
    private lateinit var idsRemainingArrayList: Array<String?>

    // 3 ArrayLists (for names, namesG and codes) of all species from arrays.xml
    // will get reduced to lists of not yet included Species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null

    private var specName: String? = null
    private var specNameG: String? = null
    private var specCode: String? = null

    // list of species to add
    private var listToAdd: ArrayList<AddSpeciesWidget>? = null

    // Screen background
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)

        if (MyDebug.LOG) Log.d(TAG, "79, onCreate")

        setContentView(R.layout.activity_add_species)
        val addScreen = findViewById<LinearLayout>(R.id.add_screen)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        bMap = tourCount!!.decodeBitmap(
            R.drawable.addbackground,
            tourCount!!.width,
            tourCount!!.height
        )
        bg = BitmapDrawable(addScreen.resources, bMap)
        addScreen.background = bg

        listToAdd = ArrayList()

        hintArea = findViewById(R.id.showHintAddLayout)
        addArea = findViewById(R.id.addSpecLayout)

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_g)))
        codesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))

        // new onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    saveData()
                    countDataSource!!.close()

                    NavUtils.navigateUpFromSameTask(this@AddSpeciesActivity)
                }
            })
        }
    }
    // end of onCreate()

    override fun onResume() {
        super.onResume()

        // clear any existing views
        addArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        // setup the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        supportActionBar!!.setTitle(R.string.addTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Further available species
        val nw = HintWidget(this, null)
        nw.setHint1(getString(R.string.specsToAdd))
        hintArea!!.addView(nw)

        // list only new species not already contained in the species counting list
        // 1. code list of contained species
        val specCodesContainedList = ArrayList<String?>()

        // get species of the counting list
        val counts: List<Count> = countDataSource!!.allSpeciesSrtCode

        // build code ArrayList of already contained species
        for (count in counts) {
            specCodesContainedList.add(count.code)
        }

        // 2.: build lists of missing species
        val specCodesContainedListSize = specCodesContainedList.size
        var posSpec: Int

        // for already contained species reduce complete arraylists
        for (i in 0 until specCodesContainedListSize) {
            if (codesCompleteArrayList!!.contains(specCodesContainedList[i])) {
                // Remove species with code x from missing species lists.
                // Prerequisites: exactly correlated arrays of selCodes, selSpecs and selSpecs_g
                //   for all localisations
                specCode = specCodesContainedList[i]
                posSpec = codesCompleteArrayList!!.indexOf(specCode)
                namesCompleteArrayList!!.removeAt(posSpec)
                namesGCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.remove(specCode)
            }
        }
        
        // create idsRemainingArrayList for all remaining species of codesCompleteArrayList
        idsRemainingArrayList = arrayOfNulls(codesCompleteArrayList!!.size)
        var i = 0
        while (i < codesCompleteArrayList!!.size) {
            idsRemainingArrayList[i] = (i + 1).toString()
            i++
        }

        // load the data of all remaining species into the widgets
        i = 0
        while (i < codesCompleteArrayList!!.size) {
            val asw = AddSpeciesWidget(this, null)
            asw.setSpecName(namesCompleteArrayList!![i])
            asw.setSpecNameG(namesGCompleteArrayList!![i])
            asw.setSpecCode(codesCompleteArrayList!![i])
            asw.setPSpec(codesCompleteArrayList!![i]!!)
            asw.setSpecId(idsRemainingArrayList[i]!!)
            asw.setMarkSpec(false)
            addArea!!.addView(asw)
            i++
        }
    }
    // end of Resume()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("new_spec_code", specCode)
        super.onSaveInstanceState(outState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (savedInstanceState.getString("new_spec_code")!!.isNotBlank())
            specCode = savedInstanceState.getString("new_spec_code")
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId
        if (id == android.R.id.home) {
            saveData()
            countDataSource!!.close()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        countDataSource!!.close()
    }

    // mark the selected species and consider it for the species counts list
    fun checkBoxAdd(view: View) {
        val idToAdd = view.tag as Int
        val asw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget
        
        val checked = asw.getMarkSpec() // return boolean isChecked

        // put species on add list
        if (checked) {
            listToAdd!!.add(asw)
            if (MyDebug.LOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "247, addCount, code: $codeA")
            }
        }
        else {
            // remove species previously added from add list
            listToAdd!!.remove(asw)
            if (MyDebug.LOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "255, removeCount, code: $codeA")
            }
        }
    }

    private fun saveData() {
        // for all species in list to add at the end of COUNT_TABLE
        var i = 0
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            if (MyDebug.LOG) {
                Log.d(TAG, "268, saveData, code: $specCode")
            }
            try {
                countDataSource!!.createCount(specName, specCode, specNameG)
            } catch (e: Exception) {
                // nothing
            }
            i++
        }

        // sort counts table for code and contiguous index
        countDataSource!!.sortCounts()

        // store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            val editor = prefs.edit()
            editor.putString("new_spec_code", specCode)
            editor.commit()
        }
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        saveData()
        countDataSource!!.close()

        NavUtils.navigateUpFromSameTask(this)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "AddSpecAct"
    }

}
