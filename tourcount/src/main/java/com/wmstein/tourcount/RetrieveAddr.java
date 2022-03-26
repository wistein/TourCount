package com.wmstein.tourcount;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.wmstein.tourcount.database.Section;
import com.wmstein.tourcount.database.SectionDataSource;
import com.wmstein.tourcount.database.Temp;
import com.wmstein.tourcount.database.TempDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.content.ContentValues.TAG;

/************************************************************************************
 * Get, parse and store address info from Nominatim Reverse Geocoder of OpenStreetMap
 *
 * Copyright 2018-2022 wmstein
 * created on 2018-03-10, last modification on 2022-03-24
 */
public class RetrieveAddr extends AsyncTask<URL, Void, String>
{
    @SuppressLint("StaticFieldLeak")
    private final Context aContext;

    RetrieveAddr(Context context)
    {
        aContext = context;
    }

    @Override
    protected String doInBackground(URL... params)
    {
        URL url = params[0];
        String xmlString;

        try
        {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.connect();

            int status = urlConnection.getResponseCode();
            if (status >= 400) // Error
            {
                return "";
            }

            // get the XML from input stream
            InputStream iStream = urlConnection.getInputStream();

            xmlString = convertStreamToString(iStream);
            if (MyDebug.LOG)
                Log.d(TAG, "xmlString: " + xmlString); // Log gzip-content of url

        } catch (IOException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Problem with address handling: " + e);
            xmlString = "";
            return xmlString;
        }
        return xmlString;
    }

    // Parse Geocoder string to write DB fields
    protected void onPostExecute(String xmlString)
    {
        super.onPostExecute(xmlString);

        SectionDataSource sectionDataSource;
        TempDataSource tempDataSource;
        String sLocality, sPlz, sCity, sPlace, sCountry;

        // parse the XML content 
        if (xmlString.contains("<addressparts>"))
        {
            int sstart = xmlString.indexOf("<addressparts>") + 14;
            int send = xmlString.indexOf("</addressparts>");
            xmlString = xmlString.substring(sstart, send);
            if (MyDebug.LOG)
                Log.d(TAG, "<addressparts>: " + xmlString);

            StringBuilder locality = new StringBuilder();
            StringBuilder plz = new StringBuilder();
            StringBuilder city = new StringBuilder();
            StringBuilder place = new StringBuilder();
            StringBuilder country = new StringBuilder();

            // 1. Get locality with road, street and suburb
            if (xmlString.contains("<road>"))
            {
                sstart = xmlString.indexOf("<road>") + 6;
                send = xmlString.indexOf("</road>");
                String road = xmlString.substring(sstart, send);
                locality.append(road);
            }

            if (xmlString.contains("<street>"))
            {
                sstart = xmlString.indexOf("<street>") + 8;
                send = xmlString.indexOf("</street>");
                String street = xmlString.substring(sstart, send);
                locality.append(street);
            }
            if (!locality.toString().equals("") && xmlString.contains("<suburb>"))
                locality.append(", ");

            if (xmlString.contains("<suburb>"))
            {
                sstart = xmlString.indexOf("<suburb>") + 8;
                send = xmlString.indexOf("</suburb>");
                String suburb = xmlString.substring(sstart, send);
                locality.append(suburb);
            }
            sLocality = locality.toString();

            // 2. Get place with city_district and village
            if (xmlString.contains("<city_district>"))
            {
                sstart = xmlString.indexOf("<city_district>") + 15;
                send = xmlString.indexOf("</city_district>");
                String city_district = xmlString.substring(sstart, send);
                place.append(city_district);
            }

            if (!place.toString().equals("") && xmlString.contains("<village>"))
                place.append(", ");

            if (xmlString.contains("<village>"))
            {
                sstart = xmlString.indexOf("<village>") + 9;
                send = xmlString.indexOf("</village>");
                String village = xmlString.substring(sstart, send);
                place.append(village);
            }
            sPlace = place.toString();

            // 3.  Get plz (postcode)
            if (xmlString.contains("<postcode>"))
            {
                sstart = xmlString.indexOf("<postcode>") + 10;
                send = xmlString.indexOf("</postcode>");
                String postcode = xmlString.substring(sstart, send);
                plz.append(postcode);
            }
            sPlz = plz.toString();

            // 4. Get city with city and town or county
            if (xmlString.contains("<city>"))
            {
                sstart = xmlString.indexOf("<city>") + 6;
                send = xmlString.indexOf("</city>");
                String tcity = xmlString.substring(sstart, send);
                city.append(tcity);
            }

            if (!city.toString().equals("") && xmlString.contains("<town>"))
                city.append(", ");

            if (xmlString.contains("<town>"))
            {
                sstart = xmlString.indexOf("<town>") + 6;
                send = xmlString.indexOf("</town>");
                String town = xmlString.substring(sstart, send);
                city.append(town);
            }

            if (xmlString.contains("<county>"))
            {
                sstart = xmlString.indexOf("<county>") + 8;
                send = xmlString.indexOf("</county>");
                String county = xmlString.substring(sstart, send);
                if (!city.toString().equals(""))
                {
                    city.append(", ");
                }
                city.append(county);
            }
            sCity = city.toString();

            // 5. Get country 
            if (xmlString.contains("<country>"))
            {
                sstart = xmlString.indexOf("<country>") + 9;
                send = xmlString.indexOf("</country>");
                String tcountry = xmlString.substring(sstart, send);
                country.append(tcountry);
            }
            sCountry = country.toString();

            Section section;
            sectionDataSource = new SectionDataSource(aContext);
            sectionDataSource.open();
            section = sectionDataSource.getSection();

            // Save sCountry, sPlz, sCity, sPlace to DB Section
            if (sCountry.length() > 0)
            {
                section.country = sCountry;
            }
            else
            {
                section.country = "";
            }
            sectionDataSource.updateEmptyCountry(section.id, section.country);

            if (sPlz.length() > 0)
            {
                section.plz = sPlz;
            }
            else
            {
                section.plz = "";
            }
            sectionDataSource.updateEmptyPlz(section.id, section.plz);

            if (sCity.length() > 0)
            {
                section.city = sCity;
            }
            else
            {   section.city = "";
            }
            sectionDataSource.updateEmptyCity(section.id, section.city);


            if (sPlace.length() > 0)
            {
                section.place = sPlace;
            }
            else
            {
                section.place = "";
            }
            sectionDataSource.updateEmptyPlace(section.id, section.place);


            // Save sLocality to DB table Temp
            Temp temp;
            tempDataSource = new TempDataSource(aContext);
            tempDataSource.open();
            temp = tempDataSource.getTemp();

            if (sLocality.length() > 0)
            {
                temp.temp_loc = sLocality;
            }
            else
            {
                temp.temp_loc = "";
            }
            tempDataSource.saveTempLoc(temp);

            sectionDataSource.close();
            tempDataSource.close();
        }
        
    }

    private String convertStreamToString(InputStream is)
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                sb.append(line).append('\n');
            }
        } catch (IOException e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Problem converting Stream to String: " + e);
        } finally
        {
            try
            {
                is.close();
            } catch (IOException e)
            {
                if (MyDebug.LOG)
                    Log.e(TAG, "Problem closing InputStream: " + e);
            }
        }
        return sb.toString();
    }

}
