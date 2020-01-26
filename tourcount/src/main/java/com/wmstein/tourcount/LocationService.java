package com.wmstein.tourcount;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

/******************************************************************
 * LocationService provides the location data: latitude, longitude, height, uncertainty.
 * It is started and ended by WelcomeActivity.
 * Its data may be read by any TourCount activity.
 *
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * licensed under the MIT License.
 * 
 * Adopted for TourCount by wmstein since 2018-07-26,
 * last modification on 2020-01-26
 */

public class LocationService extends Service implements LocationListener
{
    private static final String TAG = "TourCountLocationSrv";
    Context mContext;
    boolean checkGPS = false;
    boolean checkNetwork = false;
    boolean canGetLocation = false;
    Location location;
    double latitude, longitude, height, uncertainty;
    private static final long MIN_DISTANCE_FOR_UPDATES = 10; // (m)
    private static final long MIN_TIME_BW_UPDATES = 10000; // (msec)
    protected LocationManager locationManager;

    public LocationService(Context mContext)
    {
        this.mContext = mContext;
        getLocation();
    }

    // Default constructor demanded for service declaration in AndroidManifest.xml
    public LocationService () {}

    private void getLocation()
    {
        try
        {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // get GPS status
            checkGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // get network provider status
            checkNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (checkGPS || checkNetwork)
            {
                this.canGetLocation = true;
            }
            else
            {
                Toast.makeText(mContext, getString(R.string.no_provider), Toast.LENGTH_SHORT).show();
            }

            // if only Network is enabled get position using Network Service
                if (checkNetwork && !checkGPS)
                {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_FOR_UPDATES, this);

                        if (locationManager != null)
                        {
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null)
                            {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                height = 0;
                                uncertainty = 500;
                            }
                        }
                    }
                }

                // if GPS is enabled get position using GPS Service
                if (checkGPS)
                {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_FOR_UPDATES, this);

                        if (locationManager != null)
                        {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null)
                            {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                height = location.getAltitude();
                                uncertainty = location.getAccuracy();
                            }
                        }
                    }
                } 

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Stop location service
    public void stopListener()
    {
        try
        {
            if (locationManager != null)
            {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    // requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_GPS);
                    return;
                }
                locationManager.removeUpdates(LocationService.this);
                locationManager = null;
            }
        } catch (Exception e)
        {
            // do nothing
            Log.e(TAG, "StopListener: " + e.toString());
        }
    }

    public double getLongitude()
    {
        if (location != null)
        {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public double getLatitude()
    {
        if (location != null)
        {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getAltitude()
    {
        if (location != null)
        {
            height = location.getAltitude();
        }
        return height;
    }

    public double getAccuracy()
    {
        if (location != null)
        {
            uncertainty = location.getAccuracy();
        }
        return uncertainty;
    }

    public boolean canGetLocation()
    {
        return this.canGetLocation;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onLocationChanged(Location location)
    {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle)
    {

    }

    @Override
    public void onProviderEnabled(String s)
    {

    }

    @Override
    public void onProviderDisabled(String s)
    {

    }
}
