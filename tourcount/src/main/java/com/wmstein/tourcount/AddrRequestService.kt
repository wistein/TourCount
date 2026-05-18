package com.wmstein.tourcount

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.util.Log
import android.widget.Toast

import androidx.core.net.toUri

import com.wmstein.tourcount.TourCountApplication.Companion.adrServiceOn
import com.wmstein.tourcount.TourCountApplication.Companion.isFirstLocality
import com.wmstein.tourcount.TourCountApplication.Companion.lat
import com.wmstein.tourcount.TourCountApplication.Companion.lon
import com.wmstein.tourcount.TourCountApplication.Companion.sLocality
import com.wmstein.tourcount.Utils.fromHtml
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.Timer
import java.util.TimerTask

import javax.net.ssl.HttpsURLConnection

/**************************************************************************************
 * AddrRequestService provides the address data: country, state, city, locality, street
 * and postal code on request in a background thread.
 * It shows a message and makes a sound when the initial locality is determined.
 * When counting on the way the current locality is added to each individual record.
 * 
 * To get this data it sends an HTTP request with the current location to the Nominatim
 * website of OpenStreetMap in a configurable interval (e.g. 10 seconds).
 *
 * Created by wmstein on 2026-05-07,
 * last edited on 2026-05-18
 */
open class AddrRequestService : Service {
    private lateinit var audioAttributionContext: Context

    // Prefs
    private var metaPref: Boolean = false // Option to use Nominatim service
    private var selRequestInterval: Long = 10000 // Default time interval for updates
    private var emailString: String = "" // Needed for reliable Nominatim service
    private var alertSoundPref = false
    private var alertSound: String = ""
    private var lastVersion: String = ""

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var counter = 0 // *new*

    private var rToneA: MediaPlayer? = null
    private var xmlString: String = "" // Constructed string from Nominatim response

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    constructor() {
    if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "77, constructor")

    val prefs = TourCountApplication.getPrefs()
        emailString = prefs.getString("email_String", "")!!
        metaPref = prefs.getBoolean("pref_metadata", false) // use Reverse Geocoding
        selRequestInterval = prefs.getString("pref_request_interval", "10000")!!.toLong()
        alertSoundPref = prefs.getBoolean("pref_alert_sound", false)
        alertSound = prefs.getString("alert_sound", "")!!
        lastVersion = prefs.getString("PREFS_VERSION_KEY", "")!!

        // Service runs in 2nd thread and with background priority
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "101, onStartCommand")

        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg) // to MessageQueue for the 2. thread
        }

        // If stopped, restart
        return START_STICKY
    }

    // Handler in 2. thread that receives messages from the main thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        // Receive the message from onStartCommand()
        override fun handleMessage(msg: Message) {
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "117, handleMessage")

            // Do the work here
            if (adrServiceOn) {
                try {
                    startTimer()
                } catch (_: InterruptedException) {
                    // Restore interrupt status.
                    Thread.currentThread().interrupt()
                }

                // Stop the service using the startId, so that it doesn't stop
                //  in the middle of handling an ongoing job
                stopSelf(msg.arg1)
            }
        }
    }

    fun startTimer() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "137, startTimer")

        timer = Timer()
        initialiseTimerTask()

        // Schedule the timer, after a delay of 100 ms to wake up every 10 s (default)
        timer?.schedule(
            timerTask,
            100,
            selRequestInterval
        ) // delay = 100 ms, request period (10000 ms)
    }

    fun initialiseTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.i(TAG, "154, initialiseTimerTask: " + counter++)

                audioAttributionContext =
                    if (Build.VERSION.SDK_INT >= 30)
                        applicationContext.createAttributionContext("ringSound")
                    else applicationContext

                val uriB = if (alertSound.isNotBlank())
                    alertSound.toUri()
                else
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                rToneA = MediaPlayer.create(audioAttributionContext, uriB)

                if (lat != 0.0 || lon != 0.0) {
                    if (alertSoundPref) {
                        getAddress()
                    }
                }
            }
        }
    }

    fun stopTimerTask() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "179, stopTimerTask")

        // Stop the timer, if it's not already null
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }

        stopSelf()
    }

    // If the user has set the preference for an audible alert, then sound it here.
    fun soundAlertSound() {
        if (alertSoundPref) {
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
        super.onDestroy()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "224, onDestroy")

        if (!adrServiceOn)
            stopTimerTask()

        if (alertSoundPref && rToneA != null) {
            rToneA!!.reset()
            rToneA!!.release()
            rToneA = null
        }
    }

    // Get the address data by reverse geocoding from Nominatim service
    private fun getAddress() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "239, getAddress")

        val urlString: String?
        val sb = StringBuilder()
        if (emailString == "") {
            urlString = ("https://nominatim.openstreetmap.org/reverse?email=test@temp.test"
                    + "&format=xml&lat=" + lat + "&lon=" + lon + "&zoom=18&addressdetails=1")
        } else {
            urlString = ("https://nominatim.openstreetmap.org/reverse?email=" + emailString
                    + "&format=xml&lat=" + lat + "&lon=" + lon + "&zoom=18&addressdetails=1")
        }

        // Get app version number for User-Agent (requested parameter for Nominatim service)
        val userAgent = "TourCount $lastVersion"

        // Prepare request for Nominatim Reverse Geocoder of OpenStreetMap
        val url = URL(urlString)
        val urlConnection = url.openConnection() as HttpsURLConnection
        urlConnection.readTimeout = 10000    // 10000
        urlConnection.connectTimeout = 10000 // 15000
        urlConnection.requestMethod = "GET"
        urlConnection.setRequestProperty("User-Agent", userAgent)
        urlConnection.doInput = true

        // Connect with Nominatim Reverse Geocoder of OpenStreetMap
        try {
            urlConnection.connect()
            val status = urlConnection.responseCode

            // Handle connection error
            if (status != HttpsURLConnection.HTTP_OK) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "271, Nominatim status: $status")

                urlConnection.disconnect()
            }

            // Get the XML from input stream of Nominatim
            val iStream = urlConnection.inputStream
            val reader = BufferedReader(InputStreamReader(iStream))
            var line: String? = ""

            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
            } catch (e: IOException) {
                if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                    Log.e(TAG, "287, Problem converting Stream to String: $e")
            } finally {
                reader.close()
                iStream.close()
            }
        } catch (e: IOException) {
            // SocketTimeoutException without email
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.e(TAG, "295, Problem with internet address handling: $e")
        } finally {
            urlConnection.disconnect()
        }

        xmlString = sb.toString()

        // Resolve the address
        val sectionDataSource = SectionDataSource(applicationContext)
        val sPlz: String
        val sCity: String
        val sPlace: String
        val sCountry: String
        val sState: String

        // Parse the XML content
        if (xmlString.contains("<addressparts>")) {
            var sstart = xmlString.indexOf("<addressparts>") + 14
            var send = xmlString.indexOf("</addressparts>")
            xmlString = xmlString.substring(sstart, send)
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "316, <addressparts>: $xmlString")

            val locality = StringBuilder() // quarter, road or street
            val plz = StringBuilder()      // postal code
            val city = StringBuilder()
            val place = StringBuilder()    // suburb
            val country = StringBuilder()
            val fedState = StringBuilder() // federal state

            // 1. Get country
            if (xmlString.contains("<country>")) {
                sstart = xmlString.indexOf("<country>") + 9
                send = xmlString.indexOf("</country>")
                val tcountry = xmlString.substring(sstart, send)
                country.append(tcountry)
            }
            sCountry = country.toString()

            // 2. Get state
            if (xmlString.contains("<state>")) {
                sstart = xmlString.indexOf("<state>") + 7
                send = xmlString.indexOf("</state>")
                val tstate = xmlString.substring(sstart, send)
                fedState.append(tstate)
            }
            sState = fedState.toString()

            // 3. Get city or town and village
            if (xmlString.contains("<city>")) {
                sstart = xmlString.indexOf("<city>") + 6
                send = xmlString.indexOf("</city>")
                val tcity = xmlString.substring(sstart, send)
                city.append(tcity)
            } else {
                if (xmlString.contains("<town>")) {
                    sstart = xmlString.indexOf("<town>") + 6
                    send = xmlString.indexOf("</town>")
                    val town = xmlString.substring(sstart, send)
                    city.append(town)
                }
            }
            if (city.toString() != "" && xmlString.contains("<village>")) city.append(", ")
            if (xmlString.contains("<village>")) {
                sstart = xmlString.indexOf("<village>") + 9
                send = xmlString.indexOf("</village>")
                val village = xmlString.substring(sstart, send)
                city.append(village)
            }
            sCity = city.toString()

            // 4. Get place with suburb
            if (xmlString.contains("<suburb>")) {
                sstart = xmlString.indexOf("<suburb>") + 8
                send = xmlString.indexOf("</suburb>")
                val suburb = xmlString.substring(sstart, send)
                place.append(suburb)
            }
            sPlace = place.toString()

            // 5. Get locality with quarter, road, street
            if (xmlString.contains("<quarter>")) {
                sstart = xmlString.indexOf("<quarter>") + 9
                send = xmlString.indexOf("</quarter>")
                val quarter = xmlString.substring(sstart, send)
                locality.append(quarter)
            }
            if (locality.toString() != "" && xmlString.contains("<road>")) locality.append(", ")
            if (xmlString.contains("<road>")) {
                sstart = xmlString.indexOf("<road>") + 6
                send = xmlString.indexOf("</road>")
                val road = xmlString.substring(sstart, send)
                locality.append(road)
            }
            if (locality.toString() != "" && xmlString.contains("<street>")) locality.append(", ")
            if (xmlString.contains("<street>")) {
                sstart = xmlString.indexOf("<street>") + 8
                send = xmlString.indexOf("</street>")
                val street = xmlString.substring(sstart, send)
                locality.append(street)
            }
            sLocality = locality.toString()
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "398, $sLocality")

            if (isFirstLocality && lat != 0.0) {
                soundAlertSound()

                // European fauna area defined as inside the rectangle with
                //   latitude:   27.6 < lat < 71.2
                //   longitude: -31.3 < lon < 50.8
                Looper.prepare()
                if ((lat in 27.6..71.2) && (lon in -31.3 .. 50.8)) {
                    // inside Europe
                    val mesg = applicationContext.getString(R.string.newLoc) + " $sLocality"
                    Toast.makeText( // blue
                        applicationContext,
                        fromHtml("<bold><font color='#008000'>$mesg</font></bold>"),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val mesg = applicationContext.getString(R.string.newLoc) + " $sLocality,\n" +
                            applicationContext.getString(R.string.outsideEurope)
                    Toast.makeText( // red
                        applicationContext,
                        fromHtml("<bold><font color='#ff6000'>$mesg</font></bold>"),
                        Toast.LENGTH_LONG
                    ).show()
                }
                isFirstLocality = false
            }

            // 6.  Get plz (postcode)
            if (xmlString.contains("<postcode>")) {
                sstart = xmlString.indexOf("<postcode>") + 10
                send = xmlString.indexOf("</postcode>")
                val postcode = xmlString.substring(sstart, send)
                plz.append(postcode)
            }
            sPlz = plz.toString()

            sectionDataSource.open()
            val section: Section = sectionDataSource.section

            // Save the initial sCountry, sState, sCity, sPlace, sLocality, sPlz to DB Section table
            if (section.country == "") {
                if (sCountry.isNotEmpty())
                    section.country = sCountry
                sectionDataSource.storeEmptyCountry(section.id, section.country)
            }
            if (section.b_state == "") {
                if (sState.isNotEmpty())
                    section.b_state = sState
                sectionDataSource.storeEmptyState(section.id, section.b_state)
            }

            if (section.city == "") {
                if (sCity.isNotEmpty())
                    section.city = sCity
                sectionDataSource.storeEmptyCity(section.id, section.city)
            }

            if (section.place == "") {
                if (sPlace.isNotEmpty())
                    section.place = sPlace
                sectionDataSource.storeEmptyPlace(section.id, section.place)
            }

            if (section.st_locality == "") {
                if (sLocality.isNotEmpty())
                    section.st_locality = sLocality
                sectionDataSource.storeEmptyStLocality(section.id, section.st_locality)
            }

            if (section.plz == "") {
                if (sPlz.isNotEmpty())
                    section.plz = sPlz
                sectionDataSource.storeEmptyPlz(section.id, section.plz)
            }
            sectionDataSource.close()
        }
    }

    companion object {
        private const val TAG = "AddrRequestService"
    }

}
