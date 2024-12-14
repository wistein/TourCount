package com.wmstein.tourcount

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.WindowManager
import androidx.preference.PreferenceManager
import java.lang.Exception

/*********************************************************************
 * Handle background image and prefs
 * Partly derived from BeeCountApplication.java by milo on 14/05/2014.
 * Adopted for TourCount by wmstein on 2016-02-18,
 * converted to Kotlin on 2024-12-09,
 * last edit on 2024-12-09
 */
class TourCountApplication : Application() {
    var bMapDraw: BitmapDrawable? = null
    var width: Int = 0
    var height: Int = 0

    override fun onCreate() {
        super.onCreate()

        // Support to debug "A resource failed to call ..." (close, dispose or similar)
        if (MyDebug.dLOG) {
            Log.i(TAG, "42, StrictMode.setVmPolicy")
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectLeakedClosableObjects()
                    .build()
            )
        }

        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(this)
        } catch (e: Exception) {
            if (MyDebug.dLOG) Log.e(TAG, "46, Error: $e")
        }
    }
    // End of onCreate()

    // bMapDraw is a pre-prepared bitmap read by WelcomeActivity, SelectSectionActivity
    //   and CountingActivity
    @Suppress("DEPRECATION")
    fun setBackgr(): BitmapDrawable {
        bMapDraw = null

        val backgroundPref: String = prefs!!.getString("pref_backgr", "default")!!
        if (MyDebug.dLOG) Log.i(TAG, "58, Backgr.: $backgroundPref")

        val wm = checkNotNull(this.getSystemService(WINDOW_SERVICE) as WindowManager)
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = wm.currentWindowMetrics
            width = metrics.bounds.right + metrics.bounds.left
            height = metrics.bounds.top + metrics.bounds.bottom
        } else {
            val display = wm.defaultDisplay
            val size = Point()
            display.getSize(size)
            width = size.x
            height = size.y
        }
        if (MyDebug.dLOG) Log.d(TAG, "72, Width: $width Height: $height")

        var bMap: Bitmap?
        if (backgroundPref == "none") {
            // black screen
            bMap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bMap.eraseColor(Color.BLACK)
        } else if (backgroundPref == "grey") {
            bMap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bMap.eraseColor(-0xddddde)
        } else {
            if (height.toDouble() / width < 1.8) {
                // normal screen
                bMap = decodeBitmap(R.drawable.tourcount_picture_pn, width, height)
            } else {
                // long screen
                bMap = decodeBitmap(R.drawable.tourcount_picture_pl, width, height)
            }
        }

        bMapDraw = BitmapDrawable(this.resources, bMap)
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
        try {
            return BitmapFactory.decodeResource(resources, resId, options)
        } catch (_: OutOfMemoryError) {
            return null
        }
    }

    companion object {
        private const val TAG = "TourCountAppl"
        private var prefs: SharedPreferences? = null

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

        @JvmStatic
        fun getPrefs(): SharedPreferences {
            return prefs!!
        }
    }

}
