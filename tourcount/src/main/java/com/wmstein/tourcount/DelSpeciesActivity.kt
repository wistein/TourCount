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
 * last edited on 2024-10-13
 */
class DelSpeciesActivity : AppCompatActivity() {
    private var tourCount: TourCountApplication? = null

    // Layouts
    private var deleteArea: LinearLayout? = null
    private var hintArea: LinearLayout? = null

    // Screen background
    private var bMap: Bitmap? = null
    private var bg: BitmapDrawable? = null

    // Data
    var section: Section? = null
    private var specCode: String? = null
    private var sectionDataSource: SectionDataSource? = null
    private var countDataSource: CountDataSource? = null
    private var individualsDataSource: IndividualsDataSource? = null

    // 2 initial characters to limit selection
    private var initChars: String = ""

    // Arraylists
    private var listToDelete: ArrayList<DeleteSpeciesWidget>? = null

    // Preferences
    private var prefs = TourCountApplication.getPrefs()
    private var brightPref = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tourCount = application as TourCountApplication

        // Load preference
        brightPref = prefs.getBoolean("pref_bright", true)

        if (MyDebug.LOG) Log.d(TAG, "75 onCreate")

        setContentView(R.layout.activity_del_species)
        val deleteScreen = findViewById<LinearLayout>(R.id.delSpec)

        // Set full brightness of screen
        if (brightPref) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val params = window.attributes
            params.screenBrightness = 1.0f
            window.attributes = params
        }

        bMap = tourCount!!.decodeBitmap(
            R.drawable.delbackground,
            tourCount!!.width,
            tourCount!!.height
        )
        bg = BitmapDrawable(deleteScreen.resources, bMap)
        deleteScreen.background = bg

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
        if (Build.VERSION.SDK_INT >= 33) {
            onBackPressedDispatcher.addCallback(object :
                OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    countDataSource!!.close()
                    sectionDataSource!!.close()
                    individualsDataSource!!.close()

                    NavUtils.navigateUpFromSameTask(this@DelSpeciesActivity)
                }
            })
        }
    }
    // End of onCreate()

    override fun onResume() {
        super.onResume()

        // Build the Delete Species screen
        countDataSource!!.open()
        sectionDataSource!!.open()
        individualsDataSource!!.open()

        // Clear any existing views
        deleteArea!!.removeAllViews()
        hintArea!!.removeAllViews()

        supportActionBar!!.setTitle(R.string.delTitle)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // Display hint: Species in counting list
        val hdw = HintDelWidget(this, null)
        if (initChars != "")
            hdw.setSearchD(initChars)
        else
            hdw.setSearchD(getString(R.string.hintSearch))
        hintArea!!.addView(hdw)

        constructDelList()
    }
    // End of onResume

    // Get initial 2 characters of species to select by search button
    fun getInitialChars(view: View) {
        // Read EditText searchDel from widget_del_hint.xml
        val searchDel: EditText = findViewById(R.id.searchD)

        // Get the initial characters of species to select from
        initChars = searchDel.text.toString().trim()
        if (initChars.length < 2) {
            // Reminder: "Please, 2 characters"
            searchDel.error = getString(R.string.initCharsL)
        } else {
            searchDel.error = null

            if (MyDebug.LOG) Log.d(TAG, "167, initChars: $initChars")

            // Call DummyActivity to reenter AddSpeciesActivity for reduced add list
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
        if (initChars.isEmpty()) {
            for (count in counts) {
                val dsw = DeleteSpeciesWidget(this, null)
                dsw.setSpecName(count.name)
                dsw.setSpecNameG(count.name_g)
                dsw.setSpecCode(count.code)
                dsw.setPSpec(count)
                dsw.setSpecId(count.id.toString()) // Index in complete list
                deleteArea!!.addView(dsw)
                if (MyDebug.LOG) Log.d(TAG, "194, name: " + count.name)
            }
        }
        else {
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
                    if (MyDebug.LOG) Log.d(TAG, "210, name: " + count.name)
                }
            }
        }

        val editor = prefs.edit()
        editor.putString("is_Del", "")
        editor.commit()
    }

    // Mark the selected species and consider it for delete from the species counts list
    fun checkBoxDel(view: View) {
        val idToDel = view.tag as Int
        if (MyDebug.LOG) Log.d(TAG, "223, View.tag: $idToDel")
        val dsw = deleteArea!!.getChildAt(idToDel) as DeleteSpeciesWidget

        val checked = dsw.getMarkSpec() // return boolean isChecked

        // Put species on add list
        if (checked) {
            listToDelete!!.add(dsw)
            if (MyDebug.LOG) {
                val codeD = dsw.getSpecCode()
                Log.d(TAG, "233, mark delete code: $codeD")
            }
        } else {
            // Remove species previously added from add list
            listToDelete!!.remove(dsw)
            if (MyDebug.LOG) {
                val codeD = dsw.getSpecCode()
                Log.d(TAG, "240, mark delete code: $codeD")
            }
        }
    }

    // Delete selected species from species lists of all sections
    private fun delSpecs() {
        var i = 0 // index of species list to delete
        while (i < listToDelete!!.size) {
            specCode = listToDelete!![i].getSpecCode()
            if (MyDebug.LOG) Log.d(TAG, "250, delete code: $specCode")
            try {
                countDataSource!!.deleteCountByCode(specCode!!)
                individualsDataSource!!.deleteIndividualsByCode(specCode!!)
            } catch (e: Exception) {
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

    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        val id = item.itemId
        if (id == android.R.id.home) {
            sectionDataSource!!.close()
            countDataSource!!.close()
            individualsDataSource!!.close()

            val intent = NavUtils.getParentActivityIntent(this)!!
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NavUtils.navigateUpTo(this, intent)
        } else if (id == R.id.deleteSpec) {
            if (listToDelete!!.size > 0)
                delSpecs()
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()

        // Close the data sources
        sectionDataSource!!.close()
        countDataSource!!.close()
        individualsDataSource!!.close()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("ApplySharedPref", "MissingSuperCall")
    override fun onBackPressed() {
        countDataSource!!.close()
        sectionDataSource!!.close()
        individualsDataSource!!.close()

        NavUtils.navigateUpFromSameTask(this)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "DelSpecAct"
    }

}
