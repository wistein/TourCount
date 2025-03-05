package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/********************************************************************
 * Class IndividualsDataSource provides methods for table Individuals
 * Created by wmstein for TourCount on 2016-04-20,
 * last edited in Java on 2022-03-24,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-10-21
 */
class IndividualsDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.I_ID,
        DbHelper.I_COUNT_ID,  // ID of table count
        DbHelper.I_NAME,      // species name
        DbHelper.I_COORD_X,   // latitude
        DbHelper.I_COORD_Y,   // longitude
        DbHelper.I_COORD_Z,   // height
        DbHelper.I_UNCERT,    // uncertainty
        DbHelper.I_DATE_STAMP,  // date
        DbHelper.I_TIME_STAMP,  // time
        DbHelper.I_LOCALITY,  // locality
        DbHelper.I_SEX,       // sexus
        DbHelper.I_STADIUM,   // stadium
        DbHelper.I_STATE_1_6, // b_state
        DbHelper.I_NOTES,     // notes
        DbHelper.I_ICOUNT,    // individual count
        DbHelper.I_CATEGORY,  // category (1-6)
        DbHelper.I_CODE       // code
    )

    init {
        dbHandler = DbHelper(context)
    }

    @Throws(SQLException::class)
    fun open() {
        database = dbHandler.writableDatabase
    }

    fun close() {
        dbHandler.close()
    }

    fun createIndividuals(
        countId: Int,
        name: String?,
        latitude: Double,
        longitude: Double,
        height: Double,
        uncert: String?,
        datestamp: String?,
        timestamp: String?,
        locality: String?,
        code: String?
    ): Individuals? {
        return if (database!!.isOpen) // prohibits crash when doubleclicking count button
        {
            val values = ContentValues()
            values.put(DbHelper.I_COUNT_ID, countId)
            values.put(DbHelper.I_NAME, name)
            values.put(DbHelper.I_COORD_X, latitude)
            values.put(DbHelper.I_COORD_Y, longitude)
            values.put(DbHelper.I_COORD_Z, height)
            values.put(DbHelper.I_UNCERT, uncert)
            values.put(DbHelper.I_DATE_STAMP, datestamp)
            values.put(DbHelper.I_TIME_STAMP, timestamp)
            values.put(DbHelper.I_LOCALITY, locality)
            values.put(DbHelper.I_SEX, "")
            values.put(DbHelper.I_STADIUM, "")
            values.put(DbHelper.I_STATE_1_6, 0)
            values.put(DbHelper.I_NOTES, "")
            values.put(DbHelper.I_ICOUNT, 0)
            values.put(DbHelper.I_CATEGORY, 0)
            values.put(DbHelper.I_CODE, code)
            val insertId = database!!.insert(DbHelper.INDIVIDUALS_TABLE, null, values).toInt()
            val cursor = database!!.query(
                DbHelper.INDIVIDUALS_TABLE,
                allColumns, DbHelper.I_ID + " = " + insertId, null, null, null, null
            )
            cursor.moveToFirst()
            val newIndividuals = cursorToIndividuals(cursor)
            cursor.close()
            newIndividuals
        } else {
            null
        }
    }

    fun deleteIndividualById(id: Int) {
        database!!.delete(DbHelper.INDIVIDUALS_TABLE, DbHelper.I_ID + " = " + id, null)
    }

    fun deleteIndividualsByCode(cd: String) {
        database!!.delete(DbHelper.INDIVIDUALS_TABLE, DbHelper.I_CODE + " = '$cd'", null)
    }

    fun decreaseIndividual(id: Int, newicount: Int) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.I_ICOUNT, newicount)
        val where = DbHelper.I_ID + " = ?"
        val whereArgs = arrayOf(id.toString())
        database!!.update(DbHelper.INDIVIDUALS_TABLE, dataToInsert, where, whereArgs)
    }

    @SuppressLint("Range")
    private fun cursorToIndividuals(cursor: Cursor): Individuals {
        val newindividuals = Individuals()
        newindividuals.id = cursor.getInt(cursor.getColumnIndex(DbHelper.I_ID))
        newindividuals.count_id = cursor.getInt(cursor.getColumnIndex(DbHelper.I_COUNT_ID))
        newindividuals.name = cursor.getString(cursor.getColumnIndex(DbHelper.I_NAME))
        newindividuals.coord_x = cursor.getDouble(cursor.getColumnIndex(DbHelper.I_COORD_X))
        newindividuals.coord_y = cursor.getDouble(cursor.getColumnIndex(DbHelper.I_COORD_Y))
        newindividuals.coord_z = cursor.getDouble(cursor.getColumnIndex(DbHelper.I_COORD_Z))
        newindividuals.uncert = cursor.getString(cursor.getColumnIndex(DbHelper.I_UNCERT))
        newindividuals.date_stamp = cursor.getString(cursor.getColumnIndex(DbHelper.I_DATE_STAMP))
        newindividuals.time_stamp = cursor.getString(cursor.getColumnIndex(DbHelper.I_TIME_STAMP))
        newindividuals.locality = cursor.getString(cursor.getColumnIndex(DbHelper.I_LOCALITY))
        newindividuals.sex = cursor.getString(cursor.getColumnIndex(DbHelper.I_SEX))
        newindividuals.stadium = cursor.getString(cursor.getColumnIndex(DbHelper.I_STADIUM))
        newindividuals.state_1_6 = cursor.getInt(cursor.getColumnIndex(DbHelper.I_STATE_1_6))
        newindividuals.notes = cursor.getString(cursor.getColumnIndex(DbHelper.I_NOTES))
        newindividuals.icount = cursor.getInt(cursor.getColumnIndex(DbHelper.I_ICOUNT))
        newindividuals.icategory = cursor.getInt(cursor.getColumnIndex(DbHelper.I_CATEGORY))
        newindividuals.code = cursor.getString(cursor.getColumnIndex(DbHelper.I_CODE))
        return newindividuals
    }

    fun saveIndividual(individuals: Individuals?): Int {
        return if (individuals != null) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.I_COUNT_ID, individuals.count_id)
            dataToInsert.put(DbHelper.I_NAME, individuals.name)
            dataToInsert.put(DbHelper.I_COORD_X, individuals.coord_x)
            dataToInsert.put(DbHelper.I_COORD_Y, individuals.coord_y)
            dataToInsert.put(DbHelper.I_COORD_Z, individuals.coord_z)
            dataToInsert.put(DbHelper.I_UNCERT, individuals.uncert)
            dataToInsert.put(DbHelper.I_DATE_STAMP, individuals.date_stamp)
            dataToInsert.put(DbHelper.I_TIME_STAMP, individuals.time_stamp)
            dataToInsert.put(DbHelper.I_LOCALITY, individuals.locality)
            dataToInsert.put(DbHelper.I_SEX, individuals.sex)
            dataToInsert.put(DbHelper.I_STADIUM, individuals.stadium)
            dataToInsert.put(DbHelper.I_STATE_1_6, individuals.state_1_6)
            dataToInsert.put(DbHelper.I_NOTES, individuals.notes)
            dataToInsert.put(DbHelper.I_ICOUNT, individuals.icount)
            dataToInsert.put(DbHelper.I_CATEGORY, individuals.icategory)
            dataToInsert.put(DbHelper.I_CODE, individuals.code)
            val where = DbHelper.I_ID + " = ?"
            val whereArgs = arrayOf(individuals.id.toString())
            database!!.update(DbHelper.INDIVIDUALS_TABLE, dataToInsert, where, whereArgs)
            individuals.id
        } else {
            0 // in case of doubleclick on count button
        }
    }

    // get last individual of category of species
    fun getLastIndiv(c_Id: Int, categ: Int): Int {
        val individuals: Individuals
        val c_IdStr = c_Id.toString()
        val categStr = categ.toString()
        val cursor = database!!.rawQuery(
            "select * from " + DbHelper.INDIVIDUALS_TABLE
                    + " WHERE (" + DbHelper.I_COUNT_ID + " = " + c_IdStr + " AND "
                    + DbHelper.I_CATEGORY + " = " + categStr + ")", null, null
        )
        cursor.moveToLast()

        // check for entries in individuals table, which are not there when bulk counts are entered
        return if (!cursor.isAfterLast) {
            individuals = cursorToIndividuals(cursor)
            cursor.close()
            individuals.id
        } else {
            cursor.close()
            -1
        }
    }

    fun getIndividual(indiv_id: Int): Individuals {
        val individuals: Individuals
        val cursor = database!!.query(
            DbHelper.INDIVIDUALS_TABLE,
            allColumns,
            DbHelper.I_ID + " = ?",
            arrayOf(indiv_id.toString()),
            null,
            null,
            null
        )
        cursor.moveToFirst()
        individuals = cursorToIndividuals(cursor)
        cursor.close()
        return individuals
    }

    fun getIndividualCount(indiv_id: Int): Int {
        val individuals: Individuals
        val cursor = database!!.query(
            DbHelper.INDIVIDUALS_TABLE, allColumns, DbHelper.I_ID
                    + " = ?", arrayOf(indiv_id.toString()), null, null, null
        )
        cursor.moveToFirst()
        individuals = cursorToIndividuals(cursor)
        cursor.close()
        return individuals.icount
    }

    // Used by ShowResultsActivity
    fun getIndividualsByName(iname: String): List<Individuals> {
        val indivs: MutableList<Individuals> = ArrayList()
        val slct =
            "select * from " + DbHelper.INDIVIDUALS_TABLE + " WHERE " + DbHelper.I_NAME + " = ?"
        val cursor = database!!.rawQuery(slct, arrayOf(iname))
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val individuals = cursorToIndividuals(cursor)
            indivs.add(individuals)
            cursor.moveToNext()
        }
        cursor.close()
        return indivs
    }

    val individuals: List<Individuals>
        get() {
            val individs: MutableList<Individuals> = ArrayList()
            val slct =
                "select * from " + DbHelper.INDIVIDUALS_TABLE + " WHERE " + DbHelper.I_COORD_X + " != 0"
            val cursor = database!!.rawQuery(slct, null)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val individuals = cursorToIndividuals(cursor)
                individs.add(individuals)
                cursor.moveToNext()
            }
            cursor.close()
            return individs
        }

}
