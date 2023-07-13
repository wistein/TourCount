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
 * converted to Kotlin on 2023-07-05
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

    fun saveTempLoc(temp: Temp) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.T_ID, temp.id)
        dataToInsert.put(DbHelper.T_TEMP_LOC, temp.temp_loc)
        database!!.update(DbHelper.TEMP_TABLE, dataToInsert, null, null)
    }

    /* 
    // possibly for future use
    public void saveTempCnt(int tmpcnt)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.T_ID, 1);
        dataToInsert.put(DbHelper.T_TEMP_CNT, tmpcnt);
        database.update(DbHelper.TEMP_TABLE, dataToInsert, null, null);
    }

    // possibly for future use
    public int getTempCnt()
    {
        Temp temp;
        Cursor cursor = database.query(DbHelper.TEMP_TABLE, allColumns, String.valueOf(1), null, null, null, null);
        cursor.moveToFirst();
        temp = cursorToTemp(cursor);
        cursor.close();
        return temp.temp_cnt;
    }
    */
    val temp: Temp
        get() {
            val temp: Temp
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
            temp = cursorToTemp(cursor)
            cursor.close()
            return temp
        }

    @SuppressLint("Range")
    private fun cursorToTemp(cursor: Cursor): Temp {
        val temp = Temp()
        temp.id = cursor.getInt(cursor.getColumnIndex(DbHelper.T_ID))
        temp.temp_loc = cursor.getString(cursor.getColumnIndex(DbHelper.T_TEMP_LOC))
        temp.temp_cnt = cursor.getInt(cursor.getColumnIndex(DbHelper.T_TEMP_CNT))
        return temp
    }
}