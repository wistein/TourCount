package com.wmstein.tourcount.database;

/*************************************************
 * Definitions for table Individuals
 * Created by wmstein for TourCount on 2016-04-20
 * Last edited on 2022-03-25
 */
public class Individuals
{
    public int id;
    int count_id;
    public String name;
    public double coord_x; // latitude
    public double coord_y; // longitude
    public double coord_z;
    public String uncert;
    String date_stamp;
    String time_stamp;
    public String locality;
    public String sex;
    public String stadium;
    public int state_1_6; // takes numbers 0-6 with 0 translated to "-"
    public String notes;
    public int icount;
    public int icategory; // 1=♂♀, 2=♂, 3=♀, 4=pupa, 5=larva, 6=egg
}
