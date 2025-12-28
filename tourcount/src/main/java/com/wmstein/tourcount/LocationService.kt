package com.wmstein.tourcount

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.wmstein.tourcount.TourCountApplication.Companion.isFirstLoc

/**************************************************************************************
 * LocationService provides the location data: latitude, longitude, height, uncertainty
 * on request. Its data may be read by most TourCount activities.
 *
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * licensed under the MIT License.
 *
 * In companion object
 * uses minimal distance for updates: 10 m (default),
 *      minimal time between updates: 10 sec (default).
 *
 * Adopted for TourCount by wmstein since 2018-07-26,
 * last edited in Java on 2023-05-30,
 * converted to Kotlin on 2023-05-26,
 * last edited on 2025-12-27
 */
open class LocationService : Service, LocationListener {
    private var mContext: Context? = null
    private var checkGPS = false
    private var checkNetwork = false
    private var canGetLocation = false
    private var location: Location? = null
    private var lat = 0.0
    private var lon = 0.0
    private var height = 0.0
    private var uncertainty = 0.0
    private var locationManager: LocationManager? = null
    private var exactLocation = false
    private var locationAttributionContext: Context? = null
    private var audioAttributionContext: Context? = null
    private var rToneP: MediaPlayer? = null

    // prefs
    private var prefs: SharedPreferences? = null
    private var alertSoundPref = false
    private var alertSound: String = ""
    private var selTimeInterval: Long = 3000 // Default time interval for updates

    // Default constructor is demanded for service declaration in AndroidManifest.xml
    constructor() {}

    constructor(mContext: Context?) {
        this.mContext = mContext
        getLocation()
    }

    private fun getLocation() {
        audioAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("ringSound")
            else this
        locationAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("locationCheck")
            else this

        prefs = TourCountApplication.getPrefs()
        selTimeInterval = prefs!!.getString("pref_time_interval", "5000")!!.toLong()
        alertSoundPref = prefs!!.getBoolean("pref_alert_sound", false)
        alertSound = prefs!!.getString("alert_sound", null).toString()

        if (alertSoundPref) {
            val uriB = if (alertSound.isNotBlank())
                alertSound.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            rToneP = MediaPlayer.create(audioAttributionContext, uriB)
        }

        try {
            locationManager = mContext!!.getSystemService(LOCATION_SERVICE) as LocationManager
            assert(locationManager != null)
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // get network provider status
            checkNetwork = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (checkGPS || checkNetwork) {
                canGetLocation = true
            } else {
                val mesg = getString(R.string.no_provider)
                Toast.makeText(
                    mContext,
                    HtmlCompat.fromHtml(
                        "<font color='red'><b>$mesg</b></font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ), Toast.LENGTH_LONG
                ).show()
            }

            // if GPS is enabled get position using GPS Service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        selTimeInterval,
                        MIN_DISTANCE_FOR_UPDATES.toFloat(), this
                    )
                    if (locationManager != null) {
                        location =
                            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (location != null) {
                            lat = location!!.latitude
                            lon = location!!.longitude
                            height = location!!.altitude
                            uncertainty = location!!.accuracy.toDouble()
                            exactLocation = true
                        }
                    }
                }
            }

            if (!exactLocation) {
                // if Network is enabled and still no GPS fix achieved
                if (checkNetwork && canGetLocation) {
                    if (ActivityCompat.checkSelfPermission(
                            mContext!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            selTimeInterval,
                            MIN_DISTANCE_FOR_UPDATES.toFloat(), this
                        )
                        if (locationManager != null) {
                            location =
                                locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (location != null) {
                                lat = location!!.latitude
                                lon = location!!.longitude
                                height = location!!.altitude // 0
                                uncertainty = location!!.accuracy.toDouble() // 200
                                exactLocation = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "170, StopListener: $e")
        }
    }

    // Stop location service
    fun stopListener() {
        try {
            if (locationManager != null) {
                locationManager!!.removeUpdates(this@LocationService)
                locationManager = null

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "182, StopListener: Should stop GPS service.")
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "186, StopListener: $e")
        }

        if (alertSoundPref) {
            if (rToneP != null) {
                if (rToneP!!.isPlaying) {
                    rToneP!!.stop()
                }
                rToneP!!.release()
                rToneP = null
            }
        }
    }

    fun getLongitude(): Double {
        if (location != null) {
            lon = location!!.longitude
        }
        return lon
    }

    fun getLatitude(): Double {
        if (location != null) {
            lat = location!!.latitude
        }
        return lat
    }

    fun getAltitude(): Double {
        if (location != null) {
            height = location!!.altitude
        }
        return height
    }

    fun getAccuracy(): Double {
        if (location != null) {
            uncertainty = location!!.accuracy.toDouble()
        }
        return uncertainty
    }

    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onLocationChanged(location: Location) {
        // Show info about first lock
        if (isFirstLoc && lat != 0.0) {
            soundAlertSound()

            // European fauna area defined as inside the rectangle with
            //   latitude:   27.6 < lat < 71.2
            //   longitude: -31.3 < lon < 50.8
            var mesg: String
            if ((27.6 < lat && lat < 71.2) && (-31.3 < lon && lon < 50.8)) {
                mesg = mContext!!.getString(R.string.newLock) // in green
                Toast.makeText(
                    mContext!!,
                    HtmlCompat.fromHtml(
                        "<bold><font color='#008000'>$mesg</font></bold>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                mesg = mContext!!.getString(R.string.outsideEurope) // in blue
                Toast.makeText(
                    mContext!!,
                    HtmlCompat.fromHtml(
                        "<font color='blue'>$mesg</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    ), Toast.LENGTH_LONG
                ).show()
            }

            isFirstLoc = false
        }
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

    private fun soundAlertSound() {
        if (alertSoundPref) {
            if (rToneP!!.isPlaying) {
                rToneP!!.stop()
                rToneP!!.release()
            }
            rToneP!!.start()
        }
    }

    companion object {
        private const val TAG = "TourCountLocSrv"

        private const val MIN_DISTANCE_FOR_UPDATES: Long = 5 // default: 10 (m)
    }

}
