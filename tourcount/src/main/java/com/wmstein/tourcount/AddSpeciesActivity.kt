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
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.AddSpeciesWidget
import com.wmstein.tourcount.widgets.HintAddWidget

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
 * last edited on 2024-10-13
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    private var addArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    // Screen background
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Count data
    private var countDataSource: CountDataSource? = null

    // ID-list of not yet included species
    private lateinit var remainingIdArrayList: Array<String?>

    // 3 ArrayLists (for names, namesG and codes) of all species from arrays.xml
    // will get reduced to lists of not yet included Species
    private var namesCompleteArrayList: ArrayList<String>? = null
    private var namesReducedArrayList: ArrayList<String>? = null
    private var namesGCompleteArrayList: ArrayList<String>? = null
    private var namesGReducedArrayList: ArrayList<String>? = null
    private var codesCompleteArrayList: ArrayList<String?>? = null
    private var codesReducedArrayList: ArrayList<String?>? = null

    private var specName: String? = null
    private var specNameG: String? = null
    private var specCode: String? = null
    private var posSpec: Int = 0

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // List of selected species to add
    private var listToAdd: ArrayList<AddSpeciesWidget>? = null

    // List of all count list species sorted by codes
    private lateinit var countsCodesSortList: List<Count>

    // preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)

        if (MyDebug.LOG) Log.d(TAG, "90, onCreate")

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

        // get value from DummyActivity respective getInitialChars()
        val extras = intent.extras
        if (extras != null)
            initChars = extras.getString("init_Chars").toString()

        listToAdd = ArrayList()

        hintArea = findViewById(R.id.showHintAddLayout)
        addArea = findViewById(R.id.addSpecLayout)

        // setup the data sources
        countDataSource = CountDataSource(this)
        countDataSource!!.open()

        // new onBackPressed logic
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    countDataSource!!.close()

                    NavUtils.navigateUpFromSameTask(this@AddSpeciesActivity)
                }
            })
        }
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        // Load complete species ArrayList from arrays.xml (lists are sorted by code)
        namesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs)))
        namesGCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selSpecs_g)))
        codesCompleteArrayList = ArrayList(listOf(*resources.getStringArray(R.array.selCodes)))

        // clear any existing views
        addArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.addTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Further available species
        val haw = HintAddWidget(this, null)
        if (initChars != "")
            haw.setSearchA(initChars)
        else
            haw.setSearchA(getString(R.string.hintSearch))
        hintArea!!.addView(haw)

        constructAddList()
    }
    // End of onResume()

    // Get initial 2 characters of species to select by search button
    fun getInitialChars(view: View) {
        // Read EditText searchAdd from widget_add_hint.xml
        val searchAdd: EditText = findViewById(R.id.searchA)

        // Get the initial characters of species to select from
        initChars = searchAdd.text.toString().trim()
        if (initChars.length < 2) {
            // Reminder: "Please, 2 characters"
            searchAdd.error = getString(R.string.initCharsL)
        } else {
            searchAdd.error = null

            if (MyDebug.LOG) Log.d(TAG, "179, initChars: $initChars")

            // Call DummyActivity to reenter AddSpeciesActivity for reduced add list
            val intent = Intent(this@AddSpeciesActivity, DummyActivity::class.java)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isAdd")
            startActivity(intent)
        }
    }

    // Construct add-species-list of not already contained species in the counting list
    //   and optionally reduce it further by initChar selection
    private fun constructAddList() {
        // 1. Build list of codes of contained species in counting list
        val codesCountList = ArrayList<String?>()
        countsCodesSortList = countDataSource!!.allSpeciesSrtCode

        for (count in countsCodesSortList) {
            codesCountList.add(count.code)
        }

        // 2. Build lists of all yet missing species
        val codesCountListSize = codesCountList.size
        if (MyDebug.LOG) Log.d(TAG, "202, codesCountListSize: $codesCountListSize")

        // Reduce complete arraylists for already contained species
        for (i in 0 until codesCountListSize) {
            if (codesCompleteArrayList!!.contains(codesCountList[i])) {
                // Remove species with specCode[i] from missing species lists.
                // Prerequisites: Exactly correlated arrays of selCodes, selSpecs and selSpecs_g
                specCode = codesCountList[i]
                posSpec = codesCompleteArrayList!!.indexOf(specCode)
                if (MyDebug.LOG) Log.d(TAG, "211, 1. specCode: $specCode, posSpec: $posSpec")
                namesCompleteArrayList!!.removeAt(posSpec)
                namesGCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.removeAt(posSpec)
            }
        }

        if (MyDebug.LOG) Log.d(TAG, "218, initChars: $initChars")
        if (MyDebug.LOG) Log.d(TAG, "219, namesCompleteArrayListSize: " + namesCompleteArrayList!!.size)

        // Copy ...CompleteArrayLists to ...ReducedArrayLists
        namesReducedArrayList = namesCompleteArrayList
        namesGReducedArrayList = namesGCompleteArrayList
        codesReducedArrayList = codesCompleteArrayList

        // 3. Further, optionally reduce the complete Arraylists for all but initChar species
        if (initChars.isNotEmpty()) {
            // Empty ...ReducedArrayLists
            namesReducedArrayList = arrayListOf()
            namesGReducedArrayList = arrayListOf()
            codesReducedArrayList = arrayListOf()

            // Check NamesCompleteArrayList for InitChars
            for (i in 0 until namesCompleteArrayList!!.size) {
                if (namesCompleteArrayList!![i].substring(0, 2) == initChars) {
                    specName = namesCompleteArrayList!![i]
                    specNameG = namesGCompleteArrayList!![i]
                    specCode = codesCompleteArrayList!![i]
                    if (MyDebug.LOG) Log.d(TAG, "239, 2. specName: $specName, specCode: $specCode")

                    // Assemble remaining ReducedArrayLists for all Species with initChars
                    namesReducedArrayList!!.add(specName!!)
                    namesGReducedArrayList!!.add(specNameG!!)
                    codesReducedArrayList!!.add(specCode!!)
                }
            }
        }

        // Create remainingIdArrayList with IDs of remaining species
        remainingIdArrayList = arrayOfNulls(codesReducedArrayList!!.size)
        if (MyDebug.LOG) Log.d(TAG, "251, remainingIdArrayListSize: " + remainingIdArrayList.size)
        var i = 0
        while (i < codesReducedArrayList!!.size) {
            remainingIdArrayList[i] = (i + 1).toString()
            i++
        }

        // load the data of remaining species into the widgets
        i = 0
        while (i < codesReducedArrayList!!.size) {
            val asw = AddSpeciesWidget(this, null)
            asw.setSpecName(namesReducedArrayList!![i])
            asw.setSpecNameG(namesGReducedArrayList!![i])
            asw.setSpecCode(codesReducedArrayList!![i])
            asw.setPSpec(codesReducedArrayList!![i]!!)
            asw.setSpecId(remainingIdArrayList[i]!!)
            asw.setMarkSpec(false)
            addArea!!.addView(asw)
            i++
        }

        val editor = prefs.edit()
        editor.putString("is_Add", "")
        editor.commit()
    }

    // mark the selected species and consider it for the species counts list
    fun checkBoxAdd(view: View) {
        val idToAdd = view.tag as Int
        if (MyDebug.LOG) Log.d(TAG, "280, View.tag: $idToAdd")
        val asw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget

        val checked = asw.getMarkSpec() // return boolean isChecked

        // put species on add list
        if (checked) {
            listToAdd!!.add(asw)
            if (MyDebug.LOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "290, addCount, code: $codeA")
            }
        } else {
            // remove species previously added from add list
            listToAdd!!.remove(asw)
            if (MyDebug.LOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "297, removeCount, code: $codeA")
            }
        }
    }

    // Update COUNT_TABLE with added species 
    private fun addSpecs() {
        // Append the species from list to add to the COUNT_TABLE
        var i = 0
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            if (MyDebug.LOG) {
                Log.d(TAG, "311, addSpecs, code: $specCode")
            }
            try {
                countDataSource!!.createCount(specName, specCode, specNameG)
            } catch (e: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort counts table for code
        countDataSource!!.sortCounts()

        // Call DummyActivity to reenter AddSpeciesActivity to rebuild the species list
        val intent = Intent(this@AddSpeciesActivity, DummyActivity::class.java)
        intent.putExtra("init_Chars", "")
        intent.putExtra("is_Flag", "isAdd")
        startActivity(intent)

        // store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            val editor = prefs.edit()
            editor.putString("new_spec_code", specCode)
            editor.commit()
        }
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
            countDataSource!!.close()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.addSpecs) {
            addSpecs()
        }
        return super.onOptionsItemSelected(item)
    }

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

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        countDataSource!!.close()

        NavUtils.navigateUpFromSameTask(this)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "AddSpecAct"
    }

}
