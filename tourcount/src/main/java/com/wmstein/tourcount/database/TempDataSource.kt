package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/******************************************************
 * Class TempDataSource provides methods for table Temp
 * Created by wmstein on 2016-05-15,
 * last edited in Java on 2023-05-13,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-05-14.
 */
class TempDataSource(context: Context?) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.T_ID,
        DbHelper.T_TEMP_LOC,
        DbHelper.T_TEMP_CNT
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

    fun saveTempLoc(tmp: Temp) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.T_ID, tmp.id)
        dataToInsert.put(DbHelper.T_TEMP_LOC, tmp.temp_loc)
        database!!.update(DbHelper.TEMP_TABLE, dataToInsert, null, null)
    }

    val tmp: Temp
        get() {
            val tmp: Temp
            val cursor = database!!.query(
                DbHelper.TEMP_TABLE,
                allColumns,
                1.toString(),
                null,
                null,
                null,
                null
            )
            cursor.moveToFirst()
            tmp = cursorToTemp(cursor)
            cursor.close()
            return tmp
        }

    @SuppressLint("Range")
    private fun cursorToTemp(cursor: Cursor): Temp {
        val tmp = Temp()
        tmp.id = cursor.getInt(cursor.getColumnIndex(DbHelper.T_ID))
        tmp.temp_loc = cursor.getString(cursor.getColumnIndex(DbHelper.T_TEMP_LOC))
        tmp.temp_cnt = cursor.getInt(cursor.getColumnIndex(DbHelper.T_TEMP_CNT))
        return tmp
    }

}
