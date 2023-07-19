package com.wmstein.filechooser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import com.wmstein.tourcount.R
import java.io.File
import java.io.FileFilter
import java.text.DateFormat
import java.text.SimpleDateFormat

/**
 * AdvFileChooser lets you select files from sdcard directory.
 * It will be called within WelcomeActivity and uses FileArrayAdapter and Option.
 * Based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
 * Adopted by wmstein on 2016-06-18,
 * last change in Java on 2022-05-21
 * converted to Kotlin on 2023-07-09
 */
class AdvFileChooser : Activity() {
    private var currentDir: File? = null
    private var adapter: FileArrayAdapter? = null
    private var fileFilter: FileFilter? = null
    private var extensions: ArrayList<String>? = null
    private var filterFileName: String? = null
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_view)
        val extras = intent.extras
        if (extras != null) {
            if (extras.getStringArrayList("filterFileExtension") != null) {
                extensions = extras.getStringArrayList("filterFileExtension")
                filterFileName = extras.getString("filterFileName")
                fileFilter = FileFilter { pathname: File ->
                    pathname.name.contains(".") &&
                            pathname.name.contains(filterFileName!!) &&
                            extensions!!.contains(
                                pathname.name.substring(
                                    pathname.name.lastIndexOf(
                                        "."
                                    )
                                )
                            )
                }
            }
        }

        //currentDir = /storage/emulated/0/Android/data/com.wmstein.tourcount/files
        currentDir =
            File(applicationContext.getExternalFilesDir(null)!!.absolutePath)
        fill(currentDir!!)
    }

    // Back-key return from filechooser
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    // List only files in /sdcard
    private fun fill(f: File) {
        val dirs: Array<File>? = if (fileFilter != null) f.listFiles(fileFilter) else f.listFiles()
        this.title = getString(R.string.currentDir) + ": " + f.name
        val fls: MutableList<Option> = ArrayList()
        @SuppressLint("SimpleDateFormat") val dform: DateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm")
        try {
            assert(dirs != null)
            if (dirs != null) {
                for (ff in dirs) {
                    if (!ff.isHidden) {
                        fls.add(
                            Option(
                                ff.name, getString(R.string.fileSize) + ": "
                                        + ff.length() + " B,  " + getString(R.string.date) + ": "
                                        + dform.format(ff.lastModified()), ff.absolutePath, false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // do nothing
        }
        fls.sort()
        val listView = findViewById<ListView>(R.id.lvFiles)
        adapter = FileArrayAdapter(listView.context, R.layout.file_view, fls)
        listView.adapter = adapter
        listView.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                val o = adapter!!.getItem(position)
                if (!o.isBack) doSelect(o) else {
                        currentDir = File(o.path)
                        fill(currentDir!!)
                    }
            }
    }

    private fun doSelect(o: Option?) {
        //onFileClick(o);
        val fileSelected = File(o!!.path)
        val intent = Intent()
        intent.putExtra("fileSelected", fileSelected.absolutePath)
        setResult(RESULT_OK, intent)
        finish()
    }

    public override fun onStop() {
        super.onStop()
    }
}