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
public class AlertDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private DbHelper dbHandler;
    private String[] allColumns = {
        DbHelper.A_ID,
        DbHelper.A_COUNT_ID,
        DbHelper.A_ALERT,
        DbHelper.A_ALERT_TEXT
    };

    public AlertDataSource(Context context)
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

    public Alert createAlert(int count_id, int alert_value, String alert_text)
    {
        ContentValues values = new ContentValues();
        values.put(DbHelper.A_COUNT_ID, count_id);
        values.put(DbHelper.A_ALERT, alert_value);
        values.put(DbHelper.A_ALERT_TEXT, alert_text);

        int insertId = (int) database.insert(DbHelper.ALERT_TABLE, null, values);
        Cursor cursor = database.query(DbHelper.ALERT_TABLE,
            allColumns, DbHelper.A_ID + " = " + insertId, null,
            null, null, null);
        cursor.moveToFirst();
        Alert newAlert = cursorToAlert(cursor);
        cursor.close();
        return newAlert;
    }

    private Alert cursorToAlert(Cursor cursor)
    {
        Alert newalert = new Alert();
        newalert.id = cursor.getInt(cursor.getColumnIndex(DbHelper.A_ID));
        newalert.count_id = cursor.getInt(cursor.getColumnIndex(DbHelper.A_COUNT_ID));
        newalert.alert = cursor.getInt(cursor.getColumnIndex(DbHelper.A_ALERT));
        newalert.alert_text = cursor.getString(cursor.getColumnIndex(DbHelper.A_ALERT_TEXT));
        return newalert;
    }

    public void deleteAlert(Alert alert)
    {
        int id = alert.id;
        System.out.println("Alert deleted with id: " + id);
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_ID + " = " + id, null);
    }

    public void deleteAlertById(int id)
    {
        System.out.println("Alert deleted with id: " + id);
        database.delete(DbHelper.ALERT_TABLE, DbHelper.A_ID + " = " + id, null);
    }

    public void saveAlert(int alert_id, int alert_value, String alert_text)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(DbHelper.A_ALERT, alert_value);
        dataToInsert.put(DbHelper.A_ALERT_TEXT, alert_text);
        String where = DbHelper.A_ID + " = ?";
        String[] whereArgs = {String.valueOf(alert_id)};
        database.update(DbHelper.ALERT_TABLE, dataToInsert, where, whereArgs);
    }

    public List<Alert> getAllAlertsForCount(int count_id)
    {
        List<Alert> alerts = new ArrayList<>();

        Cursor cursor = database.query(DbHelper.ALERT_TABLE, allColumns,
            DbHelper.A_COUNT_ID + " = " + count_id, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            Alert alert = cursorToAlert(cursor);
            alerts.add(alert);
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return alerts;
    }

}
