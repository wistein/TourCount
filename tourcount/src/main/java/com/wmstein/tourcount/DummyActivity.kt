/*
 * Copyright © 2016-2024, Wilhelm Stein, Bonn, Germany.
 */
package com.wmstein.tourcount

import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/*******************************************************************************************
 * Dummy activity
 * to re-enter AddSpeciesActivity, DelSpeciesActivity or EditSpecListActivity
 *    to rebuild their views after restricted species selection
 *
 * Created by wmstein on 2024-10-07,
 * last edited on 2024-11-12
 */
class DummyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "25, Dummy")

        var initChars = ""
        var isFlag = ""

        // get value from calling Activity respective getInitialChars()
        val extras = intent.extras
        if (extras != null) {
            initChars = extras.getString("init_Chars").toString()
            isFlag = extras.getString("is_Flag").toString()
        }

        // Set smooth transition to the called activity
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < 34) // till Android 13
            overridePendingTransition(0, 0)
        else
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0, TRANSPARENT)

        when (isFlag) {
            "isAdd" -> intent = Intent(this@DummyActivity, AddSpeciesActivity::class.java)
            "isDel" -> intent = Intent(this@DummyActivity, DelSpeciesActivity::class.java)
            "isEdit" -> intent = Intent(this@DummyActivity, EditSpecListActivity::class.java)
            else -> exit()
        }

        intent.putExtra("init_Chars", initChars)
        intent.putExtra("is_Flag", "")
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))

        exit()
    }

    private fun exit() {
        super.finish()
    }

    companion object {
        private const val TAG = "DummyAct"
    }

}
