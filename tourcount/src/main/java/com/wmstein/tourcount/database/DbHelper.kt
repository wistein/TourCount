package com.wmstein.tourcount.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.wmstein.tourcount.BuildConfig
import com.wmstein.tourcount.IsRunningOnEmulator
import com.wmstein.tourcount.R
import java.util.Locale

/********************************************************************************
 * DbHelper.kt is the database helper class for SQLite functionality of TourCount
 * onUpgrade is called with 1. call of dbHelper.getWritableDatabase()
 *   if newVersion != oldVersion
 *
 * Basic structure of DbHelper.java by milo on 05/05/2014.
 * Adopted for TourCount by wmstein on 2016-04-19,
 * last edited in Java on 2022-03-24,
 * converted to Kotlin on 2023-07-06,
 * updated to version 8 on 2025-02-25,
 * last edited on 2026-04-06
 *
 * ************************************************************************
 * ATTENTION!
 * Current DATABASE_VERSION must be set under 'companion object' at the end
 * ************************************************************************
 */
class DbHelper (private val mContext: Context) :
    SQLiteOpenHelper(mContext, DATABASE_NAME, null, DATABASE_VERSION) {
    // initDataLanguage = current system language
    private var initDataLanguage = Locale.getDefault().toString().substring(0, 2)

    // Called once on database creation
    override fun onCreate(db: SQLiteDatabase) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "38, Creating database: $DATABASE_NAME")

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
                + H_OBSERVER + " text, "
                + H_DATALANGUAGE + " text)")
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

        // Create SECTION_TABLE with 1 empty row
        var values1 = ContentValues()
        values1.put(S_ID, 1)
        values1.put(S_NAME, "")
        db.insert(SECTION_TABLE, null, values1)

        // Create HEAD_TABLE with 1 empty row
        values1 = ContentValues()
        values1.put(H_ID, 1)
        values1.put(H_OBSERVER, "")
        values1.put(H_DATALANGUAGE, "")
        db.insert(HEAD_TABLE, null, values1)

        // Create TEMP_TABLE with 1 empty row
        values1 = ContentValues()
        values1.put(T_ID, 1)
        values1.put(T_TEMP_LOC, "")
        values1.put(T_TEMP_CNT, 0)
        db.insert(TEMP_TABLE, null, values1)

        // Create initial data for COUNT_TABLE
        initialCounts(db)

        // Create initial data for HEAD_TABLE
        initialHead(db)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "134, Success!")
    }

    // Initial data for COUNT_TABLE
    private fun initialCounts(db: SQLiteDatabase) {
        val specs: Array<String> = mContext.resources.getStringArray(R.array.initSpecs)
        val codes: Array<String> = mContext.resources.getStringArray(R.array.initCodes)
        // Initial local species name entries comprise initial species in the current system language
        val specsL: Array<String> = when (initDataLanguage) {
            "en" -> mContext.resources.getStringArray(R.array.initSpecs_en)
            "fr" -> mContext.resources.getStringArray(R.array.initSpecs_fr)
            "it" -> mContext.resources.getStringArray(R.array.initSpecs_it)
            "es" -> mContext.resources.getStringArray(R.array.initSpecs_es)
            else -> mContext.resources.getStringArray(R.array.initSpecs_de)
        }

        for (i in 0 until specs.size) { // changed i in 1 -> i in 0
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
            values4.put(C_NAME_G, specsL[i])
            db.insert(COUNT_TABLE, null, values4)
        }
    }

    private fun initialHead(db: SQLiteDatabase) {
        // Enter current system language as initial data language
        val sql = "UPDATE $HEAD_TABLE SET $H_DATALANGUAGE = '$initDataLanguage'"
        db.execSQL(sql)
    }
    
    // *********************************************************************************
    // Called with 1. call of dbHelper.getWritableDatabase() if newVersion != oldVersion
    //   and if a database already exists on disk with the same DATABASE_NAME.
    // see https://guides.codepath.org/android/local-databases-with-sqliteopenhelper
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "179, upGrade")

        if (oldVersion == 8) {
            version9(db)
        }
        if (oldVersion == 7) {
            version8(db)
            version9(db)
        }
        if (oldVersion == 6) {
            version7(db)
            version8(db)
            version9(db)
        }
        if (oldVersion == 5) {
            version6(db)
            version7(db)
            version8(db)
            version9(db)
        }
        if (oldVersion == 4) {
            version5(db)
            version6(db)
            version7(db)
            version8(db)
            version9(db)
        }
        if (oldVersion == 3) {
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
            version9(db)
        }
        if (oldVersion == 2) {
            version3(db)
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
            version9(db)
        }
        if (oldVersion == 1) {
            version2(db)
            version3(db)
            version4(db)
            version5(db)
            version6(db)
            version7(db)
            version8(db)
            version9(db)
        }
    }

    /*** V2 ***/
    // Version 2: New extra column icount added to INDIVIDUALS_TABLE
    private fun version2(db: SQLiteDatabase) {

        // Add new extra column icount to table individuals
        val sql = "alter table $INDIVIDUALS_TABLE add column $I_ICOUNT int"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "244, Upgraded database to version 2")
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
        } catch (_: Exception) {
            colExist = true
        }

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
        } catch (_: Exception) {
            colCatExist = true
        }

        // Fill sex and icategory with default values to avoid crash when negative counting
        if (!colCatExist) {
            sql = "UPDATE $INDIVIDUALS_TABLE SET $I_SEX = '-'"
            db.execSQL(sql)
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

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "323, Upgraded database to version 3")
        }
    }

    /*** V4 ***/
    // Version 4: Add column C_NAME_G
    private fun version4(db: SQLiteDatabase) {
        val sql = "alter table $COUNT_TABLE add column $C_NAME_G text"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "334, Upgraded database to version 4")
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

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "387, Upgraded database to version 5")
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

        // Delete table section_backup
        sql = "DROP TABLE section_backup"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "442, Upgraded database to version 6")
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

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "462, Upgraded database to version 7")
    }

    /*** V8 ***/
    // Version 8: Add fields S_STATE and S_ST_LOCALITY to SECTION_TABLE
    private fun version8(db: SQLiteDatabase) {
        var sql = "alter table $SECTION_TABLE add column $S_STATE text"
        db.execSQL(sql)

        sql = "alter table $SECTION_TABLE add column $S_ST_LOCALITY text"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "475, Upgraded database to version 8")
    }

    /*** V9 ***/
    // Version 9: New extra column datalanguage added to HEAD_TABLE
    private fun version9(db: SQLiteDatabase) {
        // Add new extra column datalanguage to table head
        var sql = "alter table head add column datalanguage text"
        db.execSQL(sql)

        // Enter empty dataLanguage
        sql = "UPDATE head SET datalanguage = ''"
        db.execSQL(sql)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "490, Upgraded database to version 9")
    }

    companion object {
        private const val DATABASE_VERSION = 9
        //DATABASE_VERSION 9: Add field H_DATALANGUAGE to HEAD_TABLE
        //DATABASE_VERSION 8: Add fields S_STATE and S_ST_LOCALITY to SECTION_TABLE
        //DATABASE_VERSION 7: Add field I_CODE to INDIVIDUALS_TABLE and clear count data
        //DATABASE_VERSION 6: Add fields S_TEMPE_END, S_WIND_END and S_CLOUDS_END to TEMP_TABLE
        //DATABASE_VERSION 5: Rename table 'temp' to 'tmp' and SECTION_TABLE column 'temp' to 'tmp'
        //DATABASE_VERSION 4: Column C_NAME_G added to COUNT_TABLE for local butterfly names
        //DATABASE_VERSION 3: New extra columns for sexes and stadiums added to COUNT_TABLE
        //DATABASE_VERSION 2: New extra column icount added to INDIVIDUALS_TABLE

        private const val TAG = "DbHelper"
        private const val DATABASE_NAME = "tourcount.db"

        // tables
        const val HEAD_TABLE = "head"
        const val SECTION_TABLE = "sections"
        const val COUNT_TABLE = "counts"
        const val INDIVIDUALS_TABLE = "individuals"
        const val TEMP_TABLE = "tmp"

        // Fields of table head
        const val H_ID = "_id"
        const val H_OBSERVER = "observer"
        const val H_DATALANGUAGE = "datalanguage"

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
