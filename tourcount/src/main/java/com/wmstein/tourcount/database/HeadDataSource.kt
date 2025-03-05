package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/**
 * Class HeadDataSource provides methods for table Head
 * Created by wmstein on 2016-03-31,
 * last edited on 2022-03-23
 */
class HeadDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.H_ID,
        DbHelper.H_OBSERVER
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

    fun saveHead(head: Head) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.H_ID, head.id)
        dataToInsert.put(DbHelper.H_OBSERVER, head.observer)
        database!!.update(DbHelper.HEAD_TABLE, dataToInsert, null, null)
    }

    @SuppressLint("Range")
    private fun cursorToHead(cursor: Cursor): Head {
        val head = Head()
        head.id = cursor.getInt(cursor.getColumnIndex(DbHelper.H_ID))
        head.observer = cursor.getString(cursor.getColumnIndex(DbHelper.H_OBSERVER))
        return head
    }

    // Make sure to close the cursor
    val head: Head
        get() {
            val head: Head
            val cursor = database!!.query(
                DbHelper.HEAD_TABLE,
                allColumns,
                1.toString(),
                null,
                null,
                null,
                null
            )
            cursor.moveToFirst()
            head = cursorToHead(cursor)
            // Make sure to close the cursor
            cursor.close()
            return head
        }

}
