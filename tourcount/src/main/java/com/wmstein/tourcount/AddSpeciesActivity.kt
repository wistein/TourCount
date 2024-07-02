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
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.AddSpeciesWidget

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
 * converted to Kotlin on 2023-07-06
 * last edited on 2024-07-02
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    private var addArea: LinearLayout? = null

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
        val addScreen = findViewById<ScrollView>(R.id.add_screen)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        bMap = tourCount!!.decodeBitmap(
            R.drawable.abackground,
            tourCount!!.width,
            tourCount!!.height
        )
        bg = BitmapDrawable(addScreen.resources, bMap)
        addScreen.background = bg

        listToAdd = ArrayList()

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

        // setup the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        supportActionBar!!.setTitle(R.string.addTitle)

        // complete ArrayLists of species will get reduced to yet missing species
        // 1.: get code list of contained species from counts
        val specCodesContainedList = ArrayList<String?>()
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

    override fun onPause() {
        super.onPause()

        countDataSource!!.close()
    }

    // mark the selected species and consider it for the species counts list
    fun checkBoxAdd(view: View) {
        val idToAdd = view.tag as Int
        val saw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget
        
        val checked = saw.getMarkSpec() // return boolean isChecked

        // put species on add list
        if (checked) {
            listToAdd!!.add(saw)
            if (MyDebug.LOG) {
                val codeA = saw.getSpecCode()
                Log.d(TAG, "214, addCount, code: $codeA")
            }
        }
        else {
            // remove species previously added from add list
            listToAdd!!.remove(saw)
            if (MyDebug.LOG) {
                val codeA = saw.getSpecCode()
                Log.d(TAG, "222, removeCount, code: $codeA")
            }
        }
    }

    private fun saveData(): Boolean {
        var retValue = true

        // for all species in list to add
        var i = 0
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            if (MyDebug.LOG) {
                Log.d(TAG, "237, saveData, code: $specCode")
            }
            try {
                countDataSource!!.createCount(specName, specCode, specNameG)
            } catch (e: Exception) {
                retValue = false
            }
            i++
        }

        // store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            val editor = prefs.edit()
            editor.putString("new_spec_code", specCode)
            editor.commit()
        }
        return retValue
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == android.R.id.home) {
            if (saveData()) {
                countDataSource!!.close()

                val intent = NavUtils.getParentActivityIntent(this)!!
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                NavUtils.navigateUpTo(this, intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        if (saveData()) {
            countDataSource!!.close()

            NavUtils.navigateUpFromSameTask(this)
        }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "AddSpecAct"
    }

}
