package com.wmstein.tourcount.database

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Class SectionDataSource provides methods for table Section
 *
 * Created by wmstein on 2016-04-18,
 * last modified in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05,
 * last modified on 2026-10-19
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

class SectionDataSource(context: Context) {
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)
    // Database fields with initial or edited location values are:
    //   S_COUNTRY, S_PLZ, S_CITY, S_PLACE, S_STATE, S_ST_LOCALITY
    private val allColumns = arrayOf(
        DbHelper.S_ID,
        DbHelper.S_NAME,
        DbHelper.S_COUNTRY,
        DbHelper.S_PLZ,
        DbHelper.S_CITY,
        DbHelper.S_PLACE,
        DbHelper.S_TEMPE,
        DbHelper.S_WIND,
        DbHelper.S_CLOUDS,
        DbHelper.S_TEMPE_END,
        DbHelper.S_WIND_END,
        DbHelper.S_CLOUDS_END,
        DbHelper.S_DATE,
        DbHelper.S_START_TM,
        DbHelper.S_END_TM,
        DbHelper.S_NOTES,
        DbHelper.S_STATE,
        DbHelper.S_ST_LOCALITY
    )

    @Throws(SQLException::class)
    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
    }

    @SuppressLint("Range")
    private fun cursorToSection(cursor: Cursor): Section {
        val section = Section()
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID))
        section.name = cursor.getString(cursor.getColumnIndex(DbHelper.S_NAME))
        section.country = cursor.getString(cursor.getColumnIndex(DbHelper.S_COUNTRY))
        section.plz = cursor.getString(cursor.getColumnIndex(DbHelper.S_PLZ))
        section.city = cursor.getString(cursor.getColumnIndex(DbHelper.S_CITY))
        section.place = cursor.getString(cursor.getColumnIndex(DbHelper.S_PLACE))
        section.tmp = cursor.getInt(cursor.getColumnIndex(DbHelper.S_TEMPE))
        section.wind = cursor.getInt(cursor.getColumnIndex(DbHelper.S_WIND))
        section.clouds = cursor.getInt(cursor.getColumnIndex(DbHelper.S_CLOUDS))
        section.tmp_end = cursor.getInt(cursor.getColumnIndex(DbHelper.S_TEMPE_END))
        section.wind_end = cursor.getInt(cursor.getColumnIndex(DbHelper.S_WIND_END))
        section.clouds_end = cursor.getInt(cursor.getColumnIndex(DbHelper.S_CLOUDS_END))
        section.date = cursor.getString(cursor.getColumnIndex(DbHelper.S_DATE))
        section.start_tm = cursor.getString(cursor.getColumnIndex(DbHelper.S_START_TM))
        section.end_tm = cursor.getString(cursor.getColumnIndex(DbHelper.S_END_TM))
        section.notes = cursor.getString(cursor.getColumnIndex(DbHelper.S_NOTES))
        section.b_state = cursor.getString(cursor.getColumnIndex(DbHelper.S_STATE))
        section.st_locality = cursor.getString(cursor.getColumnIndex(DbHelper.S_ST_LOCALITY))
        return section
    }

    fun saveSection(section: Section) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_NAME, section.name)
        dataToInsert.put(DbHelper.S_COUNTRY, section.country)
        dataToInsert.put(DbHelper.S_PLZ, section.plz)
        dataToInsert.put(DbHelper.S_CITY, section.city)
        dataToInsert.put(DbHelper.S_PLACE, section.place)
        dataToInsert.put(DbHelper.S_TEMPE, section.tmp)
        dataToInsert.put(DbHelper.S_WIND, section.wind)
        dataToInsert.put(DbHelper.S_CLOUDS, section.clouds)
        dataToInsert.put(DbHelper.S_TEMPE_END, section.tmp_end)
        dataToInsert.put(DbHelper.S_WIND_END, section.wind_end)
        dataToInsert.put(DbHelper.S_CLOUDS_END, section.clouds_end)
        dataToInsert.put(DbHelper.S_DATE, section.date)
        dataToInsert.put(DbHelper.S_START_TM, section.start_tm)
        dataToInsert.put(DbHelper.S_END_TM, section.end_tm)
        dataToInsert.put(DbHelper.S_NOTES, section.notes)
        dataToInsert.put(DbHelper.S_STATE, section.b_state)
        dataToInsert.put(DbHelper.S_ST_LOCALITY, section.st_locality)
        val where = DbHelper.S_ID + " = ?"
        val whereArgs = arrayOf("1")
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    // Called from WelcomeActivity, CountingActivity and EditMetaActivity
    val section: Section
        get() {
            val section: Section
            val cursor = database!!.query(
                DbHelper.SECTION_TABLE,
                allColumns,
                DbHelper.S_ID + " = 1",
                null, null, null, null
            )
            cursor.moveToFirst()
            section = cursorToSection(cursor)
            cursor.close()
            return section
        }

    // Store only when field is empty
    fun storeEmptyCountry(name: String) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_COUNTRY, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_COUNTRY + " IS NULL OR " + DbHelper.S_COUNTRY + " == '')"
        val whereArgs = arrayOf("1") // ID is always "1"
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun storeEmptyState(name: String) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_STATE, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_STATE + " IS NULL OR " + DbHelper.S_STATE + " == '')"
        val whereArgs = arrayOf("1")
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun storeEmptyPlz(name: String) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_PLZ, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_PLZ + " IS NULL OR " + DbHelper.S_PLZ + " == '')"
        val whereArgs = arrayOf("1")
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun storeEmptyCity(name: String) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_CITY, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_CITY + " IS NULL OR " + DbHelper.S_CITY + " == '')"
        val whereArgs = arrayOf("1")
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun storeEmptyPlace(name: String) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_PLACE, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_PLACE + " IS NULL OR " + DbHelper.S_PLACE + " == '')"
        val whereArgs = arrayOf("1")
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun storeEmptyStLocality(name: String) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_ST_LOCALITY, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_ST_LOCALITY + " IS NULL OR " + DbHelper.S_ST_LOCALITY + " == '')"
        val whereArgs = arrayOf("1")
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

}
