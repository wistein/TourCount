package com.wmstein.tourcount

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
import androidx.core.app.NavUtils
import com.wmstein.tourcount.database.CountDataSource
import com.wmstein.tourcount.database.IndividualsDataSource
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.widgets.DeleteSpeciesWidget
import com.wmstein.tourcount.widgets.HintDelWidget

/********************************************************************
 * DelSpeciesActivity lets you delete species from the species lists.
 * It is called from CountingActivity.
 * Uses DelSpeciesWidget.kt, EditTitleWidget.kt,
 * activity_del_species.xml, widget_edit_title.xml.
 * Based on EditSpeciesListActivity.kt.
 * Created on 2024-08-22 by wmstein,
 * last edited on 2024-11-25
 */
class DelSpeciesActivity : AppCompatActivity() {
    // Data
    var section: Section? = null
    private var specCode: String? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null

    // Layouts
    private var deleteArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // Arraylists
    private var listToDelete: ArrayList<DeleteSpeciesWidget>? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.dLOG) Log.i(TAG, "56, onCreate")

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)

        setContentView(R.layout.activity_del_species)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        // Get value from DummyActivity respective getInitialChars()
        val extras = intent.extras
        if (extras != null)
            initChars = extras.getString("init_Chars").toString()

        listToDelete = ArrayList()

        hintArea = findViewById(R.id.showHintDelLayout)
        deleteArea = findViewById(R.id.deleteSpecLayout)

        // Setup the data sources
        sectionDataSource = SectionDataSource(this)
        countDataSource = CountDataSource(this)
        individualsDataSource = IndividualsDataSource(this)

        // New onBackPressed logic
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavUtils.navigateUpFromSameTask(this@DelSpeciesActivity)
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        if (MyDebug.dLOG) Log.d(TAG, "99 onResume")

        sectionDataSource!!.open()
        countDataSource!!.open()
        individualsDataSource!!.open()

        // Clear any existing views
        deleteArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.delTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Species in counting list
        val hdw = HintDelWidget(this, null)
        if (initChars.length == 2)
            hdw.setSearchD(initChars)
        else
            hdw.setSearchD(getString(R.string.hintSearch))
        hintArea!!.addView(hdw)

        constructDelList()
    }
    // End of onResume()

    // Get initial 2 characters of species to select by search button
    fun getInitialChars(view: View) {
        // Read EditText searchDel from widget_del_hint.xml
        val searchDel: EditText = findViewById(R.id.searchD)

        // Get the initial characters of species to select from
        initChars = searchDel.text.toString().trim()
        if (initChars.length == 1) {
            // Reminder: "Please, 2 characters"
            searchDel.error = getString(R.string.initCharsL)
        } else {
            searchDel.error = null

            if (MyDebug.dLOG) Log.d(TAG, "137, initChars: $initChars")

            // Call DummyActivity to reenter DelSpeciesActivity for reduced add list
            val intent = Intent(this@DelSpeciesActivity, DummyActivity::class.java)
            intent.putExtra("init_Chars", initChars)
            intent.putExtra("is_Flag", "isDel")
            startActivity(intent)
        }
    }

    // Construct del-species-list of contained species in the counting list
    //   and optionally reduce it by initChar selection
    private fun constructDelList() {
        // Load the sorted species data from section 1
        val counts = countDataSource!!.allSpeciesSrtCode

        // Get all counting list species into their CountEditWidgets and add these to the view
        if (initChars.length == 2) {
            // Check name in counts for InitChars to reduce list
            var cnt = 1
            for (count in counts) {
                if (count.name?.substring(0, 2) == initChars) {
                    val dsw = DeleteSpeciesWidget(this, null)
                    dsw.setSpecName(count.name)
                    dsw.setSpecNameG(count.name_g)
                    dsw.setSpecCode(count.code)
                    dsw.setPSpec(count)
                    dsw.setSpecId(cnt.toString()) // Index in reduced list
                    cnt++
                    deleteArea!!.addView(dsw)
                    if (MyDebug.dLOG) Log.d(TAG, "167, name: " + count.name)
                }
            }
        } else {
            for (count in counts) {
                val dsw = DeleteSpeciesWidget(this, null)
                dsw.setSpecName(count.name)
                dsw.setSpecNameG(count.name_g)
                dsw.setSpecCode(count.code)
                dsw.setPSpec(count)
                dsw.setSpecId(count.id.toString()) // Index in complete list
                deleteArea!!.addView(dsw)
                if (MyDebug.dLOG) Log.d(TAG, "179, name: " + count.name)
            }
        }
    }

    // Mark the selected species and consider it for delete from the species counts list
    fun checkBoxDel(view: View) {
        val idToDel = view.tag as Int
        if (MyDebug.dLOG) Log.d(TAG, "187, View.tag: $idToDel")
        val dsw = deleteArea!!.getChildAt(idToDel) as DeleteSpeciesWidget

        val checked = dsw.getMarkSpec() // return boolean isChecked

        // Put species on add list
        if (checked) {
            listToDelete!!.add(dsw)
            if (MyDebug.dLOG) {
                val codeD = dsw.getSpecCode()
                Log.d(TAG, "197, mark delete code: $codeD")
            }
        } else {
            // Remove species previously added from add list
            listToDelete!!.remove(dsw)
            if (MyDebug.dLOG) {
                val codeD = dsw.getSpecCode()
                Log.d(TAG, "204 mark delete code: $codeD")
            }
        }
    }

    // Delete selected species from species list
    private fun delSpecs() {
        var i = 0 // index of species list to delete
        while (i < listToDelete!!.size) {
            specCode = listToDelete!![i].getSpecCode()
            if (MyDebug.dLOG) Log.d(TAG, "214, delete code: $specCode")
            try {
                countDataSource!!.deleteCountByCode(specCode!!)
                individualsDataSource!!.deleteIndividualsByCode(specCode!!)
            } catch (_: Exception) {
                // nothing
            }
            i++
        }

        // Re-index and sort counts table for code
        countDataSource!!.sortCounts()

        // Rebuild the species list
        val counts = countDataSource!!.allSpeciesSrtCode

        hintArea!!.removeAllViews()
        val hdw = HintDelWidget(this, null)
        hdw.setSearchD(getString(R.string.hintSearch))
        hintArea!!.addView(hdw)

        deleteArea!!.removeAllViews()
        for (count in counts) {
            val dsw = DeleteSpeciesWidget(this, null)
            dsw.setSpecName(count.name)
            dsw.setSpecNameG(count.name_g)
            dsw.setSpecCode(count.code)
            dsw.setPSpec(count)
            dsw.setSpecId(count.id.toString())
            deleteArea!!.addView(dsw)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.delete_species, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        } else if (id == R.id.deleteSpec) {
            if (listToDelete!!.size > 0)
                delSpecs()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        if (MyDebug.dLOG) Log.i(TAG, "270, onPause")

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        individualsDataSource!!.close()
    }

    companion object {
        private const val TAG = "DelSpeciesAct"
    }

}
