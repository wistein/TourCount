package com.wmstein.tourcount

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Provides global app data and handles background image and prefs
 *
 * Partly based on BeeCountApplication.java of BeeCount by Milo Thurston (knirirr) on 2014-05-14,
 * licensed under the Apache License, Version 2.0.
 *
 * Adopted for TourCount by wmstein on 2016-02-18,
 * converted to Kotlin on 2024-12-09,
 * last edited on 2026-10-19.
 **************************************************************************************************/

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.WindowManager

import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.preference.PreferenceManager

class TourCountApplication : Application() {
    var bMapDraw: BitmapDrawable? = null
    var width = 0
    var height = 0

    override fun onCreate() {
        super.onCreate()

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i(TAG, "54, StrictMode.setVmPolicy")
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "66, Error: $e")
        }

        // Initiate ActivityLifecycle for stopping periodic location requests
        registerActivityLifecycleCallbacks(TCLifecycleHandler())
    }
    // End of onCreate()

    // bMapDraw is a pre-prepared bitmap read by WelcomeActivity, SelectSectionActivity
    //   and CountingActivity
    fun setBackgr(): BitmapDrawable {
        bMapDraw = null

        val backgroundPref: String = prefs!!.getString("pref_backgr", "default")!!
        val wm = checkNotNull(this.getSystemService(WINDOW_SERVICE) as WindowManager)

        val metrics = wm.currentWindowMetrics
        width = metrics.bounds.right + metrics.bounds.left
        height = metrics.bounds.top + metrics.bounds.bottom
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "86, Width: $width Height: $height")

        screenRatioBig = height / width > 2

        var bMap: Bitmap?
        when (backgroundPref) {
            "none" -> {
                // black screen
                bMap = createBitmap(width, height, Bitmap.Config.RGB_565)
                bMap.eraseColor(Color.BLACK)
            }
            "grey" -> {
                bMap = createBitmap(width, height, Bitmap.Config.RGB_565)
                bMap.eraseColor(-0xddddde)
            }
            else -> {
                bMap = if (height.toDouble() / width < 1.8) {
                    // normal screen
                    decodeBitmap(R.drawable.tourcount_picture_pn, width, height)
                } else {
                    // long screen
                    decodeBitmap(R.drawable.tourcount_picture_pl, width, height)
                }
            }
        }

        bMapDraw = bMap?.toDrawable(this.resources)
        return bMapDraw!!
    }

    fun decodeBitmap(resId: Int, reqWidth: Int, reqHeight: Int): Bitmap? {
        // First decode with inJustDecodeBounds = true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeResource(resources, resId, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return try {
            BitmapFactory.decodeResource(resources, resId, options)
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    // Scale background bitmap
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height1 = options.outHeight
        val width1 = options.outWidth
        var inSampleSize = 1

        if (height1 > reqHeight || width1 > reqWidth) {
            val halfHeight = height1 / 2
            val halfWidth = width1 / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            //   height1 and width1 larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private const val TAG = "TourCountAppl"
        private var prefs: SharedPreferences? = null

        @JvmStatic
        fun getPrefs(): SharedPreferences {
            return prefs!!
        }

        @JvmField
        var screenRatioBig = false // WelcomeActivity, CountingActivity

        @JvmField
        var lat = 0.0 // set by getLatitude() in LocationService

        @JvmField
        var lon = 0.0 // set by getLongitude() in LocationService

        @JvmField
        var heightNN = 0.0 // set by getAltitude() in LocationService

        @JvmField
        var uncertainty = 0.0 // set by getAccuracy() in LocationService

        @JvmField
        var tLocality = "" // set by getAddressL() in LocationService and getAddressC() in CountingActivity

        @JvmField
        var addrServiceOn = false // controlled in WelcomeActivity and SettingsActivity

        @JvmField
        var isFirstLocality = true // true for showing an info message for the 1. GPS lock

        @JvmField
        var loopAddr = false // controls Looper for address messaging

        @JvmField
        var isFirstStart = true // true for showing a 'missing email' hint message once

        // Reduces Activity count in TCLifecycleHandler
        @JvmField
        var isStorPermReq = false // true for showing a 'missing email' hint message once
    }

}
