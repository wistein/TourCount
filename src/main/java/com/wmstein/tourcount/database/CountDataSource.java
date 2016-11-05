package com.wmstein.tourcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by milo on 05/05/2014.
 * Changed by wmstein on 18.02.2016
 */
public class CountDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private String[] allColumns = {
        DbHelper.C_ID,
        DbHelper.C_COUNT,
        DbHelper.C_NAME,
        DbHelper.C_CODE,
        DbHelper.C_NOTES
    };

    public List<Count> count_list;

    public CountDataSource(Context context)
    {
        dbHandler = new DbHelper(context);
    }

    public void open() throws SQLException
    {
        database = dbHandler.getWritableDatabase();
    }

    public void close()
    {
        dbHandler.close();
    }

    public Count createCount(String name, String code)
    {
        ContentValues values = new ContentValues();
        values.put(DbHelper.C_NAME, name);
        values.put(DbHelper.C_COUNT, 0);
        values.put(DbHelper.C_CODE, code);
        // notes should be default null and so isn't created here

        int insertId = (int) database.insert(DbHelper.COUNT_TABLE, null, values);
        Cursor cursor = database.query(DbHelper.COUNT_TABLE,
            allColumns, DbHelper.C_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        Count newCount = cursorToCount(cursor);
        cursor.close();
        return newCount;
    }

    private Count cursorToCount(Cursor cursor)
    {
        Count newcount = new Count();
        newcount.id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_ID));
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME));
        newcount.count = cursor.getInt(cursor.getColumnIndex(DbHelper.C_COUNT));
        newcount.code = cursor.getString(cursor.getColumnIndex(DbHelper.C_CODE));
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES));
        return newcount;
    }

    public void deleteCountById(int id)
    {
        System.out.println("Gelöscht: Zähler mit ID: " + id);
        database.delete(DbHelper.COUNT_TABLE, DbHelper.C_ID + " = " + id, null);

    }

    public void saveCount(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_COUNT, count.count);
        dataToInsert.put(DbHelper.C_NAME, count.name);
        dataToInsert.put(DbHelper.C_CODE, count.code);
        dataToInsert.put(DbHelper.C_NOTES, count.notes);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    public void updateCountName(int id, String name, String code)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.C_NAME, name);
        dataToInsert.put(DbHelper.C_CODE, code);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    public Count getCountById(int count_id)
    {
        Cursor cursor = database.query(DbHelper.COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + count_id, null, null, null, null);

        cursor.moveToFirst();
        Count count = cursorToCount(cursor);
        cursor.close();
        return count;
    }

    // Used by ListSpeciesActivity
    public List<Count> getAllSpecies()
    {
        List<Count> speci = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " order by " + DbHelper.C_ID, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            speci.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return speci;
    }

    // Used by ListSpeciesActivity
    public List<Count> getAllSpeciesSrtName()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " order by " + DbHelper.C_NAME, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by ListSpeciesActivity
    public List<Count> getAllSpeciesSrtCode()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " order by " + DbHelper.C_CODE, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by ListSpeciesActivity
    public List<Count> getCntSpecies()
    {
        List<Count> speci = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_COUNT + " > 0 order by " + DbHelper.C_ID, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            speci.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return speci;
    }

    // Used by ListSpeciesActivity
    public List<Count> getCntSpeciesSrtName()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_COUNT + " > 0 order by " + DbHelper.C_NAME, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }

    // Used by ListSpeciesActivity
    public List<Count> getCntSpeciesSrtCode()
    {
        List<Count> counts = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DbHelper.COUNT_TABLE
            + " WHERE " + DbHelper.C_COUNT + " > 0 order by " + DbHelper.C_CODE, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Count count = cursorToCount(cursor);
            counts.add(count);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return counts;
    }
}
