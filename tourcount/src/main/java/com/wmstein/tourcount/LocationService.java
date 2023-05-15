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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/******************************************************************
 * LocationService provides the location data: latitude, longitude, height, uncertainty.
 * Its data may be read by most TourCount activities.
 <p>
 * Based on LocationSrv created by anupamchugh on 28/11/16, published under
 * https://github.com/journaldev/journaldev/tree/master/Android/GPSLocationTracking
 * licensed under the MIT License.
 <p>
 * Adopted for TourCount by wmstein since 2018-07-26,
 * last modification on 2023-05-13
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
    private boolean exactLocation = false;

    public LocationService(Context mContext)
    {
        this.mContext = mContext;
        getLocation();
    }

    // Default constructor demanded for service declaration in AndroidManifest.xml
    public LocationService()
    {
    }

    private void getLocation()
    {
        try
        {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            // get GPS status
            assert locationManager != null;
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

            // if GPS is enabled get position using GPS Service
            if (checkGPS && canGetLocation)
            {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                    || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
                            exactLocation = true;
                        }

                    }
                }
            }

            if (!exactLocation)
            {
                // if Network is enabled and still no GPS fix achieved
                if (checkNetwork && canGetLocation)
                {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
//                        || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
                                height = location.getAltitude(); // 0
                                uncertainty = location.getAccuracy(); // 200
                                exactLocation = false;
                            }

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
            Log.e(TAG, "StopListener: " + e);
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
    public void onLocationChanged(@NonNull Location location)
    {
        // do nothing
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle)
    {
        // do nothing
    }

    @Override
    public void onProviderEnabled(@NonNull String s)
    {
        // do nothing
    }

    @Override
    public void onProviderDisabled(@NonNull String s)
    {
        // do nothing
    }
}
