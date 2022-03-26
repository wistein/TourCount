package com.wmstein.tourcount.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/************************************************************
 * Class SectionDataSource provides methods for table Section
 * Created by wmstein on 2016-04-18,
 * last modified on 2022-03-23
 */
public class SectionDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private final DbHelper dbHandler;
    private final String[] allColumns = {
        DbHelper.S_ID,
        DbHelper.S_NAME,
        DbHelper.S_COUNTRY,
        DbHelper.S_PLZ,
        DbHelper.S_CITY,
        DbHelper.S_PLACE,
        DbHelper.S_TEMP,
        DbHelper.S_WIND,
        DbHelper.S_CLOUDS,
        DbHelper.S_DATE,
        DbHelper.S_START_TM,
        DbHelper.S_END_TM,
        DbHelper.S_NOTES
    };

    public SectionDataSource(Context context)
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

    @SuppressLint("Range")
    private Section cursorToSection(Cursor cursor)
    {
        Section section = new Section();
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID));
        section.name = cursor.getString(cursor.getColumnIndex(DbHelper.S_NAME));
        section.country = cursor.getString(cursor.getColumnIndex(DbHelper.S_COUNTRY));
        section.plz = cursor.getString(cursor.getColumnIndex(DbHelper.S_PLZ));
        section.city = cursor.getString(cursor.getColumnIndex(DbHelper.S_CITY));
        section.place = cursor.getString(cursor.getColumnIndex(DbHelper.S_PLACE));
        section.temp = cursor.getInt(cursor.getColumnIndex(DbHelper.S_TEMP));
        section.wind = cursor.getInt(cursor.getColumnIndex(DbHelper.S_WIND));
        section.clouds = cursor.getInt(cursor.getColumnIndex(DbHelper.S_CLOUDS));
        section.date = cursor.getString(cursor.getColumnIndex(DbHelper.S_DATE));
        section.start_tm = cursor.getString(cursor.getColumnIndex(DbHelper.S_START_TM));
        section.end_tm = cursor.getString(cursor.getColumnIndex(DbHelper.S_END_TM));
        section.notes = cursor.getString(cursor.getColumnIndex(DbHelper.S_NOTES));
        return section;
    }

    public void saveSection(Section section)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.S_NAME, section.name);
        dataToInsert.put(DbHelper.S_COUNTRY, section.country);
        dataToInsert.put(DbHelper.S_PLZ, section.plz);
        dataToInsert.put(DbHelper.S_CITY, section.city);
        dataToInsert.put(DbHelper.S_PLACE, section.place);
        dataToInsert.put(DbHelper.S_TEMP, section.temp);
        dataToInsert.put(DbHelper.S_WIND, section.wind);
        dataToInsert.put(DbHelper.S_CLOUDS, section.clouds);
        dataToInsert.put(DbHelper.S_DATE, section.date);
        dataToInsert.put(DbHelper.S_START_TM, section.start_tm);
        dataToInsert.put(DbHelper.S_END_TM, section.end_tm);
        dataToInsert.put(DbHelper.S_NOTES, section.notes);
        String where = DbHelper.S_ID + " = ?";
        String[] whereArgs = {String.valueOf(section.id)};
        database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
    }

    // called from CountingActivity and EditSpecListActivity
    public Section getSection()
    {
        Section section;
        Cursor cursor = database.query(DbHelper.SECTION_TABLE, allColumns, DbHelper.S_ID + " = 1", null, null, null, null);
        cursor.moveToFirst();
        section = cursorToSection(cursor);
        // Make sure to close the cursor
        cursor.close();
        return section;
    }
    
    // called from CountingActivity
    // store only when field is empty
    public void updateEmptyCountry(int id, String name)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.S_COUNTRY, name);
        String where = DbHelper.S_ID + " = ? AND (" + DbHelper.S_COUNTRY + " IS NULL OR " + DbHelper.S_COUNTRY + " == '')";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
    }

    public void updateEmptyPlz(int id, String name)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.S_PLZ, name);
        String where = DbHelper.S_ID + " = ? AND (" + DbHelper.S_PLZ + " IS NULL OR " + DbHelper.S_PLZ + " == '')";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
    }

    public void updateEmptyCity(int id, String name)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.S_CITY, name);
        String where = DbHelper.S_ID + " = ? AND (" + DbHelper.S_CITY + " IS NULL OR " + DbHelper.S_CITY + " == '')";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
    }

    public void updateEmptyPlace(int id, String name)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.S_PLACE, name);
        String where = DbHelper.S_ID + " = ? AND (" + DbHelper.S_PLACE + " IS NULL OR " + DbHelper.S_PLACE + " == '')";
        String[] whereArgs = {String.valueOf(id)};
        database.update(DbHelper.SECTION_TABLE, dataToInsert, where, whereArgs);
    }

}
