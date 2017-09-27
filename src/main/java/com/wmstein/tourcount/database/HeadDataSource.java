package com.wmstein.tourcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/*
 * Created by wmstein on 31.03.2016.
 */
public class HeadDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private final DbHelper dbHandler;
    private final String[] allColumns = {
        DbHelper.H_ID,
        DbHelper.H_OBSERVER,
    };

    public HeadDataSource(Context context)
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

    public void saveHead(Head head)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.H_ID, head.id);
        dataToInsert.put(DbHelper.H_OBSERVER, head.observer);
        database.update(DbHelper.HEAD_TABLE, dataToInsert, null, null);
    }

    private Head cursorToHead(Cursor cursor)
    {
        Head head = new Head();
        head.id = cursor.getInt(cursor.getColumnIndex(DbHelper.H_ID));
        head.observer = cursor.getString(cursor.getColumnIndex(DbHelper.H_OBSERVER));
        return head;
    }

    public Head getHead()
    {
        Head head;
        Cursor cursor = database.query(DbHelper.HEAD_TABLE, allColumns, String.valueOf(1), null, null, null, null);
        cursor.moveToFirst();
        head = cursorToHead(cursor);
        // Make sure to close the cursor
        cursor.close();
        return head;
    }

}
