package com.wmstein.tourcount.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.wmstein.tourcount.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by wmstein on 18.04.2016
 */
public class SectionDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private String[] allColumns = {
        DbHelper.S_ID,
        DbHelper.S_CREATED_AT,
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

    public List<Section> section_list;

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

    public Section createSection()
    {
        ContentValues values = new ContentValues();
        values.put(DbHelper.S_NAME, "");
        values.put(DbHelper.S_COUNTRY, "");
        values.put(DbHelper.S_PLZ, "");
        values.put(DbHelper.S_CITY, "");
        values.put(DbHelper.S_PLACE, "");
        values.put(DbHelper.S_TEMP, 0);
        values.put(DbHelper.S_WIND, 0);
        values.put(DbHelper.S_CLOUDS, 0);
        values.put(DbHelper.S_DATE, "");
        values.put(DbHelper.S_START_TM, "");
        values.put(DbHelper.S_END_TM, "");
        int insertId = (int) database.insert(DbHelper.SECTION_TABLE, null, values);
        Cursor cursor = database.query(DbHelper.SECTION_TABLE,
            allColumns, DbHelper.S_ID + " = " + insertId, null,
            null, null, null);
        cursor.moveToFirst();
        Section newSection = cursorToSection(cursor);
        cursor.close();
        return newSection;
    }

    private Section cursorToSection(Cursor cursor)
    {
        Section section = new Section();
        section.id = cursor.getInt(cursor.getColumnIndex(DbHelper.S_ID));
        section.created_at = cursor.getLong(cursor.getColumnIndex(DbHelper.S_CREATED_AT));
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

    /******************************/
    // called from CountingActivity
    public void saveDateSection(Section section)
    {
        Date date = new Date();
        long timeMsec = date.getTime();

        ContentValues values = new ContentValues();
        values.put(DbHelper.S_CREATED_AT, timeMsec);
        String where = DbHelper.S_ID + " = ?";
        String[] whereArgs = {String.valueOf(section.id)};
        database.update(DbHelper.SECTION_TABLE, values, where, whereArgs);
    }

    // called from CountingActivity and EditSectionActivity
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
    
}
