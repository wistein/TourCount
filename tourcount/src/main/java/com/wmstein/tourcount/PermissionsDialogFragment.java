package com.wmstein.tourcount;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * PermissionsDialogFragment provides the permission handling, which is
 * necessary since Android Marshmallow (M)
 * 
 * Original version from RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 * 
 * Adopted for TourCount by wistein on 2018-06-20,
 * last edited on 2020-04-17.
 */
public class PermissionsDialogFragment extends DialogFragment
{
    private static final int PERMISSION_REQUEST_CODE = 101;

    private Context context;
    private PermissionsGrantedCallback listener;

    private boolean shouldResolve;
    private boolean shouldRetry;
    private boolean externalGrantNeeded;

    static PermissionsDialogFragment newInstance()
    {
        return new PermissionsDialogFragment();
    }

    public PermissionsDialogFragment()
    {
    }

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        this.context = context;
        if (context instanceof PermissionsGrantedCallback)
        {
            listener = (PermissionsGrantedCallback) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle);
        setCancelable(false);
        requestNecessaryPermissions();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (shouldResolve)
        {
            if (externalGrantNeeded)
            {
                showAppSettingsDialog();
            }

            else if (shouldRetry)
            {
                showRetryDialog();
            }
           
            else
            {
                //permissions have been accepted
                if (listener != null)
                {
                    listener.permissionCaptureFragment();
                    dismiss();
                }
            }
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        context = null;
        listener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        shouldResolve = true;
        shouldRetry = false;

        for (int i = 0; i < permissions.length; i++)
        {
            String permission = permissions[i];
            int grantResult = grantResults[i];

            if (!shouldShowRequestPermissionRationale(permission) && grantResult != PackageManager.PERMISSION_GRANTED)
            {
                externalGrantNeeded = true;
                return;
            }
            else if (grantResult != PackageManager.PERMISSION_GRANTED)
            {
                shouldRetry = true;
                return;
            }
        }
    }

    private void requestNecessaryPermissions()
    {
        requestPermissions(new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
            PERMISSION_REQUEST_CODE);
    }

    private void showAppSettingsDialog()
    {
        new AlertDialog.Builder(context)
            .setTitle(getString(R.string.perm_required))
            .setMessage(getString(R.string.perm_hint) + " " + getString(R.string.perm_hint1))
            .setPositiveButton(getString(R.string.app_settings), (dialogInterface, i) -> {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getApplicationContext().getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
                dismiss();
            })
            .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dismiss()).create().show();
    }

    private void showRetryDialog()
    {
        new AlertDialog.Builder(context)
            .setTitle(getString(R.string.perm_declined))
            .setMessage(getString(R.string.perm_hint))
            .setPositiveButton(getString(R.string.retry), (dialogInterface, i) -> requestNecessaryPermissions())
            .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dismiss()).create().show();
    }

    public interface PermissionsGrantedCallback
    {
        void permissionCaptureFragment();
    }
    
}
