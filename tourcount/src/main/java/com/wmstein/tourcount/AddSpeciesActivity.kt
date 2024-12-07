package com.wmstein.tourcount

import android.annotation.SuppressLint
import android.content.Intent
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
import com.wmstein.tourcount.database.Count
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.widgets.AddSpeciesWidget
import com.wmstein.tourcount.widgets.HintAddWidget

/**********************************************************************
 * AddSpeciesActivity lets you insert new species into the species list
 * AddSpeciesActivity is called from CountingActivity
 * Uses AddSpeciesWidget.kt, widget_add_spec.xml.
 *
 * The sorting order of the species to add cannot be changed, as it is
 * determined by 3 interdependent and correlated arrays in arrays.xml
 *
 * Created for TourCount by wmstein on 2019-04-12,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-05-26
 * last edited on 2024-11-25
 */
class AddSpeciesActivity : AppCompatActivity() {
    private var addArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.dLOG) Log.i(TAG, "73, onCreate")

        // Load preferences
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_add_species)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // get value from DummyActivity respective getInitialChars()
        val extras = intent.extras
        if (extras != null)
            initChars = extras.getString("init_Chars").toString()

        listToAdd = ArrayList()

        hintArea = findViewById(R.id.showHintAddLayout)
        addArea = findViewById(R.id.addSpecLayout)

        // setup the data sources
        countDataSource = CountDataSource(this)

        // new onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (MyDebug.dLOG) Log.i(TAG, "104, handleOnBackPressed")

                countDataSource!!.close()
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.dLOG) Log.i(TAG, "117, onResume")

        countDataSource!!.open()

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
        if (initChars.length == 2)
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
        searchAdd.findFocus()

        // Get the initial characters of species to select from
        initChars = searchAdd.text.toString().trim()
        if (initChars.length == 1) {
            // Reminder: "Please, 2 characters"
            searchAdd.error = getString(R.string.initCharsL)
        } else {
            searchAdd.error = null

            if (MyDebug.dLOG) Log.d(TAG, "159, initChars: $initChars")

            // Call DummyActivity to reenter AddSpeciesActivity for reduced add list
            countDataSource!!.close()
            val intent = Intent(this@AddSpeciesActivity, DummyActivity::class.java)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isAdd")
            startActivity(intent)
        }
    }

    // Construct add-species-list of not already contained species in the counting list
    //   and optionally reduce it further by initChar selection
    @SuppressLint("ApplySharedPref")
    private fun constructAddList() {
        // 1. Build list of codes of contained species in counting list
        val codesCountList = ArrayList<String?>()

        // Get sorted species of the counting list
        countsCodesSortList = countDataSource!!.allSpeciesSrtCode

        // build ArrayList of codes of already contained species
        for (count in countsCodesSortList) {
            codesCountList.add(count.code)
        }

        // 2. Build lists of all yet missing species
        val codesCountListSize = codesCountList.size
        if (MyDebug.dLOG) Log.d(TAG, "187, codesCountListSize: $codesCountListSize")

        // Reduce complete arraylists for already contained species
        for (i in 0 until codesCountListSize) {
            if (codesCompleteArrayList!!.contains(codesCountList[i])) {
                // Remove species with specCode[i] from missing species lists.
                // Prerequisites: Exactly correlated arrays of selCodes, selSpecs and selSpecs_g
                specCode = codesCountList[i]
                posSpec = codesCompleteArrayList!!.indexOf(specCode)
                if (MyDebug.dLOG) Log.d(TAG, "196, 1. specCode: $specCode, posSpec: $posSpec")
                namesCompleteArrayList!!.removeAt(posSpec)
                namesGCompleteArrayList!!.removeAt(posSpec)
                codesCompleteArrayList!!.removeAt(posSpec)
            }
        }

        if (MyDebug.dLOG) Log.d(TAG, "203, initChars: $initChars")
        if (MyDebug.dLOG) Log.d(TAG, "204, namesCompleteArrayListSize: "
                + namesCompleteArrayList!!.size
        )

        // Copy ...CompleteArrayLists to ...ReducedArrayLists
        namesReducedArrayList = namesCompleteArrayList
        namesGReducedArrayList = namesGCompleteArrayList
        codesReducedArrayList = codesCompleteArrayList

        // 3. Further, optionally reduce the complete Arraylists for all but initChar species
        if (initChars.length == 2) {
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
                    if (MyDebug.dLOG) Log.d(TAG, "226, 2. specName: $specName, specCode: $specCode")

                    // Assemble remaining ReducedArrayLists for all Species with initChars
                    namesReducedArrayList!!.add(specName!!)
                    namesGReducedArrayList!!.add(specNameG!!)
                    codesReducedArrayList!!.add(specCode!!)
                }
            }
        }

        // Create remainingIdArrayList with IDs of remaining species
        remainingIdArrayList = arrayOfNulls(codesReducedArrayList!!.size)
        if (MyDebug.dLOG) Log.d(TAG, "238, remainingIdArrayListSize: " + remainingIdArrayList.size)
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
        if (MyDebug.dLOG) Log.d(TAG, "267, View.tag: $idToAdd")
        val asw = addArea!!.getChildAt(idToAdd) as AddSpeciesWidget

        val checked = asw.getMarkSpec() // return boolean isChecked

        // put species on add list
        if (checked) {
            listToAdd!!.add(asw)
            if (MyDebug.dLOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "277, addCount, code: $codeA")
            }
        } else {
            // remove species previously added from add list
            listToAdd!!.remove(asw)
            if (MyDebug.dLOG) {
                val codeA = asw.getSpecCode()
                Log.d(TAG, "284, removeCount, code: $codeA")
            }
        }
    }

    // Update COUNT_TABLE with added species 
    @SuppressLint("ApplySharedPref")
    private fun addSpecs() {
        // Append the species from list to add to the COUNT_TABLE
        var i = 0
        while (i < listToAdd!!.size) {
            specName = listToAdd!![i].getSpecName()
            specCode = listToAdd!![i].getSpecCode()
            specNameG = listToAdd!![i].getSpecNameG()
            if (MyDebug.dLOG) {
                Log.d(TAG, "299, addSpecs, code: $specCode")
            }
            try {
                countDataSource!!.createCount(specName, specCode, specNameG)
            } catch (_: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort counts table for code
        countDataSource!!.sortCounts()

        // Store code of last selected species in sharedPreferences
        //  for Spinner in CountingActivity
        if (i > 0) {
            val editor = prefs.edit()
            editor.putString("new_spec_code", specCode)
            editor.commit()
        }

        // Call DummyActivity to reenter AddSpeciesActivity to rebuild the species list
        countDataSource!!.close()
        val intent = Intent(this@AddSpeciesActivity, DummyActivity::class.java)
        intent.putExtra("init_Chars", "")
        intent.putExtra("is_Flag", "isAdd")
        startActivity(intent)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.add_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.addSpecs) {
            if (listToAdd!!.size > 0)
                addSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (MyDebug.dLOG) Log.i(TAG, "362, onPause")

        countDataSource!!.close()
    }

    companion object {
        private const val TAG = "AddSpecAct"
    }

}
