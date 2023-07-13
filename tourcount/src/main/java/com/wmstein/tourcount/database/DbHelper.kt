package com.wmstein.tourcount.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.wmstein.tourcount.MyDebug
import com.wmstein.tourcount.R

/**************************************************
 * Basic structure by milo on 05/05/2014.
 * Created by wmstein on 2016-04-19,
 * updated to version 2 on 2017-09-09,
 * updated to version 3 on 2018-03-31
 * updated to version 4 on 2019-03-25
 * last edited in Java on 2022-03-24
 * converted to Kotlin on 2023-07-06
 */
class DbHelper    // constructor
    (private val mContext: Context?) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
    // called once on database creation
    override fun onCreate(db: SQLiteDatabase) {
        if (MyDebug.LOG) Log.i(TAG, "Creating database: $DATABASE_NAME")
        var sql = ("create table " + SECTION_TABLE + " ("
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
                + S_NOTES + " text)")
        db.execSQL(sql)
        sql = ("create table " + COUNT_TABLE + " ("
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
                + C_NAME_G + " text)")
        db.execSQL(sql)
        sql = ("create table " + HEAD_TABLE + " ("
                + H_ID + " integer primary key, "
                + H_OBSERVER + " text)")
        db.execSQL(sql)
        sql = ("create table " + TEMP_TABLE + " ("
                + T_ID + " integer primary key, "
                + T_TEMP_LOC + " text, "
                + T_TEMP_CNT + " int)")
        db.execSQL(sql)
        sql = ("create table " + INDIVIDUALS_TABLE + " ("
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
                + I_CATEGORY + " int)")
        db.execSQL(sql)

        //create empty row for SECTION_TABLE
        var values1 = ContentValues()
        values1.put(S_ID, 1)
        values1.put(S_NAME, "")
        db.insert(SECTION_TABLE, null, values1)

        //create empty row for HEAD_TABLE
        values1 = ContentValues()
        values1.put(H_ID, 1)
        values1.put(H_OBSERVER, "")
        db.insert(HEAD_TABLE, null, values1)

        //create empty row for TEMP_TABLE
        values1 = ContentValues()
        values1.put(T_ID, 1)
        values1.put(T_TEMP_LOC, "")
        values1.put(T_TEMP_CNT, 0)
        db.insert(TEMP_TABLE, null, values1)

        //create initial data for COUNT_TABLE
        initialCounts(db)
        if (MyDebug.LOG) Log.d(TAG, "Success!")
    }

    // initial data for COUNT_TABLE
    private fun initialCounts(db: SQLiteDatabase) {
        val specs: Array<String> = mContext?.resources!!.getStringArray(R.array.initSpecs)
        val codes: Array<String> = mContext.resources.getStringArray(R.array.initCodes)
        val specs_g: Array<String> = mContext.resources.getStringArray(R.array.initSpecs_g)
        for (i in 1 until specs.size) {
            val values4 = ContentValues()
            values4.put(C_ID, i)
            values4.put(C_NAME, specs[i])
            values4.put(C_CODE, codes[i])
            values4.put(C_COUNT_F1I, 0)
            values4.put(C_COUNT_F2I, 0)
            values4.put(C_COUNT_F3I, 0)
            values4.put(C_COUNT_PI, 0)
            values4.put(C_COUNT_LI, 0)
            values4.put(C_COUNT_EI, 0)
            values4.put(C_NOTES, "")
            values4.put(C_NAME_G, specs_g[i])
            db.insert(COUNT_TABLE, null, values4)
        }
    }

    // ******************************************************************************************
    // called if newVersion != oldVersion
    // https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 3) {
            version_4(db)
        }
        if (oldVersion == 2) {
            version_3(db)
            version_4(db)
        }
        if (oldVersion == 1) {
            version_2(db)
            version_3(db)
            version_4(db)
        }
    }

    private fun version_2(db: SQLiteDatabase) {
        val sql: String

        // add new extra column icount to table individuals
        try {
            sql = "alter table $INDIVIDUALS_TABLE add column $I_ICOUNT int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing icount column added to individuals")
        } catch (e: Exception) {
            if (MyDebug.LOG) Log.e(TAG, "Column already present: $e")
        }
        if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 2")
    }

    private fun version_3(db: SQLiteDatabase) {
        var sql: String
        var colExist = false
        var colCatExist = false

        // add new extra columns to table counts
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2I int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_f2i column added to counts!")
        } catch (e: Exception) {
            if (MyDebug.LOG) Log.e(TAG, "Column already present: $e")
            colExist = true
        }
        if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 3")
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F3I int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_f3i column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_PI int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_pi column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_LI int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_li column added to counts!")
        } catch (e: Exception) {
            //
        }
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_EI int"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing count_ei column added to counts!")
        } catch (e: Exception) {
            //
        }

        // add I_CATEGORY to INDIVIDUALS table
        try {
            sql = ("alter table " + INDIVIDUALS_TABLE + " add column " + I_CATEGORY
                    + " int default 1")
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Missing icategory column added to individuals!")
        } catch (e: Exception) {
            if (MyDebug.LOG) Log.e(TAG, "Column I_CATEGORY already present: $e")
            colCatExist = true
        }

        // fill sex and icategory with default values to avoid crash when negative counting
        if (!colCatExist) {
            try {
                sql = "UPDATE $INDIVIDUALS_TABLE SET $I_SEX = '-'"
                db.execSQL(sql)
                if (MyDebug.LOG) Log.d(TAG, "I_SEX filled with '-'")
            } catch (e: Exception) {
                //
            }
        }

        // copy old data into new structure
        if (!colExist) {
            // rename table counts to counts_backup
            sql = "alter table $COUNT_TABLE rename to counts_backup"
            db.execSQL(sql)

            // create new counts table
            sql = ("create table " + COUNT_TABLE + " ("
                    + C_ID + " integer primary key, "
                    + C_COUNT_F1I + " int, "
                    + C_COUNT_F2I + " int, "
                    + C_COUNT_F3I + " int, "
                    + C_COUNT_PI + " int, "
                    + C_COUNT_LI + " int, "
                    + C_COUNT_EI + " int, "
                    + C_NAME + " text, "
                    + C_CODE + " text, "
                    + C_NOTES + " text)")
            db.execSQL(sql)

            // insert the old data into counts
            sql = ("INSERT INTO " + COUNT_TABLE + " SELECT "
                    + C_ID + ","
                    + C_COUNT + ","
                    + C_COUNT_F2I + ","
                    + C_COUNT_F3I + ","
                    + C_COUNT_PI + ","
                    + C_COUNT_LI + ","
                    + C_COUNT_EI + ","
                    + C_NAME + ","
                    + C_CODE + ","
                    + C_NOTES + " FROM counts_backup")
            db.execSQL(sql)
            sql = "DROP TABLE counts_backup"
            db.execSQL(sql)
            if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 3")
        }
    }

    // Add column C_NAME_G
    private fun version_4(db: SQLiteDatabase) {
        val sql = "alter table $COUNT_TABLE add column $C_NAME_G text"
        db.execSQL(sql)
        if (MyDebug.LOG) Log.d(TAG, "Upgraded database to version 4")
    }

    companion object {
        private const val TAG = "TourCount DBHelper"
        private const val DATABASE_NAME = "tourcount.db"

        //DATABASE_VERSION 2: New extra column icount added to INDIVIDUALS_TABLE
        //DATABASE_VERSION 3: New extra columns for sexus and stadiums added to COUNT_TABLE
        //DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names 
        private const val DATABASE_VERSION = 4

        // tables
        const val SECTION_TABLE = "sections"
        const val COUNT_TABLE = "counts"
        const val HEAD_TABLE = "head"
        const val INDIVIDUALS_TABLE = "individuals"
        const val TEMP_TABLE = "temp"

        // fields
        const val S_ID = "_id"
        const val S_NAME = "name"
        const val S_COUNTRY = "country"
        const val S_PLZ = "plz"
        const val S_CITY = "city"
        const val S_PLACE = "place"
        const val S_TEMP = "temp"
        const val S_WIND = "wind"
        const val S_CLOUDS = "clouds"
        const val S_DATE = "date"
        const val S_START_TM = "start_tm"
        const val S_END_TM = "end_tm"
        const val S_NOTES = "notes"
        const val C_ID = "_id"
        const val C_COUNT_F1I = "count_f1i"
        const val C_COUNT_F2I = "count_f2i"
        const val C_COUNT_F3I = "count_f3i"
        const val C_COUNT_PI = "count_pi"
        const val C_COUNT_LI = "count_li"
        const val C_COUNT_EI = "count_ei"
        const val C_NAME = "name"
        const val C_CODE = "code"
        const val C_NOTES = "notes"
        const val C_NAME_G = "name_g"
        private const val C_COUNT = "count" //deprecated
        const val H_ID = "_id"
        const val H_OBSERVER = "observer"
        const val I_ID = "_id"
        const val I_COUNT_ID = "count_id"
        const val I_NAME = "name"
        const val I_COORD_X = "coord_x"
        const val I_COORD_Y = "coord_y"
        const val I_COORD_Z = "coord_z"
        const val I_UNCERT = "uncert"
        const val I_DATE_STAMP = "date_stamp"
        const val I_TIME_STAMP = "time_stamp"
        const val I_LOCALITY = "locality"
        const val I_SEX = "sex"
        const val I_STADIUM = "stadium"
        const val I_STATE_1_6 = "state_1_6"
        const val I_NOTES = "notes"
        const val I_ICOUNT = "icount"
        const val I_CATEGORY = "icategory"
        const val T_ID = "_id"
        const val T_TEMP_LOC = "temp_loc"
        const val T_TEMP_CNT = "temp_cnt"
    }
}