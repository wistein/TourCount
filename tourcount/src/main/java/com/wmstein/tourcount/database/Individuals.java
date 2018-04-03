package com.wmstein.tourcount.database;

/*************************************************
 * 
 * Created by wmstein for TourCount on 2016-04-20
 * Last updated on 2018-03-26
 */
public class Individuals
{
    public int id;
    public int count_id;
    public String name;
    public double coord_x; // latitude
    public double coord_y; // longitude
    public double coord_z;
    public String uncert;
    public String date_stamp;
    public String time_stamp;
    public String locality;
    public String sex;
    public String stadium;
    public int state_1_6;
    public String notes;
    public int icount;
    public int icategory; // 1=♂♀, 2=♂, 3=♀, 4=pupa, 5=larva, 6=egg
}
