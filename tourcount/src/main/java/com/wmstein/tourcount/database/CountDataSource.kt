package com.wmstein.tourcount.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import com.wmstein.tourcount.TourCountApplication

/********************************************************
 * Class CountDataSource provides methods for table Count
 * Basic structure by milo on 05/05/2014.
 * Created by wmstein on 2016-02-18,
 * last change on 2022-03-23,
 * converted to Kotlin on 2023-07-06,
 * last edited on 2024-07-23
 */
class CountDataSource(context: Context?) {
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
    private val tourCountApp = TourCountApplication()

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
    //   test: orderBy code has no effect
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
            val insertId = database!!.insert(DbHelper.COUNT_TABLE, null, values).toInt()
            val cursor = database!!.query(
                DbHelper.COUNT_TABLE,
                allColumns, DbHelper.C_ID + " = " + insertId, null, null, null, null
            )
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

    fun deleteCountById(id: Int) {
        println("Gelöscht: Zähler mit ID: $id")
        database!!.delete(DbHelper.COUNT_TABLE, DbHelper.C_ID + " = " + id, null)
    }

    fun saveCount(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i)
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i)
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i)
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi)
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li)
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei)
        dataToInsert.put(DbHelper.C_NAME, count.name)
        dataToInsert.put(DbHelper.C_CODE, count.code)
        dataToInsert.put(DbHelper.C_NOTES, count.notes)
        dataToInsert.put(DbHelper.C_NAME_G, count.name_g)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f1i
    fun saveCountf1i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F1I, count.count_f1i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f2i
    fun saveCountf2i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F2I, count.count_f2i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_f3i
    fun saveCountf3i(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_F3I, count.count_f3i)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_pi
    fun saveCountpi(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_PI, count.count_pi)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_li
    fun saveCountli(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_LI, count.count_li)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    // save count_ei
    fun saveCountei(count: Count) {
        val dataToInsert = ContentValues()
        dataToInsert.put(DbHelper.C_COUNT_EI, count.count_ei)
        val where = DbHelper.C_ID + " = ?"
        val whereArgs = arrayOf(count.id.toString())
        database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
    }

    fun updateCountName(id: Int, name: String?, code: String?, name_g: String?) {
        if (database!!.isOpen) {
            val dataToInsert = ContentValues()
            dataToInsert.put(DbHelper.C_NAME, name)
            dataToInsert.put(DbHelper.C_CODE, code)
            dataToInsert.put(DbHelper.C_NAME_G, name_g)
            val where = DbHelper.C_ID + " = ?"
            val whereArgs = arrayOf(id.toString())
            database!!.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs)
        }
    }

    fun getCountById(count_id: Int): Count {
        val cursor = database!!.query(
            DbHelper.COUNT_TABLE, allColumns,
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
                DbHelper.COUNT_TABLE, allColumns,
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
                DbHelper.COUNT_TABLE, allColumns,
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
                DbHelper.COUNT_TABLE, allColumns,
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
            DbHelper.COUNT_TABLE, allColumns,
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
            DbHelper.COUNT_TABLE, allColumns,
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
            DbHelper.COUNT_TABLE, allColumns,
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

    // Used by CountingActivity
    val allImages: Array<Int?>
        get() {
            val cursor = database!!.query(
                DbHelper.COUNT_TABLE, allColumns,
                null, null, null, null, null
            )
            val imageArray = arrayOfNulls<Int>(cursor.count)
            cursor.moveToFirst()
            var i = 0
            while (!cursor.isAfterLast) {
                @SuppressLint("Range") val ucode = cursor.getString(cursor.getColumnIndex("code"))
                val rname = "p$ucode" // species picture resource name
                val resId = tourCountApp.getResId(rname)
                val resId0 = tourCountApp.getResId("p00000")
                if (resId != 0) {
                    imageArray[i] = resId
                } else {
                    imageArray[i] = resId0
                }
                i++
                cursor.moveToNext()
            }
            cursor.close()
            return imageArray
        }

    // Used by CountingActivity
    val allImagesSrtName: Array<Int?>
        get() {
            val cursor = database!!.query(
                DbHelper.COUNT_TABLE, allColumns,
                null, null, null, null, DbHelper.C_NAME
            )
            val imageArray = arrayOfNulls<Int>(cursor.count)
            cursor.moveToFirst()
            var i = 0
            while (!cursor.isAfterLast) {
                @SuppressLint("Range") val ucode = cursor.getString(cursor.getColumnIndex("code"))
                val rname = "p$ucode" // species picture resource name
                val resId = tourCountApp.getResId(rname)
                val resId0 = tourCountApp.getResId("p00000")
                if (resId != 0) {
                    imageArray[i] = resId
                } else {
                    imageArray[i] = resId0
                }
                i++
                cursor.moveToNext()
            }
            cursor.close()
            return imageArray
        }

    // Used by CountingActivity
    val allImagesSrtCode: Array<Int?>
        get() {
            val cursor = database!!.query(
                DbHelper.COUNT_TABLE, allColumns,
                null, null, null, null, DbHelper.C_CODE
            )
            val imageArray = arrayOfNulls<Int>(cursor.count)
            cursor.moveToFirst()
            var i = 0
            while (!cursor.isAfterLast) {
                @SuppressLint("Range") val ucode = cursor.getString(cursor.getColumnIndex("code"))
                val rname = "p$ucode" // species picture resource name
                val resId = tourCountApp.getResId(rname)
                val resId0 = tourCountApp.getResId("p00000")
                if (resId != 0) {
                    imageArray[i] = resId
                } else {
                    imageArray[i] = resId0
                }
                i++
                cursor.moveToNext()
            }
            cursor.close()
            return imageArray
        }

    // Used by EditSpecListActivity
    val allSpecies: List<Count>
        get() {
            val speci: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.COUNT_TABLE
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
                "select DISTINCT " + DbHelper.C_CODE + " from " + DbHelper.COUNT_TABLE
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
                "select * from " + DbHelper.COUNT_TABLE
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
                "select * from " + DbHelper.COUNT_TABLE
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

    // Used by ListSpeciesActivity
    val cntSpecies: List<Count>
        get() {
            val speci: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.COUNT_TABLE
                        + " WHERE ("
                        + DbHelper.C_COUNT_F1I + " > 0 or " + DbHelper.C_COUNT_F2I + " > 0 or "
                        + DbHelper.C_COUNT_F3I + " > 0 or " + DbHelper.C_COUNT_PI + " > 0 or "
                        + DbHelper.C_COUNT_LI + " > 0 or " + DbHelper.C_COUNT_EI + " > 0)"
                        + " order by " + DbHelper.C_ID, null, null
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

    // Used by ListSpeciesActivity
    val cntSpeciesSrtName: List<Count>
        get() {
            val counts: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.COUNT_TABLE
                        + " WHERE " + " ("
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

    // Used by ListSpeciesActivity
    val cntSpeciesSrtCode: List<Count>
        get() {
            val counts: MutableList<Count> = ArrayList()
            val cursor = database!!.rawQuery(
                "select * from " + DbHelper.COUNT_TABLE
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

}
