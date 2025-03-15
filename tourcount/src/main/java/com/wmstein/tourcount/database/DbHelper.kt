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
 * updated to version 3 on 2018-03-31,
 * updated to version 4 on 2019-03-25,
 * updated to version 8 on 2025-02-25,
 * last edited in Java on 2022-03-24,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2025-03-15
 *
 * ************************************************************************
 * ATTENTION!
 * Current DATABASE_VERSION must be set under 'companion object' at the end
 * ************************************************************************
 */
class DbHelper (private val mContext: Context) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {

    // Called once on database creation
    override fun onCreate(db: SQLiteDatabase) {
        if (MyDebug.DLOG) Log.d(TAG, "32, Creating database: $DATABASE_NAME")

        var sql = ("create table " + SECTION_TABLE + " ("
                + S_ID + " integer primary key, "
                + S_NAME + " text, "
                + S_COUNTRY + " text, "
                + S_PLZ + " text, "
                + S_CITY + " text, "
                + S_PLACE + " text, "
                + S_TEMPE + " int, "
                + S_WIND + " int, "
                + S_CLOUDS + " int, "
                + S_TEMPE_END + " int, "
                + S_WIND_END + " int, "
                + S_CLOUDS_END + " int, "
                + S_DATE + " text, "
                + S_START_TM + " text, "
                + S_END_TM + " text, "
                + S_NOTES + " text, "
                + S_STATE + " text, "
                + S_ST_LOCALITY + " text)")
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
                + I_CATEGORY + " int, "
                + I_CODE + " text)")
        db.execSQL(sql)

        // Create empty row for SECTION_TABLE
        var values1 = ContentValues()
        values1.put(S_ID, 1)
        values1.put(S_NAME, "")
        db.insert(SECTION_TABLE, null, values1)

        // Create empty row for HEAD_TABLE
        values1 = ContentValues()
        values1.put(H_ID, 1)
        values1.put(H_OBSERVER, "")
        db.insert(HEAD_TABLE, null, values1)

        // Create empty row for TEMP_TABLE
        values1 = ContentValues()
        values1.put(T_ID, 1)
        values1.put(T_TEMP_LOC, "")
        values1.put(T_TEMP_CNT, 0)
        db.insert(TEMP_TABLE, null, values1)

        // Create initial data for COUNT_TABLE
        initialCounts(db)
        if (MyDebug.DLOG) Log.d(TAG, "121, Success!")
    }

    // Initial data for COUNT_TABLE
    private fun initialCounts(db: SQLiteDatabase) {
        val specs: Array<String> = mContext.resources.getStringArray(R.array.initSpecs)
        val codes: Array<String> = mContext.resources.getStringArray(R.array.initCodes)
        val specsG: Array<String> = mContext.resources.getStringArray(R.array.initSpecs_g)
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
            values4.put(C_NAME_G, specsG[i])
            db.insert(COUNT_TABLE, null, values4)
        }
    }

    // **********************************************************************************
    // Called with 1. call of dbHandler.getWritableDatabase() if newVersion != oldVersion
    // https://www.androidpit.de/forum/472061/sqliteopenhelper-mit-upgrade-beispielen-und-zentraler-instanz
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 7) {
            version8(db)
        }
        if (oldVersion == 6) {
            version7(db)
            version8(db)
        }
        if (oldVersion == 5) {
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 4) {
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 3) {
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 2) {
            version3(db)
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
        if (oldVersion == 1) {
            version2(db)
            version3(db)
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
        }
    }

    /*** V2 ***/
    // Version 2: New extra column icount added to INDIVIDUALS_TABLE
    private fun version2(db: SQLiteDatabase) {
        val sql: String

        // Add new extra column icount to table individuals
        sql = "alter table $INDIVIDUALS_TABLE add column $I_ICOUNT int"
        db.execSQL(sql)

        if (MyDebug.DLOG) Log.d(TAG, "203, Upgraded database to version 2")
    }

    /*** V3 ***/
    // Version 3: New extra columns for sexes and stadiums added to COUNT_TABLE
    private fun version3(db: SQLiteDatabase) {
        var sql: String
        var colExist = false
        var colCatExist = false

        // add new extra columns to table counts
        try {
            sql = "alter table $COUNT_TABLE add column $C_COUNT_F2I int"
            db.execSQL(sql)
            if (MyDebug.DLOG) Log.d(TAG, "217, Missing count_f2i column added to counts!")
        } catch (e: Exception) {
            if (MyDebug.DLOG) Log.e(TAG, "219, Column already present: $e")
            colExist = true
        }
        if (MyDebug.DLOG) Log.d(TAG, "222, Upgraded database to version 3")

        sql = "alter table $COUNT_TABLE add column $C_COUNT_F3I int"
        db.execSQL(sql)
        sql = "alter table $COUNT_TABLE add column $C_COUNT_PI int"
        db.execSQL(sql)
        sql = "alter table $COUNT_TABLE add column $C_COUNT_LI int"
        db.execSQL(sql)
        sql = "alter table $COUNT_TABLE add column $C_COUNT_EI int"
        db.execSQL(sql)

        // Add I_CATEGORY to INDIVIDUALS table
        try {
            sql = ("alter table " + INDIVIDUALS_TABLE + " add column " + I_CATEGORY
                    + " int default 1")
            db.execSQL(sql)
            if (MyDebug.DLOG) Log.d(TAG, "238, Missing icategory column added to individuals!")
        } catch (e: Exception) {
            if (MyDebug.DLOG) Log.e(TAG, "240, Column I_CATEGORY already present: $e")
            colCatExist = true
        }

        // Fill sex and icategory with default values to avoid crash when negative counting
        if (!colCatExist) {
            sql = "UPDATE $INDIVIDUALS_TABLE SET $I_SEX = '-'"
            db.execSQL(sql)
            if (MyDebug.DLOG) Log.d(TAG, "248, I_SEX filled with '-'")
        }

        // Copy old data into new structure
        if (!colExist) {
            // Rename table counts to counts_backup
            sql = "alter table $COUNT_TABLE rename to counts_backup"
            db.execSQL(sql)

            // Create new counts table
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

            // Insert the old data into counts
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
            if (MyDebug.DLOG) Log.d(TAG, "286, Upgraded database to version 3")
        }
    }

    /*** V4 ***/
    // Version 4: Add column C_NAME_G
    private fun version4(db: SQLiteDatabase) {
        val sql = "alter table $COUNT_TABLE add column $C_NAME_G text"
        db.execSQL(sql)
        if (MyDebug.DLOG) Log.d(TAG, "295, Upgraded database to version 4")
    }

    /*** V5 ***/
    // Version 5: Change TEMP_TABLE name to 'tmp' and SECTION_TABLE column S_TEMP to S_TEMPE and 'tmp'
    private fun version5(db: SQLiteDatabase) {
        // rename table temp to tmp
        var sql = "alter table 'temp' rename to 'tmp'"
        db.execSQL(sql)

        // Rename column 'temp' to 'tmp' in SECTION_TABLE
        sql = "alter table $SECTION_TABLE rename to 'section_backup'"
        db.execSQL(sql)

        // Create new sections table
        sql = ("create table " + SECTION_TABLE + " ("
                + S_ID + " integer primary key, "
                + S_NAME + " text, "
                + S_COUNTRY + " text, "
                + S_PLZ + " text, "
                + S_CITY + " text, "
                + S_PLACE + " text, "
                + S_TEMPE + " int, "
                + S_WIND + " int, "
                + S_CLOUDS + " int, "
                + S_DATE + " text, "
                + S_START_TM + " text, "
                + S_END_TM + " text, "
                + S_NOTES + " text)")
        db.execSQL(sql)

        // Insert the old data into sections
        sql = ("INSERT INTO " + SECTION_TABLE + " SELECT "
                + S_ID + ", "
                + S_NAME + ", "
                + S_COUNTRY + ", "
                + S_PLZ + ", "
                + S_CITY + ", "
                + S_PLACE + ", "
                + S_TEMP + ", "
                + S_WIND + ", "
                + S_CLOUDS + ", "
                + S_DATE + ", "
                + S_START_TM + ", "
                + S_END_TM + ", "
                + S_NOTES + " FROM section_backup")
        db.execSQL(sql)

        // Delete section_backup
        sql = "DROP TABLE section_backup"
        db.execSQL(sql)

        if (MyDebug.DLOG) Log.d(TAG, "347, Upgraded database to version 5")
    }

    /*** V6 ***/
    // Version 6: Add fields S_TEMPE_END, S_WIND_END and S_CLOUDS_END to SECTION_TABLE
    private fun version6(db: SQLiteDatabase) {
        // in SECTION_TABLE rename column 'temp' to 'tmp'
        var sql = "alter table $SECTION_TABLE rename to 'section_backup'"
        db.execSQL(sql)

        // Create new sections table
        sql = ("create table " + SECTION_TABLE + " ("
                + S_ID + " integer primary key, "
                + S_NAME + " text, "
                + S_COUNTRY + " text, "
                + S_PLZ + " text, "
                + S_CITY + " text, "
                + S_PLACE + " text, "
                + S_TEMPE + " int, "
                + S_WIND + " int, "
                + S_CLOUDS + " int, "
                + S_TEMPE_END + " int, "
                + S_WIND_END + " int, "
                + S_CLOUDS_END + " int, "
                + S_DATE + " text, "
                + S_START_TM + " text, "
                + S_END_TM + " text, "
                + S_NOTES + " text)")
        db.execSQL(sql)

        // Insert the old data into sections
        sql = ("INSERT INTO " + SECTION_TABLE + " SELECT "
                + S_ID + ", "
                + S_NAME + ", "
                + S_COUNTRY + ", "
                + S_PLZ + ", "
                + S_CITY + ", "
                + S_PLACE + ", "
                + S_TEMPE + ", "
                + S_WIND + ", "
                + S_CLOUDS + ", "
                + "0, "
                + "0, "
                + "0, "
                + S_DATE + ", "
                + S_START_TM + ", "
                + S_END_TM + ", "
                + S_NOTES + " FROM section_backup")
        db.execSQL(sql)

        // Delete section_backup
        sql = "DROP TABLE section_backup"
        db.execSQL(sql)

        if (MyDebug.DLOG) Log.d(TAG, "401, Upgraded database to version 6")
    }

    /*** V7 ***/
    // Version 7: Add field I_CODE to INDIVIDUALS_TABLE and clear count data
    private fun version7(db: SQLiteDatabase) {
        // Empty INDIVIDUALS_TABLE
        var sql = "DELETE FROM $INDIVIDUALS_TABLE"
        db.execSQL(sql)

        // Add I_CODE to INDIVIDUALS table
        sql = "alter table $INDIVIDUALS_TABLE add column $I_CODE text"
        db.execSQL(sql)

        // Empty COUNT_TABLE
        sql = "UPDATE $COUNT_TABLE SET $C_COUNT_F1I = 0, $C_COUNT_F2I = 0, $C_COUNT_F3I = 0, " +
                "$C_COUNT_PI = 0, $C_COUNT_LI = 0, $C_COUNT_EI = 0, $C_NOTES = ''"
        db.execSQL(sql)

        if (MyDebug.DLOG) Log.d(TAG, "420, Upgraded database to version 7")
    }

    /*** V8 ***/
    // Version 8: Add fields S_STATE and S_ST_LOCALITY to SECTION_TABLE
    private fun version8(db: SQLiteDatabase) {
        if (MyDebug.DLOG) Log.d(TAG, "426, Upgrade to database version 8")

        var sql = "alter table $SECTION_TABLE add column $S_STATE text"
        db.execSQL(sql)

        sql = "alter table $SECTION_TABLE add column $S_ST_LOCALITY text"
        db.execSQL(sql)

        if (MyDebug.DLOG) Log.d(TAG, "434, Upgraded database to version 8")
    }

    companion object {
        private const val DATABASE_VERSION = 8
        //DATABASE_VERSION 8: Add fields S_STATE and S_ST_LOCALITY to SECTION_TABLE
        //DATABASE_VERSION 7: Add field I_CODE to INDIVIDUALS_TABLE and clear count data
        //DATABASE_VERSION 6: Add fields S_TEMPE_END, S_WIND_END and S_CLOUDS_END to TEMP_TABLE
        //DATABASE_VERSION 5: Rename table 'temp' to 'tmp' and SECTION_TABLE column 'temp' to 'tmp'
        //DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names
        //DATABASE_VERSION 3: New extra columns for sexes and stadiums added to COUNT_TABLE
        //DATABASE_VERSION 2: New extra column icount added to INDIVIDUALS_TABLE

        private const val TAG = "DBHelper"
        private const val DATABASE_NAME = "tourcount.db"

        // tables
        const val SECTION_TABLE = "sections"
        const val COUNT_TABLE = "counts"
        const val HEAD_TABLE = "head"
        const val INDIVIDUALS_TABLE = "individuals"
        const val TEMP_TABLE = "tmp"

        // fields of table sections
        const val S_ID = "_id"
        const val S_NAME = "name"
        const val S_COUNTRY = "country"
        const val S_PLZ = "plz"
        const val S_CITY = "city"
        const val S_PLACE = "place"
        const val S_TEMPE = "tmp"
        const val S_WIND = "wind"
        const val S_CLOUDS = "clouds"
        const val S_TEMPE_END = "tmp_end"
        const val S_WIND_END = "wind_end"
        const val S_CLOUDS_END = "clouds_end"
        const val S_DATE = "date"
        const val S_START_TM = "start_tm"
        const val S_END_TM = "end_tm"
        const val S_NOTES = "notes"
        const val S_STATE = "b_state"
        const val S_ST_LOCALITY = "st_locality"

        private const val S_TEMP = "temp" // table name 'temp' has term conflict, changed tp 'tmp'

        // Fields of table counts
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
        private const val C_COUNT = "count" // eliminated in Version 3

        // Fields of table head
        const val H_ID = "_id"
        const val H_OBSERVER = "observer"

        // Fields of table individuals
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
        const val I_CODE = "code"

        // Fields of table temp
        const val T_ID = "_id"
        const val T_TEMP_LOC = "temp_loc"
        const val T_TEMP_CNT = "temp_cnt"
    }

}
