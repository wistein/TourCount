package com.wmstein.tourcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.wmstein.tourcount.MyDebug;
import com.wmstein.tourcount.R;

/**************************************************
 * Based on DbHelper created by milo on 05/05/2014.
 * Adopted for TourCount by wmstein on 2016-04-19,
 * updated to version 2 on 2017-09-09,
 * updated to version 3 on 2018-03-31
 * last edited on 2020-01-26
 */
public class DbHelper extends SQLiteOpenHelper
{
    private static final String TAG = "TourCount DBHelper";
    private static final String DATABASE_NAME = "tourcount.db";
    //DATABASE_VERSION 2: New extra column icount added to INDIVIDUALS_TABLE
    //DATABASE_VERSION 3: New extra columns for sexus and stadiums added to COUNT_TABLE
    //DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names 
    private static final int DATABASE_VERSION = 4;

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
    public static final String C_COUNT_F1I = "count_f1i";
    public static final String C_COUNT_F2I = "count_f2i";
    public static final String C_COUNT_F3I = "count_f3i";
    public static final String C_COUNT_PI = "count_pi";
    public static final String C_COUNT_LI = "count_li";
    public static final String C_COUNT_EI = "count_ei";
    public static final String C_NAME = "name";
    public static final String C_CODE = "code";
    public static final String C_NOTES = "notes";
    public static final String C_NAME_G = "name_g";

    public static final String C_COUNT = "count"; //deprecated

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
    public static final String I_ICOUNT = "icount";
    public static final String I_CATEGORY = "icategory";

    public static final String T_ID = "_id";
    public static final String T_TEMP_LOC = "temp_loc";
    public static final String T_TEMP_CNT = "temp_cnt";

    private Context mContext;

    // constructor
    public DbHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    // called once on database creation
    @Override
    public void onCreate(SQLiteDatabase db)
    {
        if (MyDebug.LOG)
            Log.i(TAG, "Creating database: " + DATABASE_NAME);
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
            + C_COUNT_F1I + " int, "
            + C_COUNT_F2I + " int, "
            + C_COUNT_F3I + " int, "
            + C_COUNT_PI + " int, "
            + C_COUNT_LI + " int, "
            + C_COUNT_EI + " int, "
            + C_NAME + " text, "
            + C_CODE + " text, "
            + C_NOTES + " text, "
            + C_NAME_G + " text)";
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
            + I_NOTES + " text, "
            + I_ICOUNT + " int, "
            + I_CATEGORY + " int)";
        db.execSQL(sql);

        //create empty row for SECTION_TABLE
        ContentValues values1 = new ContentValues();
        values1.put(S_ID, 1);
        values1.put(S_NAME, "");
        db.insert(SECTION_TABLE, null, values1);

        //create empty row for HEAD_TABLE
        values1 = new ContentValues();
        values1.put(H_ID, 1);
        values1.put(H_OBSERVER, "");
        db.insert(HEAD_TABLE, null, values1);

        //create empty row for TEMP_TABLE
        values1 = new ContentValues();
        values1.put(T_ID, 1);
        values1.put(T_TEMP_LOC, "");
        values1.put(T_TEMP_CNT, 0);
        db.insert(TEMP_TABLE, null, values1);

        //create initial data for COUNT_TABLE
        initialCounts(db);

        if (MyDebug.LOG)
            Log.d(TAG, "Success!");
    }

    // initial data for COUNT_TABLE
    private void initialCounts(SQLiteDatabase db)
    {
        String[] specs, codes, specs_g;
        specs = mContext.getResources().getStringArray(R.array.initSpecs);
        codes = mContext.getResources().getStringArray(R.array.initCodes);
        specs_g = mContext.getResources().getStringArray(R.array.initSpecs_g);

        for (int i = 1; i < specs.length; i++)
        {
            ContentValues values4 = new ContentValues();
            values4.put(C_ID, i);
            values4.put(C_NAME, specs[i]);
            values4.put(C_CODE, codes[i]);
            values4.put(C_COUNT_F1I, 0);
            values4.put(C_COUNT_F2I, 0);
            values4.put(C_COUNT_F3I, 0);
            values4.put(C_COUNT_PI, 0);
            values4.put(C_COUNT_LI, 0);
            values4.put(C_COUNT_EI, 0);
            values4.put(C_NOTES, "");
            values4.put(C_NAME_G, specs_g[i]);
            db.insert(COUNT_TABLE, null, values4);
        }
    }

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        if (oldVersion == 3)
        {
            version_4(db);
        }
        if (oldVersion == 2)
        {
            version_3(db);
            version_4(db);
        }
        if (oldVersion == 1)
        {
            version_2(db);
            version_3(db);
            version_4(db);
        }
    }

    private void version_2(SQLiteDatabase db)
    {
        String sql;

        // add new extra column icount to table individuals
        try
        {
            sql = "alter table " + INDIVIDUALS_TABLE + " add column " + I_ICOUNT + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing icount column added to individuals");
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Column already present: " + e.toString());
        }

        if (MyDebug.LOG)
            Log.d(TAG, "Upgraded database to version 2");
    }

    private void version_3(SQLiteDatabase db)
    {
        String sql;
        boolean colExist = false;
        boolean colCatExist = false;

        // add new extra columns to table counts
        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_F2I + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_f2i column added to counts!");
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Column already present: " + e.toString());
            colExist = true;
        }

        if (MyDebug.LOG)
            Log.d(TAG, "Upgraded database to version 3");

        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_F3I + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_f3i column added to counts!");
        } catch (Exception e)
        {
            //
        }

        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_PI + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_pi column added to counts!");
        } catch (Exception e)
        {
            //
        }

        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_LI + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_li column added to counts!");
        } catch (Exception e)
        {
            //
        }

        try
        {
            sql = "alter table " + COUNT_TABLE + " add column " + C_COUNT_EI + " int";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing count_ei column added to counts!");
        } catch (Exception e)
        {
            //
        }

        // add I_CATEGORY to INDIVIDUALS table
        try
        {
            sql = "alter table " + INDIVIDUALS_TABLE + " add column " + I_CATEGORY 
                + " int default 1";
            db.execSQL(sql);
            if (MyDebug.LOG)
                Log.d(TAG, "Missing icategory column added to individuals!");
        }  catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, "Column I_CATEGORY already present: " + e.toString());
            colCatExist = true;
        }

        // fill sex and icategory with default values to avoid crash when negative counting
        if (!colCatExist)
        {
            try
            {
                sql = "UPDATE " + INDIVIDUALS_TABLE + " SET " + I_SEX + " = '-'";
                db.execSQL(sql);
                if (MyDebug.LOG)
                    Log.d(TAG, "I_SEX filled with '-'");
            } catch (Exception e)
            {
                //
            }
        }
        
        // copy old data into new structure
        if (!colExist)
        {
            // rename table counts to counts_backup
            sql = "alter table " + COUNT_TABLE + " rename to counts_backup";
            db.execSQL(sql);

            // create new counts table
            sql = "create table " + COUNT_TABLE + " ("
                + C_ID + " integer primary key, "
                + C_COUNT_F1I + " int, "
                + C_COUNT_F2I + " int, "
                + C_COUNT_F3I + " int, "
                + C_COUNT_PI + " int, "
                + C_COUNT_LI + " int, "
                + C_COUNT_EI + " int, "
                + C_NAME + " text, "
                + C_CODE + " text, "
                + C_NOTES + " text)";
            db.execSQL(sql);

            // insert the old data into counts
            sql = "INSERT INTO " + COUNT_TABLE + " SELECT "
                + C_ID + ","
                + C_COUNT + ","
                + C_COUNT_F2I + ","
                + C_COUNT_F3I + ","
                + C_COUNT_PI + ","
                + C_COUNT_LI + ","
                + C_COUNT_EI + ","
                + C_NAME + ","
                + C_CODE + ","
                + C_NOTES + " FROM counts_backup";
            db.execSQL(sql);

            sql = "DROP TABLE counts_backup";
            db.execSQL(sql);

            if (MyDebug.LOG)
                Log.d(TAG, "Upgraded database to version 3");
        }
    }

    // Add column C_NAME_G
    private void version_4(SQLiteDatabase db)
    {
        String sql;
        sql = "alter table " + COUNT_TABLE + " add column " + C_NAME_G + " text";
        db.execSQL(sql);

        if (MyDebug.LOG)
            Log.d(TAG, "Upgraded database to version 4");
    }

}