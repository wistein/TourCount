package com.wmstein.tourcount.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.wmstein.tourcount.TourCountApplication;

import java.util.ArrayList;
import java.util.List;

import static com.wmstein.tourcount.database.DbHelper.COUNT_TABLE;
import static com.wmstein.tourcount.database.DbHelper.C_CODE;
import static com.wmstein.tourcount.database.DbHelper.C_COUNT_EI;
import static com.wmstein.tourcount.database.DbHelper.C_COUNT_F1I;
import static com.wmstein.tourcount.database.DbHelper.C_COUNT_F2I;
import static com.wmstein.tourcount.database.DbHelper.C_COUNT_F3I;
import static com.wmstein.tourcount.database.DbHelper.C_COUNT_LI;
import static com.wmstein.tourcount.database.DbHelper.C_COUNT_PI;

/********************************************************
 * Class CountDataSource provides methods for table Count
 * Basic structure by milo on 05/05/2014.
 * Created by wmstein on 2016-02-18,
 * last change on 2022-03-23
 */
public class CountDataSource
{
    // Database fields
    private SQLiteDatabase database;
    private final DbHelper dbHandler;
    private final String[] allColumns = {
        DbHelper.C_ID,
        C_COUNT_F1I,
        C_COUNT_F2I,
        C_COUNT_F3I,
        C_COUNT_PI,
        C_COUNT_LI,
        C_COUNT_EI,
        DbHelper.C_NAME,
        C_CODE,
        DbHelper.C_NOTES,
        DbHelper.C_NAME_G
    };

    private final TourCountApplication tourCountApp = new TourCountApplication();

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

    public void createCount(String name, String code, String name_g)
    {
        if (database.isOpen())
        {
            ContentValues values = new ContentValues();
            values.put(DbHelper.C_NAME, name);
            values.put(C_COUNT_F1I, 0);
            values.put(C_COUNT_F2I, 0);
            values.put(C_COUNT_F3I, 0);
            values.put(C_COUNT_PI, 0);
            values.put(C_COUNT_LI, 0);
            values.put(C_COUNT_EI, 0);
            values.put(C_CODE, code);
            values.put(DbHelper.C_NOTES, "");
            values.put(DbHelper.C_NAME_G, name_g);

            int insertId = (int) database.insert(COUNT_TABLE, null, values);
            Cursor cursor = database.query(COUNT_TABLE,
                allColumns, DbHelper.C_ID + " = " + insertId, null, null, null, null);
            cursor.close();
        }
    }

    @SuppressLint("Range")
    private Count cursorToCount(Cursor cursor)
    {
        Count newcount = new Count();
        newcount.id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_ID));
        newcount.name = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME));
        newcount.count_f1i = cursor.getInt(cursor.getColumnIndex(C_COUNT_F1I));
        newcount.count_f2i = cursor.getInt(cursor.getColumnIndex(C_COUNT_F2I));
        newcount.count_f3i = cursor.getInt(cursor.getColumnIndex(C_COUNT_F3I));
        newcount.count_pi = cursor.getInt(cursor.getColumnIndex(C_COUNT_PI));
        newcount.count_li = cursor.getInt(cursor.getColumnIndex(C_COUNT_LI));
        newcount.count_ei = cursor.getInt(cursor.getColumnIndex(C_COUNT_EI));
        newcount.code = cursor.getString(cursor.getColumnIndex(C_CODE));
        newcount.notes = cursor.getString(cursor.getColumnIndex(DbHelper.C_NOTES));
        newcount.name_g = cursor.getString(cursor.getColumnIndex(DbHelper.C_NAME_G));
        return newcount;
    }

    public void deleteCountById(int id)
    {
        System.out.println("Gelöscht: Zähler mit ID: " + id);
        database.delete(COUNT_TABLE, DbHelper.C_ID + " = " + id, null);

    }

    public void saveCount(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F1I, count.count_f1i);
        dataToInsert.put(C_COUNT_F2I, count.count_f2i);
        dataToInsert.put(C_COUNT_F3I, count.count_f3i);
        dataToInsert.put(C_COUNT_PI, count.count_pi);
        dataToInsert.put(C_COUNT_LI, count.count_li);
        dataToInsert.put(C_COUNT_EI, count.count_ei);
        dataToInsert.put(DbHelper.C_NAME, count.name);
        dataToInsert.put(C_CODE, count.code);
        dataToInsert.put(DbHelper.C_NOTES, count.notes);
        dataToInsert.put(DbHelper.C_NAME_G, count.name_g);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f1i
    public void saveCountf1i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F1I, count.count_f1i);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f2i
    public void saveCountf2i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F2I, count.count_f2i);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_f3i
    public void saveCountf3i(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_F3I, count.count_f3i);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_pi
    public void saveCountpi(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_PI, count.count_pi);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_li
    public void saveCountli(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_LI, count.count_li);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    // save count_ei
    public void saveCountei(Count count)
    {
        ContentValues dataToInsert = new ContentValues();
        dataToInsert.put(C_COUNT_EI, count.count_ei);
        String where = DbHelper.C_ID + " = ?";
        String[] whereArgs = {String.valueOf(count.id)};
        database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
    }

    public void updateCountName(int id, String name, String code, String name_g)
    {
        if (database.isOpen())
        {
            ContentValues dataToInsert = new ContentValues();
            dataToInsert.put(DbHelper.C_NAME, name);
            dataToInsert.put(C_CODE, code);
            dataToInsert.put(DbHelper.C_NAME_G, name_g);
            String where = DbHelper.C_ID + " = ?";
            String[] whereArgs = {String.valueOf(id)};
            database.update(COUNT_TABLE, dataToInsert, where, whereArgs);
        }
    }

    public Count getCountById(int count_id)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            DbHelper.C_ID + " = " + count_id,
            null, null, null, null);

        cursor.moveToFirst();
        Count count = cursorToCount(cursor);
        cursor.close();
        return count;
    }

    // Used by CountingActivity
    public String[] getAllIds()
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, null);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Used by CountingActivity
    public String[] getAllIdsSrtName()
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, DbHelper.C_NAME);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Used by CountingActivity
    public String[] getAllIdsSrtCode()
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, C_CODE);

        String[] idArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            int uid = cursor.getInt(0);
            idArray[i] = Integer.toString(uid);
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return idArray;
    }

    // Used by CountingActivity
    public String[] getAllStrings(String sname)
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, null);

        String[] uArray = new String[cursor.getCount()];
        int i = 0;
        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Used by CountingActivity
    public String[] getAllStringsSrtName(String sname)
    {

        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, DbHelper.C_NAME);

        String[] uArray = new String[cursor.getCount()];

        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Used by CountingActivity
    public String[] getAllStringsSrtCode(String sname)
    {

        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, C_CODE);

        String[] uArray = new String[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String uname = cursor.getString(cursor.getColumnIndex(sname));
            uArray[i] = uname;
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return uArray;
    }

    // Used by CountingActivity
    public Integer[] getAllImages()
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, null);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = tourCountApp.getResId(rname);
            int resId0 = tourCountApp.getResId("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Used by CountingActivity
    public Integer[] getAllImagesSrtName()
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, DbHelper.C_NAME);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = tourCountApp.getResId(rname);
            int resId0 = tourCountApp.getResId("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Used by CountingActivity
    public Integer[] getAllImagesSrtCode()
    {
        Cursor cursor = database.query(COUNT_TABLE, allColumns,
            null, null, null, null, C_CODE);

        Integer[] imageArray = new Integer[cursor.getCount()];
        cursor.moveToFirst();
        int i = 0;
        while (!cursor.isAfterLast())
        {
            @SuppressLint("Range") String ucode = cursor.getString(cursor.getColumnIndex("code"));

            String rname = "p" + ucode; // species picture resource name
            int resId = tourCountApp.getResId(rname);
            int resId0 = tourCountApp.getResId("p00000");

            if (resId != 0)
            {
                imageArray[i] = resId;
            }
            else
            {
                imageArray[i] = resId0;
            }
            i++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return imageArray;
    }

    // Used by EditSpecListActivity
    public List<Count> getAllSpecies()
    {
        List<Count> speci = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
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

    // Used by WelcomeActivity
    public int getDiffSpec()
    {
        int cntSpec = 0;
        Cursor cursor = database.rawQuery("select DISTINCT " + C_CODE + " from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0)", null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast())
        {
            cntSpec++;
            cursor.moveToNext();
        }
        // Make sure to close the cursor
        cursor.close();
        return cntSpec;
    }

    // Used by EditSpecListActivity
    public List<Count> getAllSpeciesSrtName()
    {
        List<Count> speci = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " order by " + DbHelper.C_NAME, null);

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

    // Used by EditSpecListActivity and AddSpeciesActivity
    public List<Count> getAllSpeciesSrtCode()
    {
        List<Count> speci = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " order by " + C_CODE, null);

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
    public List<Count> getCntSpecies()
    {
        List<Count> speci = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0)"
            + " order by " + DbHelper.C_ID, null, null);

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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0)"
            + " order by " + DbHelper.C_NAME, null, null);

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

        Cursor cursor = database.rawQuery("select * from " + COUNT_TABLE
            + " WHERE " + " ("
            + C_COUNT_F1I + " > 0 or " + C_COUNT_F2I + " > 0 or "
            + C_COUNT_F3I + " > 0 or " + C_COUNT_PI + " > 0 or "
            + C_COUNT_LI + " > 0 or " + C_COUNT_EI + " > 0)"
            + " order by " + C_CODE, null, null);

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
