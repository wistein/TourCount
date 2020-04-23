package com.wmstein.tourcount;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

/**********************************************************
 * Based on BeeCountApplication.java by milo on 14/05/2014.
 * Adopted for TourCount by wmstein on 2016-02-18, 
 * last change on 2020-04-22
 */
public class TourCountApplication extends Application
{
    private static final String TAG = "TourCountAppl";
    private static SharedPreferences prefs;
    private BitmapDrawable bMapDraw;
    private Bitmap bMap;
    int width;
    int height;
    
    @Override
    public void onCreate()
    {
        super.onCreate();
        bMapDraw = null;
        bMap = null;
        try
        {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        } catch (Exception e)
        {
            if (MyDebug.LOG)
                Log.e(TAG, e.toString());
        }
    }

    public BitmapDrawable getBackground()
    {
        if (bMapDraw == null)
        {
            return setBackground();
        }
        else
        {
            return bMapDraw;
        }
    }

    public BitmapDrawable setBackground()
    {
        bMapDraw = null;

        String backgroundPref = prefs.getString("pref_back", "default");
        boolean screenOrientL = prefs.getBoolean("screen_Orientation", false);

        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        if (MyDebug.LOG)
            Log.i(TAG, "Width: " + width + "Height: " + height);

        switch (backgroundPref)
        {
        case "none":
            // boring black screen
            bMap = null;
            bMap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bMap.eraseColor(Color.BLACK);
            break;
        case "default":
            if (screenOrientL)
            {
                // landscape
                if ((double) width/height < 1.8)
                {
                    // normal screen size
                    bMap = decodeBitmap(R.drawable.tourcount_picture_ln, width, height);
                }
                else
                {
                    // long screen
                    bMap = decodeBitmap(R.drawable.tourcount_picture_ll, width, height);
                }
            } else
            {
                // portrait
                if ((double) height/width < 1.8)
                {
                    // normal screen
                    bMap = decodeBitmap(R.drawable.tourcount_picture_pn, width, height);
                }
                else
                {
                    // long screen
                    bMap = decodeBitmap(R.drawable.tourcount_picture_pl, width, height);
                }
            }
            break;
        }

        bMapDraw = new BitmapDrawable(this.getResources(), bMap);
        bMap = null;
        return bMapDraw;
    }

    public Bitmap decodeBitmap(int resId, int reqWidth, int reqHeight)
    {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try
        {
            return BitmapFactory.decodeResource(getResources(), resId, options);
        } catch (OutOfMemoryError e)
        {
            return null;
        }
    }

    /*
     * Keep bMapDraw around as a pre-prepared bitmap, only setting it up
     * when the user's settings change or when the application starts up.
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        // Raw height and width of image
        final int height1 = options.outHeight;
        final int width1 = options.outWidth;
        int inSampleSize = 1;

        if (height1 > reqHeight || width1 > reqWidth)
        {

            final int halfHeight = height1 / 2;
            final int halfWidth = width1 / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            //   height1 and width1 larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth)
            {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static SharedPreferences getPrefs()
    {
        return prefs;
    }

}
