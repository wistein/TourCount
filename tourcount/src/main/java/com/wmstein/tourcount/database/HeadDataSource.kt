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
 * Class HeadDataSource provides methods for table Head
 *
 * Created by wmstein on 2016-03-31.
 * Last edited in Java on 2022-04-26,
 * converted to Kotlin on 2023-06-26,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase

class HeadDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHelper: DbHelper = DbHelper(context)
    private val allColumns = arrayOf(
        DbHelper.H_ID,
        DbHelper.H_OBSERVER,
        DbHelper.H_DATALANGUAGE
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
        dataToInsert.put(DbHelper.H_DATALANGUAGE, head.datalanguage)
        database!!.update(DbHelper.HEAD_TABLE, dataToInsert, null, null)
    }

    // getHead()
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
            cursor.close()
            return head
        }

    @SuppressLint("Range")
    private fun cursorToHead(cursor: Cursor): Head {
        val head = Head()
        head.id = cursor.getInt(cursor.getColumnIndex(DbHelper.H_ID))
        head.observer = cursor.getString(cursor.getColumnIndex(DbHelper.H_OBSERVER))
        head.datalanguage = cursor.getString(cursor.getColumnIndex(DbHelper.H_DATALANGUAGE))
        return head
    }

}
