package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

/******************************************************
 * Class HeadDataSource provides methods for table Head
 *
 * Created by wmstein on 2016-03-31.
 * Last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-03-20
 */
class HeadDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)
    private val allColumns = arrayOf(
        DbHelper.H_ID,
        DbHelper.H_OBSERVER,
    )

    @Throws(SQLException::class)
    fun open() {
        database = dbHelper.writableDatabase
    }

    fun close() {
        dbHelper.close()
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

    // getHead()
    val head: Head
        get() {
            val head: Head
            val cursor = database!!.query(
                DbHelper.HEAD_TABLE,
                allColumns,
                DbHelper.H_ID + " = 1",
                null,
                null,
                null,
                null
            )
            cursor.moveToFirst()
            head = cursorToHead(cursor)
            cursor.close()
            return head
        }

}
