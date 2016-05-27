package com.wmstein.tourcount.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */
public class Section
{
    public int id;
    public long created_at;
    public String name;
    public String country;
    public String plz;
    public String city;
    public String place;
    public int temp;
    public int wind;
    public int clouds;
    public String date;
    public String start_tm;
    public String end_tm;
    public String notes;

    //Get Date from DB table sections field created_at
    public String getDate()
    {
        Date date = new Date(created_at);
        DateFormat df = SimpleDateFormat.getDateInstance();
        return df.format(date);
    }

}
