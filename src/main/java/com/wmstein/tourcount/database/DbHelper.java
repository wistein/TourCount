package com.wmstein.tourcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 19.04.2016
 */
public class DbHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "tourcount.db";
    // tables
    public static final String SECTION_TABLE = "sections";
    public static final String COUNT_TABLE = "counts";
    public static final String HEAD_TABLE = "head";
    public static final String INDIVIDUALS_TABLE = "individuals";
    public static final String TEMP_TABLE = "temp";
    // fields
    public static final String S_ID = "_id";
    public static final String S_NAME = "name";
    public static final String S_COUNTRY = "country";
    public static final String S_PLZ = "plz";
    public static final String S_CITY = "city";
    public static final String S_PLACE = "place";
    public static final String S_TEMP = "temp";
    public static final String S_WIND = "wind";
    public static final String S_CLOUDS = "clouds";
    public static final String S_DATE = "date";
    public static final String S_START_TM = "start_tm";
    public static final String S_END_TM = "end_tm";
    public static final String S_NOTES = "notes";
    public static final String C_ID = "_id";
    public static final String C_COUNT = "count";
    public static final String C_NAME = "name";
    public static final String C_CODE = "code";
    public static final String C_NOTES = "notes";
    public static final String H_ID = "_id";
    public static final String H_OBSERVER = "observer";
    public static final String I_ID = "_id";
    public static final String I_COUNT_ID = "count_id";
    public static final String I_NAME = "name";
    public static final String I_COORD_X = "coord_x";
    public static final String I_COORD_Y = "coord_y";
    public static final String I_COORD_Z = "coord_z";
    public static final String I_UNCERT = "uncert";
    public static final String I_DATE_STAMP = "date_stamp";
    public static final String I_TIME_STAMP = "time_stamp";
    public static final String I_LOCALITY = "locality";
    public static final String I_SEX = "sex";
    public static final String I_STADIUM = "stadium";
    public static final String I_STATE_1_6 = "state_1_6";
    public static final String I_NOTES = "notes";
    public static final String T_ID = "_id";
    public static final String T_TEMP_LOC = "temp_loc";
    public static final String T_TEMP_CNT = "temp_cnt";
    static final String TAG = "TourCount DB";
    private static final int DATABASE_VERSION = 1;
    private SQLiteDatabase db;

    // constructor
    public DbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // called once on database creation
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        //Log.i(TAG, "Creating database: " + DATABASE_NAME);
        String sql = "create table " + SECTION_TABLE + " ("
            + S_ID + " integer primary key, "
            + S_NAME + " text, "
            + S_COUNTRY + " text, "
            + S_PLZ + " text, "
            + S_CITY + " text, "
            + S_PLACE + " text, "
            + S_TEMP + " int, "
            + S_WIND + " int, "
            + S_CLOUDS + " int, "
            + S_DATE + " text, "
            + S_START_TM + " text, "
            + S_END_TM + " text, "
            + S_NOTES + " text)";
        db.execSQL(sql);
        sql = "create table " + COUNT_TABLE + " ("
            + C_ID + " integer primary key, "
            + C_COUNT + " int, "
            + C_NAME + " text, "
            + C_CODE + " text, "
            + C_NOTES + " text default NULL)";
        db.execSQL(sql);
        sql = "create table " + HEAD_TABLE + " ("
            + H_ID + " integer primary key, "
            + H_OBSERVER + " text)";
        db.execSQL(sql);
        sql = "create table " + TEMP_TABLE + " ("
            + T_ID + " integer primary key, "
            + T_TEMP_LOC + " text, "
            + T_TEMP_CNT + " int)";
        db.execSQL(sql);
        sql = "create table " + INDIVIDUALS_TABLE + " ("
            + I_ID + " integer primary key, "
            + I_COUNT_ID + " int, "
            + I_NAME + " text, "
            + I_COORD_X + " numeric, "
            + I_COORD_Y + " numeric, "
            + I_COORD_Z + " numeric, "
            + I_UNCERT + " text, "
            + I_DATE_STAMP + " text, "
            + I_TIME_STAMP + " text, "
            + I_LOCALITY + " text, "
            + I_SEX + " text, "
            + I_STADIUM + " text, "
            + I_STATE_1_6 + " int, "
            + I_NOTES + " text)";
        db.execSQL(sql);

        //create empty row for SECTION_TABLE, HEAD_TABLE and TEMP_TABLE

        ContentValues values1 = new ContentValues();
        values1.put(S_ID, 1);
        values1.put(S_NAME, "");
        db.insert(SECTION_TABLE, null, values1);

        values1 = new ContentValues();
        values1.put(H_ID, 1);
        values1.put(H_OBSERVER, "");
        db.insert(HEAD_TABLE, null, values1);

        values1 = new ContentValues();
        values1.put(T_ID, 1);
        values1.put(T_TEMP_LOC, "");
        values1.put(T_TEMP_CNT, 0);
        db.insert(TEMP_TABLE, null, values1);

        //Log.i(TAG, "Success!");
    }

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // placeholder as class demands for it, see beeCount or 
    // https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //nothing to upgrade
        // getString() in Toast doesn't work here
        //Toast.makeText(mContext.getApplicationContext(), getString(R.string.wait), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, getString(R.string.wait), Toast.LENGTH_LONG).show();
    }

}
