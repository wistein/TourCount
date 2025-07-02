package com.wmstein.filechooser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.wmstein.tourcount.MyDebug
import com.wmstein.tourcount.R
import java.io.File
import java.io.FileFilter
import java.text.DateFormat
import java.text.SimpleDateFormat

/********************************************************************************
 * AdvFileChooser lets you select files from user's Documents directory.
 * It will be called within WelcomeActivity and uses FileArrayAdapter and Option.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18,
 * last change in Java on 2022-05-21,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2025-05-30
 */
class AdvFileChooser : AppCompatActivity() {
    private var currentDir: File? = null
    private var adapter: FileArrayAdapter? = null
    private var fileExtension: String = ""
    private var fileNameStart: String? = null
    private var fileFilter: FileFilter? = null
    private var fileHd: String? = null

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "51, onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            enableEdgeToEdge()
        }

        setContentView(R.layout.list_view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.baseFilesLayout))
        { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view. You can also update the view padding
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) // SDK 35+
        {
            setStatusBarColor(window, ContextCompat.getColor(applicationContext,
                R.color.DarkerGray))
        }

        val extras = intent.extras
        if (extras != null) {
            if (extras.getString("filterFileExtension") != null) {
                fileExtension = extras.getString("filterFileExtension")!!
                fileNameStart = extras.getString("filterFileNameStart")
                fileHd = extras.getString("fileHd")

                fileFilter = FileFilter { pathname: File ->
                    pathname.name.contains(".") &&
                            pathname.name.contains(fileNameStart!!) &&
                            fileExtension.contains(
                                pathname.name.substring(
                                    pathname.name.lastIndexOf(".")
                                )
                            )
                }
            }
        }

        // Set FileChooser Headline
        val fileHead: TextView = findViewById(R.id.fileHead)
        fileHead.text = fileHd

        // currentDir = /storage/emulated/0/Documents/TourCount/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) // Android 10+
        {
            currentDir = Environment.getExternalStorageDirectory()
            currentDir = File("$currentDir/Documents/TourCount")
        } else {
            currentDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )
            currentDir = File("$currentDir/TourCount")
        }
        fill(currentDir!!)
    }
    // End of onCreate()

    @RequiresApi(Build.VERSION_CODES.R)
    fun setStatusBarColor(window: Window, color: Int) {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            view.setBackgroundColor(color)
            insets
        }
    }

    // List only files in user's home directory
    private fun fill(f: File) {
        val dirs: Array<File>? = f.listFiles(fileFilter)
        this.title = getString(R.string.importHead)
        val fls: MutableList<Option> = ArrayList() // list of suitable files to choose from

        @SuppressLint("SimpleDateFormat")
        val dform: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        try {
            if (dirs != null) {
                for (ff in dirs) {
                    if (!ff.isHidden) {
                        fls.add(
                            Option(
                                ff.name,
                                getString(R.string.fileSize) + ": " + ff.length() + " B,  " + getString(
                                    R.string.date
                                ) + ": "
                                        + dform.format(ff.lastModified()),
                                ff.absolutePath,
                                false
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // do nothing
        }

        if (fls.isNotEmpty()) {
            fls.sort()
            val listView = findViewById<ListView>(R.id.lvFiles)
            adapter = FileArrayAdapter(listView.context, R.layout.file_view, fls)
            listView.adapter = adapter
            listView.onItemClickListener =
                OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                    val opt = adapter!!.getItem(position)
                    if (!opt.isBack) doSelect(opt) else {
                        currentDir = File(opt.path)
                        fill(currentDir!!)
                    }
                }
        } else {
            val intent = Intent()
            intent.putExtra("fileSelected", "")
            setResult(RESULT_FIRST_USER, intent)
            finish()
        }
    }

    // onFileClick(opt);
    private fun doSelect(opt: Option?) {
        val fileSelected = File(opt!!.path)
        val intent = Intent()
        intent.putExtra("fileSelected", fileSelected.absolutePath)
        setResult(RESULT_OK, intent)
        if (MyDebug.DLOG) Log.i(TAG, "186, Selected file: $fileSelected")
        finish()
    }

    public override fun onStop() {
        super.onStop()
    }

    companion object {
        private const val TAG = "AdvFileChooser"
    }

}
