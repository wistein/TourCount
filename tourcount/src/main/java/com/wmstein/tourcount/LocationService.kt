package com.wmstein.tourcount

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast

import androidx.core.app.ActivityCompat

import com.wmstein.egm.EarthGravitationalModel
import com.wmstein.tourcount.TourCountApplication.Companion.heightNN
import com.wmstein.tourcount.TourCountApplication.Companion.lat
import com.wmstein.tourcount.TourCountApplication.Companion.lon
import com.wmstein.tourcount.TourCountApplication.Companion.uncertainty
import com.wmstein.tourcount.Utils.fromHtml

import java.io.IOException

/*****************************************************************************************
 * LocationService provides the location data: latitude, longitude, heightGPS, uncertainty
 * on request. Its data may be read by most TourCount activities.
 *
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * licensed under the MIT License.
 *
 * Part of that code was adopted for TourCount by wmstein since 2018-07-26.
 *
 * Last edited in Java on 2023-05-30,
 * converted to Kotlin on 2023-05-26,
 * last edited on 2026-05-23
 */
open class LocationService : Service, LocationListener {
    private var mContext: Context? = null
    private var locationAttributionContext: Context? = null
    private var checkGPS = false
    private var checkNetwork = false
    var canGetLocation: Boolean = false // must be public
    private var location: Location? = null
    private var heightGPS = 0.0
    private var locationManager: LocationManager? = null
    private var exactLocation = false

    // prefs
    private var selTimeInterval: Long = 5000 // Default time interval for updates
    private var minDistanceM: Long = 10 // No movement between updates necessary

    // Default constructor is demanded for service declaration
    constructor() // not to be removed!

    constructor(context: Context) {
        this.mContext = context
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "61, getLocation")

        locationAttributionContext =
            mContext!!.createAttributionContext("locationCheck")

        val prefs = TourCountApplication.getPrefs()
        selTimeInterval = prefs.getString("pref_time_interval", "5000")!!.toLong()
        minDistanceM = prefs.getString("pref_distance", "10")!!.toLong()

        try {
            locationManager =
                locationAttributionContext!!.getSystemService(LOCATION_SERVICE) as LocationManager

            // get GPS status
            checkGPS = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // get network provider status
            checkNetwork = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (checkGPS || checkNetwork) {
                this.canGetLocation = true
            } else {
                val mesg = getString(R.string.no_provider)
                Toast.makeText(
                    mContext!!,
                    fromHtml("<font color='red'><b>$mesg</b></font>"),
                    Toast.LENGTH_LONG
                ).show()
            }

            // if GPS is enabled get position using GPS Service
            if (checkGPS && canGetLocation) {
                if (ActivityCompat.checkSelfPermission(
                        mContext!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        selTimeInterval,
                        minDistanceM.toFloat(), this
                    )
                }
                exactLocation = true
            }

            if (!exactLocation) {
                // if Network is enabled and still no GPS fix achieved
                if (checkNetwork && canGetLocation) {
                    if (ActivityCompat.checkSelfPermission(
                            mContext!!,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            selTimeInterval,
                            minDistanceM.toFloat(), this
                        )
                    }
                    exactLocation = false
                }
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "126, Error in getLocation: $e")
        }
    }

    // Get locality info when location has changed
    override fun onLocationChanged(loc: Location) {
        if (ActivityCompat.checkSelfPermission(
                mContext!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (locationManager != null) {
                if (exactLocation) {
                    location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        lat = location!!.latitude
                        lon = location!!.longitude
                        // set heightNN
                        heightGPS = location!!.altitude
                        if (heightGPS != 0.0)
                            correctHeight(lat, lon, heightGPS)
                        uncertainty = location!!.accuracy.toDouble()
                    }
                } else {
                    location =
                        locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (location != null) {
                        lat = location!!.latitude
                        lon = location!!.longitude
                        heightNN = 0.0
                        uncertainty = location!!.accuracy.toDouble() // 200
                    }
                }
            }
        }
    }

    // Correct height with geoid offset from simplified EarthGravitationalModel
    private fun correctHeight(latitude: Double, longitude: Double, gpsHeight: Double) {
        var corrHeight = 0.0

        val gh = EarthGravitationalModel()
        try {
            gh.load(locationAttributionContext!!) // load the WGS84 correction coefficient table egm180.txt
        } catch (_: IOException) {
            // nothing
        }

        // Calculate the offset between the ellipsoid and geoid
        try {
            corrHeight = gh.heightOffset(latitude, longitude, gpsHeight)
        } catch (_: java.lang.Exception) {
            // nothing
        }

        heightNN = gpsHeight + corrHeight
    }

    // Stop location service
    fun stopListener() {
        try {
            if (locationManager != null) {
                locationManager!!.removeUpdates(this@LocationService)
                stopSelf()
                locationManager = null

                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "193, StopListener: Stop GPS service.")
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "197, Error in StopListener: $e")
        }
    }

    fun canGetLocation(): Boolean {
        return this.canGetLocation
    }

    fun getLongitude() {
        if (location != null) {
            lon = location!!.longitude
        }
    }

    fun getLatitude() {
        if (location != null) {
            lat = location!!.latitude
        }
    }

    fun getAltitude() {
        if (location != null) {
            heightGPS = location!!.altitude
            // Write corrected height to global var heightNN
            if (heightGPS != 0.0) correctHeight(lat, lon, heightGPS)
        }
    }

    fun getAccuracy() {
        if (location != null) {
            uncertainty = location!!.accuracy.toDouble()
        }
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "245, onDestroy")

        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocationService"
    }

}
