package com.wmstein.tourcount

import android.content.ContentValues
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
 * last edited on 2023-12-15
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
                if (MyDebug.LOG) Log.e(ContentValues.TAG, "59, Problem converting Stream to String: $e")
            } finally {
                try {
                    iStream.close()
                } catch (e: IOException) {
                    if (MyDebug.LOG) Log.e(ContentValues.TAG, "64, Problem closing InputStream: $e")
                }
            }
            xmlString = sb.toString()
            if (MyDebug.LOG) Log.d(
                ContentValues.TAG,
                "70, xmlString: $xmlString"
            ) // Log gzip-content of url

            // Parse Geocoder string to write DB fields
            val sectionDataSource: SectionDataSource
            val tempDataSource: TempDataSource
            val sLocality: String
            val sPlz: String
            val sCity: String
            val sPlace: String
            val sCountry: String

            // parse the XML content
            if (xmlString.contains("<addressparts>")) {
                var sstart = xmlString.indexOf("<addressparts>") + 14
                var send = xmlString.indexOf("</addressparts>")
                xmlString = xmlString.substring(sstart, send)
                if (MyDebug.LOG) Log.d(ContentValues.TAG, "87, <addressparts>: $xmlString")
                val locality = StringBuilder()
                val plz = StringBuilder()
                val city = StringBuilder()
                val place = StringBuilder()
                val country = StringBuilder()

                // 1. Get locality with road, street and suburb
                if (xmlString.contains("<road>")) {
                    sstart = xmlString.indexOf("<road>") + 6
                    send = xmlString.indexOf("</road>")
                    val road = xmlString.substring(sstart, send)
                    locality.append(road)
                }
                if (xmlString.contains("<street>")) {
                    sstart = xmlString.indexOf("<street>") + 8
                    send = xmlString.indexOf("</street>")
                    val street = xmlString.substring(sstart, send)
                    locality.append(street)
                }
                if (locality.toString() != "" && xmlString.contains("<suburb>")) locality.append(", ")
                if (xmlString.contains("<suburb>")) {
                    sstart = xmlString.indexOf("<suburb>") + 8
                    send = xmlString.indexOf("</suburb>")
                    val suburb = xmlString.substring(sstart, send)
                    locality.append(suburb)
                }
                sLocality = locality.toString()

                // 2. Get place with city_district and village
                if (xmlString.contains("<city_district>")) {
                    sstart = xmlString.indexOf("<city_district>") + 15
                    send = xmlString.indexOf("</city_district>")
                    val cityDistrict = xmlString.substring(sstart, send)
                    place.append(cityDistrict)
                }
                if (place.toString() != "" && xmlString.contains("<village>")) place.append(", ")
                if (xmlString.contains("<village>")) {
                    sstart = xmlString.indexOf("<village>") + 9
                    send = xmlString.indexOf("</village>")
                    val village = xmlString.substring(sstart, send)
                    place.append(village)
                }
                sPlace = place.toString()

                // 3.  Get plz (postcode)
                if (xmlString.contains("<postcode>")) {
                    sstart = xmlString.indexOf("<postcode>") + 10
                    send = xmlString.indexOf("</postcode>")
                    val postcode = xmlString.substring(sstart, send)
                    plz.append(postcode)
                }
                sPlz = plz.toString()

                // 4. Get city with city and town or county
                if (xmlString.contains("<city>")) {
                    sstart = xmlString.indexOf("<city>") + 6
                    send = xmlString.indexOf("</city>")
                    val tcity = xmlString.substring(sstart, send)
                    city.append(tcity)
                }
                if (city.toString() != "" && xmlString.contains("<town>")) city.append(", ")
                if (xmlString.contains("<town>")) {
                    sstart = xmlString.indexOf("<town>") + 6
                    send = xmlString.indexOf("</town>")
                    val town = xmlString.substring(sstart, send)
                    city.append(town)
                }
                if (xmlString.contains("<county>")) {
                    sstart = xmlString.indexOf("<county>") + 8
                    send = xmlString.indexOf("</county>")
                    val county = xmlString.substring(sstart, send)
                    if (city.toString() != "") {
                        city.append(", ")
                    }
                    city.append(county)
                }
                sCity = city.toString()

                // 5. Get country
                if (xmlString.contains("<country>")) {
                    sstart = xmlString.indexOf("<country>") + 9
                    send = xmlString.indexOf("</country>")
                    val tcountry = xmlString.substring(sstart, send)
                    country.append(tcountry)
                }
                sCountry = country.toString()
                sectionDataSource = SectionDataSource(TourCountApplication.getAppContext())
                sectionDataSource.open()
                val section: Section = sectionDataSource.section

                // Save sCountry, sPlz, sCity, sPlace to DB Section
                if (sCountry.isNotEmpty()) {
                    section.country = sCountry
                } else {
                    section.country = ""
                }
                sectionDataSource.updateEmptyCountry(section.id, section.country)
                if (sPlz.isNotEmpty()) {
                    section.plz = sPlz
                } else {
                    section.plz = ""
                }
                sectionDataSource.updateEmptyPlz(section.id, section.plz)
                if (sCity.isNotEmpty()) {
                    section.city = sCity
                } else {
                    section.city = ""
                }
                sectionDataSource.updateEmptyCity(section.id, section.city)
                if (sPlace.isNotEmpty()) {
                    section.place = sPlace
                } else {
                    section.place = ""
                }
                sectionDataSource.updateEmptyPlace(section.id, section.place)
                sectionDataSource.close()

                // Save sLocality to DB table Temp
                tempDataSource = TempDataSource(TourCountApplication.getAppContext())
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
            if (MyDebug.LOG) Log.e(ContentValues.TAG, "218, Problem with address handling: $e")
        }
        return Result.success()
    }
}
