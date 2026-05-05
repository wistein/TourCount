package com.wmstein.tourcount

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
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
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

import com.wmstein.egm.EarthGravitationalModel
import com.wmstein.tourcount.TourCountApplication.Companion.heightNN
import com.wmstein.tourcount.TourCountApplication.Companion.isFirstLoc
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
 * last edited on 2026-05-05
 */
open class LocationService : Service, LocationListener {
    private var mContext: Context? = null
    private var locationAttributionContext: Context? = null
    private var audioAttributionContext: Context? = null
    private var checkGPS = false
    private var checkNetwork = false
    var canGetLocation: Boolean = false // must be public
    private var location: Location? = null
    private var heightGPS = 0.0
    private var locationManager: LocationManager? = null
    private var exactLocation = false
    private var rToneA: MediaPlayer? = null

    // prefs
    private var alertSoundPref = false
    private var alertSound: String = ""
    private var selTimeInterval: Long = 5000 // Default time interval for updates
    private var minDistanceM: Long = 10 // No movement between updates necessary
    private var metaPref: Boolean = false
    private var emailString: String = ""

    // Default constructor is demanded for service declaration in AndroidManifest.xml
    constructor() // not to be removed!

    constructor(mContext: Context?) {
        this.mContext = mContext
        getLocation()
    }

    fun getLocation() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "80, getLocation")

        audioAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("ringSound")
            else mContext
        locationAttributionContext =
            if (Build.VERSION.SDK_INT >= 30)
                mContext!!.createAttributionContext("locationCheck")
            else mContext

        val prefs = TourCountApplication.getPrefs()
        selTimeInterval = prefs.getString("pref_time_interval", "5000")!!.toLong()
        minDistanceM = prefs.getString("pref_distance", "10")!!.toLong()
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)
        alertSound = prefs.getString("alert_sound", null).toString()
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        emailString = prefs.getString("email_String", "").toString()

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
                    mContext,
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

                    if (locationManager != null) {
                        location =
                            locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (location != null) {
                            lat = location!!.latitude
                            lon = location!!.longitude
                            // set heightNN
                            heightGPS = location!!.altitude
                            if (heightGPS != 0.0)
                                correctHeight(lat, lon, heightGPS)
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
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            selTimeInterval,
                            minDistanceM.toFloat(), this
                        )

                        if (locationManager != null) {
                            location =
                                locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (location != null) {
                                lat = location!!.latitude
                                lon = location!!.longitude
                                heightNN = 0.0
                                uncertainty = location!!.accuracy.toDouble() // 200
                                exactLocation = false
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "177, Error in getLocation: $e")
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
                    Log.i(TAG, "214, StopListener: Stop GPS service.")
            }
        } catch (e: Exception) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "218, Error in StopListener: $e")
        }

        if (alertSoundPref) {
            if (rToneA != null) {
                if (rToneA!!.isPlaying) {
                    rToneA!!.stop() // stop media player
                }
                rToneA!!.release()
                rToneA = null
            }
        }
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

    fun canGetLocation(): Boolean {
        return this.canGetLocation
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    // Get locality info on first lock
    override fun onLocationChanged(location: Location) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "269, onLocationChanged")
        getLatitude()
        getLongitude()
        getAltitude()
        getAccuracy()

        if (isFirstLoc && lat != 0.0) {
            soundAlertSound()

            // European fauna area defined as inside the rectangle with
            //   latitude:   27.6 < lat < 71.2
            //   longitude: -31.3 < lon < 50.8
            var mesg: String
            if ((27.6 < lat && lat < 71.2) && (-31.3 < lon && lon < 50.8)) {
                mesg = mContext!!.getString(R.string.newLock) // in green
                Toast.makeText( // bright green
                    mContext,
                    fromHtml("<bold><font color='#008000'>$mesg</font></bold>"),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                mesg = mContext!!.getString(R.string.outsideEurope) // in blue
                Toast.makeText( // orange
                    mContext,
                    fromHtml("<font color='#ff6000'>$mesg</font>"),
                    Toast.LENGTH_LONG
                ).show()
            }
            isFirstLoc = false
        }

        // Get location data from Nominatim
        if (metaPref)
            getAddressL()
    }

    override fun onProviderEnabled(s: String) {
        // do nothing
    }

    override fun onProviderDisabled(s: String) {
        // do nothing
    }

    // Get the address data by reverse geocoding from Nominatim service
    private fun getAddressL() {
        if (lat != 0.0 || lon != 0.0) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG,"317, getAddressL, lat = $lat")
            val urlString: String?
            if (emailString == "") {
                urlString = ("https://nominatim.openstreetmap.org/reverse?" +
                        "email=test@temp.test" + "&format=xml&lat="
                        + lat + "&lon=" + lon + "&zoom=18&addressdetails=1")
            } else {
                urlString = ("https://nominatim.openstreetmap.org/reverse?" +
                        "email=" + emailString + "&format=xml&lat="
                        + lat + "&lon=" + lon + "&zoom=18&addressdetails=1")
            }

            // Start RetrieveAddrWorker immediately (with setExpedited())
            val retrieveAddrWorkRequest =
                OneTimeWorkRequestBuilder<RetrieveAddrWorker>()
                    .setInputData(
                        Data.Builder()
                            .putString("URL_STRING", urlString)
                            .build()
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            WorkManager.getInstance(mContext!!).enqueue(retrieveAddrWorkRequest)
        }
    }

    private fun soundAlertSound() {
        if (alertSoundPref) {
            val uriB = if (alertSound.isNotBlank())
                alertSound.toUri()
            else
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            rToneA = MediaPlayer.create(audioAttributionContext, uriB)
            if (rToneA!!.isPlaying) {
                rToneA!!.stop()
                rToneA!!.release()
            }
            rToneA!!.start()
        }
    }

    // Release alert sound, called by WelcomeActivity
    fun releaseSoundA() {
        if (alertSoundPref && rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
    }

    // Stop alert sound, called by WelcomeActivity when denied in settings
    fun stopSoundA() {
        if (rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
    }

    override fun onDestroy() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "379, onDestroy")

        if (alertSoundPref && rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LocationService"
    }

}
