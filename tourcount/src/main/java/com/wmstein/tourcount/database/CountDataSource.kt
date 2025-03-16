package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.wmstein.tourcount.database.DbHelper.Companion.COUNT_TABLE

/********************************************************
 * Class CountDataSource provides methods for table Count
 * Basic structure by milo on 05/05/2014.
 * Created by wmstein on 2016-02-18,
 * last change on 2022-03-23,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2025-03-15
 */
class CountDataSource(context: Context) {
    // Database fields
    private var database: SQLiteDatabase? = null
    private val dbHandler: DbHelper
    private val allColumns = arrayOf(
        DbHelper.C_ID,
        DbHelper.C_COUNT_F1I,
        DbHelper.C_COUNT_F2I,
        DbHelper.C_COUNT_F3I,
        DbHelper.C_COUNT_PI,
        DbHelper.C_COUNT_LI,
        DbHelper.C_COUNT_EI,
        DbHelper.C_NAME,
        DbHelper.C_CODE,
        DbHelper.C_NOTES,
        DbHelper.C_NAME_G
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

    // Used by AddSpeciesActivity
    fun createCount(name: String?, code: String?, name_g: String?) {
        if (database!!.isOpen) {
            val values = ContentValues()
            values.put(DbHelper.C_NAME, name)
            values.put(DbHelper.C_COUNT_F1I, 0)
            values.put(DbHelper.C_COUNT_F2I, 0)
            values.put(DbHelper.C_COUNT_F3I, 0)
            values.put(DbHelper.C_COUNT_PI, 0)
            values.put(DbHelper.C_COUNT_LI, 0)
            values.put(DbHelper.C_COUNT_EI, 0)
            values.put(DbHelper.C_CODE, code)
            values.put(DbHelper.C_NOTES, "")
            values.put(DbHelper.C_NAME_G, name_g)
            val insertId = database!!.insert(COUNT_TABLE, null, values).toInt()
            val cursor = database!!.query(COUNT_TABLE,
                allColumns, DbHelper.C_ID + " = " + insertId, null, null, null, null)
            cursor.close()
        }
    }

    @SuppressLint("Range")
    private fun cursorToCount(cursor: Cursor): Count {
        val newcount = Count()
        newcount.id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_ID))
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME))
        newcount.count_f1i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F1I))
        newcount.count_f2i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F2I))
        newcount.count_f3i = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_F3I))
        newcount.count_pi = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_PI))
        newcount.count_li = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_LI))
        newcount.count_ei = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT_EI))
        newcount.code = cursor.getString(cursor.getColumnIndex(DbHelper.C_CODE))
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES))
        newcount.name_g = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME_G))
        return newcount
    }

    fun deleteCountByCode(code: String) {
        println("Gelöscht: Zähler mit Code: $code")
        database!!.delete(COUNT_TABLE, DbHelper.C_CODE + " = '$code'", null)
    }

    fun saveCountNotes(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_NOTES, count.notes)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Save count_f1i
    fun saveCountf1i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Save count_f2i
    fun saveCountf2i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Save count_f3i
    fun saveCountf3i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Save count_pi
    fun saveCountpi(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Save count_li
    fun saveCountli(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Save count_ei
    fun saveCountei(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // Used by EditSpecListActivity
    fun updateCountItem(id: Int, name: String?, code: String?, nameG: String?) {
        if (database!!.isOpen) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.C_NAME, name)
            dataToInsert.put(DbHelper.C_CODE, code)
            dataToInsert.put(DbHelper.C_NAME_G, nameG)
            val where = DbHelper.C_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            database!!.update(COUNT_TABLE, dataToInsert, where, whereArgs)
        }
    }

    // Used by WelcomeActivity
    fun writeCountItem(id: String?, code: String?, name: String?, nameG: String?) {
        if (database!!.isOpen) {
            val values = ContentValues()
            values.put(DbHelper.C_ID, id)
            values.put(DbHelper.C_NAME, name)
            values.put(DbHelper.C_COUNT_F1I, 0)
            values.put(DbHelper.C_COUNT_F2I, 0)
            values.put(DbHelper.C_COUNT_F3I, 0)
            values.put(DbHelper.C_COUNT_PI, 0)
            values.put(DbHelper.C_COUNT_LI, 0)
            values.put(DbHelper.C_COUNT_EI, 0)
            values.put(DbHelper.C_CODE, code)
            values.put(DbHelper.C_NOTES, "")
            values.put(DbHelper.C_NAME_G, nameG)
            database!!.insert(COUNT_TABLE, null, values)
        }
    }

    fun getCountById(count_id: Int): Count {
        val cursor = database!!.query(
            COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + count_id,
            null, null, null, null
        )
        cursor.moveToFirst()
        val count = cursorToCount(cursor)
        cursor.close()
        return count
    }

    // Used by CountingActivity
    val allIds: Array<String?>
        get() {
            val cursor = database!!.query(
                COUNT_TABLE, allColumns,
                null, null, null, null, null
            )
            val idArray = arrayOfNulls<String>(cursor.count)
            cursor.moveToFirst()
            var i = 0
            while (!cursor.isAfterLast) {
                val uid = cursor.getInt(0)
                idArray[i] = uid.toString()
                i++
                cursor.moveToNext()
            }
            cursor.close()
            return idArray
        }

    // Used by CountingActivity
    val allIdsSrtName: Array<String?>
        get() {
            val cursor = database!!.query(
                COUNT_TABLE, allColumns,
                null, null, null, null, DbHelper.C_NAME
            )
            val idArray = arrayOfNulls<String>(cursor.count)
            cursor.moveToFirst()
            var i = 0
            while (!cursor.isAfterLast) {
                val uid = cursor.getInt(0)
                idArray[i] = uid.toString()
                i++
                cursor.moveToNext()
            }
            cursor.close()
            return idArray
        }

    // Used by CountingActivity
    val allIdsSrtCode: Array<String?>
        get() {
            val cursor = database!!.query(
                COUNT_TABLE, allColumns,
                null, null, null, null, DbHelper.C_CODE
            )
            val idArray = arrayOfNulls<String>(cursor.count)
            cursor.moveToFirst()
            var i = 0
            while (!cursor.isAfterLast) {
                val uid = cursor.getInt(0)
                idArray[i] = uid.toString()
                i++
                cursor.moveToNext()
            }
            cursor.close()
            return idArray
        }

    // Used by CountingActivity
    fun getAllStrings(sname: String?): Array<String?> {
        val cursor = database!!.query(
            COUNT_TABLE, allColumns,
            null, null, null, null, null
        )
        val uArray = arrayOfNulls<String>(cursor.count)
        var i = 0
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            @SuppressLint("Range") val uname = cursor.getString(cursor.getColumnIndex(sname))
            uArray[i] = uname
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return uArray
    }

    // Used by CountingActivity
    fun getAllStringsSrtName(sname: String?): Array<String?> {
        val cursor = database!!.query(
            COUNT_TABLE, allColumns,
            null, null, null, null, DbHelper.C_NAME
        )
        val uArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range") val uname = cursor.getString(cursor.getColumnIndex(sname))
            uArray[i] = uname
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return uArray
    }

    // Used by CountingActivity
    fun getAllStringsSrtCode(sname: String?): Array<String?> {
        val cursor = database!!.query(
            COUNT_TABLE, allColumns,
            null, null, null, null, DbHelper.C_CODE
        )
        val uArray = arrayOfNulls<String>(cursor.count)
        cursor.moveToFirst()
        var i = 0
        while (!cursor.isAfterLast) {
            @SuppressLint("Range") val uname = cursor.getString(cursor.getColumnIndex(sname))
            uArray[i] = uname
            i++
            cursor.moveToNext()
        }
        cursor.close()
        return uArray
    }

    // Used by EditSpecListActivity
    val allSpecies: List<Count>
        get() {
            val speci: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + COUNT_TABLE
                        + " order by " + DbHelper.C_ID, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                speci.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return speci
        }

    // Used by WelcomeActivity
    val diffSpec: Int
        get() {
            var cntSpec = 0
            val cursor = database!!.rawQuery(
                "select DISTINCT " + DbHelper.C_CODE + " from " + COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)", null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                cntSpec++
                cursor.moveToNext()
            }
            cursor.close()
            return cntSpec
        }

    // Used by EditSpecListActivity
    val allSpeciesSrtName: List<Count>
        get() {
            val speci: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + COUNT_TABLE
                        + " order by " + DbHelper.C_NAME, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                speci.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return speci
        }

    // Used by EditSpecListActivity and AddSpeciesActivity
    val allSpeciesSrtCode: List<Count>
        get() {
            val speci: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + COUNT_TABLE
                        + " order by " + DbHelper.C_CODE, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                speci.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return speci
        }

    // Used by ShowResultsActivity
    val cntSpeciesSrtName: List<Count>
        get() {
            val counts: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_NAME, null, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                counts.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return counts
        }

    // Used by ShowResultsActivity
    val cntSpeciesSrtCode: List<Count>
        get() {
            val counts: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + COUNT_TABLE
                        + " WHERE " + " ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_CODE, null, null
            )
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val count = cursorToCount(cursor)
                counts.add(count)
                cursor.moveToNext()
            }
            cursor.close()
            return counts
        }

    // Sorts COUNT_TABLE for C_CODE and contiguous index
    fun sortCounts() {
        var sql = "alter table 'counts' rename to 'counts_backup'"
        database!!.execSQL(sql)

        // create new counts table
        sql = ("create table counts("
                + DbHelper.C_ID + " integer primary key, "
                + DbHelper.C_COUNT_F1I + " int, "
                + DbHelper.C_COUNT_F2I + " int, "
                + DbHelper.C_COUNT_F3I + " int, "
                + DbHelper.C_COUNT_PI + " int, "
                + DbHelper.C_COUNT_LI + " int, "
                + DbHelper.C_COUNT_EI + " int, "
                + DbHelper.C_NAME + " text, "
                + DbHelper.C_CODE + " text, "
                + DbHelper.C_NOTES + " text, "
                + DbHelper.C_NAME_G + " text)")
        database!!.execSQL(sql)

        // insert the whole COUNT_TABLE data sorted into counts
        sql = ("INSERT INTO 'counts' (" +
                "'count_f1i', 'count_f2i', 'count_f3i', 'count_pi', 'count_li', 'count_ei', " +
                "'name', 'code', 'notes', 'name_g') " +
                "SELECT " +
                "count_f1i, count_f2i, count_f3i, count_pi, count_li, count_ei, " +
                "name, code, notes, name_g " +
                "from 'counts_backup' order by code")
        database!!.execSQL(sql)

        sql = "DROP TABLE 'counts_backup'"
        database!!.execSQL(sql)
    }

}
