package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/************************************************************
 * Class SectionDataSource provides methods for table Section
 * Created by wmstein on 2016-04-18,
 * last modified in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05
 * last modified on 2023-12-15,
 */
class SectionDataSource(context: Context?) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
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
        DbHelper.S_DATE,
        DbHelper.S_START_TM,
        DbHelper.S_END_TM,
        DbHelper.S_NOTES
    )

    init {
        dbHandler = DbHelper(context!!)
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun close() {
        dbHandler.close()
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
        section.date = cursor.getString(cursor.getColumnIndex(DbHelper.S_DATE))
        section.start_tm = cursor.getString(cursor.getColumnIndex(DbHelper.S_START_TM))
        section.end_tm = cursor.getString(cursor.getColumnIndex(DbHelper.S_END_TM))
        section.notes = cursor.getString(cursor.getColumnIndex(DbHelper.S_NOTES))
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
        dataToInsert.put(DbHelper.S_DATE, section.date)
        dataToInsert.put(DbHelper.S_START_TM, section.start_tm)
        dataToInsert.put(DbHelper.S_END_TM, section.end_tm)
        dataToInsert.put(DbHelper.S_NOTES, section.notes)
        val where = DbHelper.S_ID + " = ?"
        val whereArgs = arrayOf(section.id.toString())
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }// Make sure to close the cursor

    // called from RetrieveAddr, CountingActivity and EditSpecListActivity
    val section: Section
        get() {
            val section: Section
            val cursor = database!!.query(
                DbHelper.SECTION_TABLE,
                allColumns,
                DbHelper.S_ID + " = 1",
                null,
                null,
                null,
                null
            )
            cursor.moveToFirst()
            section = cursorToSection(cursor)
            // Make sure to close the cursor
            cursor.close()
            return section
        }

    // called from CountingActivity
    // store only when field is empty
    fun updateEmptyCountry(id: Int, name: String?) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_COUNTRY, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_COUNTRY + " IS NULL OR " + DbHelper.S_COUNTRY + " == '')"
        val whereArgs = arrayOf(id.toString())
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun updateEmptyPlz(id: Int, name: String?) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_PLZ, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_PLZ + " IS NULL OR " + DbHelper.S_PLZ + " == '')"
        val whereArgs = arrayOf(id.toString())
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun updateEmptyCity(id: Int, name: String?) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_CITY, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_CITY + " IS NULL OR " + DbHelper.S_CITY + " == '')"
        val whereArgs = arrayOf(id.toString())
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }

    fun updateEmptyPlace(id: Int, name: String?) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.S_PLACE, name)
        val where =
            DbHelper.S_ID + " = ? AND (" + DbHelper.S_PLACE + " IS NULL OR " + DbHelper.S_PLACE + " == '')"
        val whereArgs = arrayOf(id.toString())
        database!!.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs)
    }
}