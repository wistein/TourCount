package com.wmstein.tourcount

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wmstein.tourcount.database.Section
import com.wmstein.tourcount.database.SectionDataSource
import com.wmstein.tourcount.database.Temp
import com.wmstein.tourcount.database.TempDataSource
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**********************************************************************************************
 * Worker to get, parse and store address info from Nominatim Reverse Geocoder of OpenStreetMap
 *
 * Copyright 2018-2023 wmstein
 * created on 2018-03-10,
 * last modification in Java on 2023-05-30,
 * converted to Kotlin on 2023-07-09,
 * last edited on 2025-03-06
 */
class RetrieveAddrWorker(context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {
    override fun doWork(): Result {
        var xmlString: String
        val url: URL

        // get parameters from calling Activity
        val urlString = inputData.getString("URL_STRING") ?: return Result.failure()
        try {
            url = URL(urlString)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.readTimeout = 10000
            urlConnection.connectTimeout = 15000
            urlConnection.requestMethod = "GET"
            urlConnection.doInput = true
            urlConnection.connect()
            val status = urlConnection.responseCode
            if (status >= 400) // Error
            {
                return Result.failure()
            }

            // get the XML from input stream
            val iStream = urlConnection.inputStream
            val reader = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuilder()
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
            } catch (e: IOException) {
                if (MyDebug.DLOG) Log.e(TAG, "58, Problem converting Stream to String: $e")
            } finally {
                try {
                    iStream.close()
                } catch (e: IOException) {
                    if (MyDebug.DLOG) Log.e(TAG, "63, Problem closing InputStream: $e")
                }
            }
            xmlString = sb.toString()
            // Log gzip-content of url
            if (MyDebug.DLOG) Log.d(TAG, "68, xmlString: $xmlString")

            // Parse Geocoder string to write DB fields
            val sectionDataSource: SectionDataSource
            val tempDataSource: TempDataSource
            val sLocality: String
            val sPlz: String
            val sCity: String
            val sPlace: String
            val sCountry: String
            val sState: String

            // parse the XML content
            if (xmlString.contains("<addressparts>")) {
                var sstart = xmlString.indexOf("<addressparts>") + 14
                var send = xmlString.indexOf("</addressparts>")
                xmlString = xmlString.substring(sstart, send)
                if (MyDebug.DLOG) Log.d(TAG, "85, <addressparts>: $xmlString")

                val locality = StringBuilder()
                val plz = StringBuilder()
                val city = StringBuilder()
                val place = StringBuilder() //
                val country = StringBuilder() // Land
                val b_state = StringBuilder() // Bundesland

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
                    b_state.append(tstate)
                }
                sState = b_state.toString()

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

                // 6.  Get plz (postcode)
                if (xmlString.contains("<postcode>")) {
                    sstart = xmlString.indexOf("<postcode>") + 10
                    send = xmlString.indexOf("</postcode>")
                    val postcode = xmlString.substring(sstart, send)
                    plz.append(postcode)
                }
                sPlz = plz.toString()

                sectionDataSource = SectionDataSource(applicationContext)
                sectionDataSource.open()
                val section: Section = sectionDataSource.section

                // Save sCountry, sState, sCity, sPlace, sLocality, sPlz to DB Section table
                if (section.country == "") {
                    if (sCountry.isNotEmpty()) {
                        section.country = sCountry
                    } else {
                        section.country = ""
                    }
                    sectionDataSource.storeEmptyCountry(section.id, section.country)
                }
                if (section.b_state == "") {
                    if (sState.isNotEmpty()) {
                        section.b_state = sState
                    } else {
                        section.b_state = ""
                    }
                    sectionDataSource.storeEmptyState(section.id, section.b_state)
                }

                if (section.city == "") {
                    if (sCity.isNotEmpty()) {
                        section.city = sCity
                    } else {
                        section.city = ""
                    }
                    sectionDataSource.storeEmptyCity(section.id, section.city)
                }

                if (sPlace.isNotEmpty()) {
                    section.place = sPlace
                } else {
                    section.place = ""
                }
                sectionDataSource.storeEmptyPlace(section.id, section.place)

                if (section.st_locality == "") {
                    if (sLocality.isNotEmpty()) {
                        section.st_locality = sLocality
                    } else {
                        section.st_locality = ""
                    }
                    sectionDataSource.storeEmptyStLocality(section.id, section.st_locality)
                }

                if (section.plz == "") {
                    if (sPlz.isNotEmpty()) {
                        section.plz = sPlz
                    } else {
                        section.plz = ""
                    }
                    sectionDataSource.storeEmptyPlz(section.id, section.plz)
                }

                sectionDataSource.close()

                // Save sLocality to temp_loc in DB table Temp
                tempDataSource = TempDataSource(applicationContext)
                tempDataSource.open()
                val tmp: Temp = tempDataSource.tmp

                if (sLocality.isNotEmpty()) {
                    tmp.temp_loc = sLocality
                } else {
                    tmp.temp_loc = ""
                }
                tempDataSource.saveTempLoc(tmp)

                tempDataSource.close()
            }
        } catch (e: IOException) {
            if (MyDebug.DLOG) Log.e(TAG, "220, Problem with address handling: $e")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "RetrAddrWorker"
    }

}
