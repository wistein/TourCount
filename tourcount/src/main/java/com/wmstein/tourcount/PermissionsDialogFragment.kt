package com.wmstein.tourcount

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * PermissionsDialogFragment provides the permission handling, which is
 * necessary since Android Marshmallow (M)
 *
 * Created in Kotlin on 2023-05-26,
 * last edited on 2023-12-15
 */
class PermissionsDialogFragment : DialogFragment() {
    private var context: Context? = null
    private var listener: PermissionsGrantedCallback? = null
    private var shouldResolve = false
    private var externalGrantNeeded = false
    private var externalGrant30Needed = false
    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.context = context
        if (context is PermissionsGrantedCallback) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false
        requestNecessaryPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (shouldResolve) {
            if (externalGrantNeeded) {
                showAppSettingsDialog()
            }
            @RequiresApi(Build.VERSION_CODES.R)
            if (externalGrant30Needed) {
                showAppSettingsDialog30()

            } else {
                //permissions have been accepted
                if (listener != null) {
                    listener!!.locationCaptureFragment()
                    dismiss()
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        context = null
        listener = null
    }

    // Solution with multiple permissions launcher
    private fun requestNecessaryPermissions() {

        val permissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        //launcher permissions request dialog
        permissionLauncherMultiple.launch(permissions)

        //launcher permission request dialog (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
            permissionLauncherSingle.launch(permission)
        }
    }

    // Request multiple permissions
    private val permissionLauncherMultiple = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    )
    { result ->
        // check if permissions were granted from permission request dialog or already granted before

        var allAreGranted = true
        shouldResolve = true
        for (isGranted in result.values) {
            Log.d(TAG, "101, onActivityResult: isGranted: $isGranted")
            allAreGranted = allAreGranted && isGranted
        }

        if (allAreGranted) {
            // ok, multiple permissions are granted
            externalGrantNeeded = false
        } else {
            //All or some Permissions were denied so can't do the task that requires that permission
            externalGrantNeeded = true
            Log.d(TAG, "111, onActivityResult: All or some permissions denied...")
            Toast.makeText(this.context, R.string.perm_denied, Toast.LENGTH_SHORT).show()
        }
    }

    // Request single permissions
    private val permissionLauncherSingle = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        shouldResolve = true
        Log.d(TAG, "121, onActivityResult: isGranted: $isGranted")

        if (isGranted) {
            externalGrant30Needed = false
        } else {
            externalGrant30Needed = true
            Log.d(TAG, "127, onActivityResult: Permission denied...")
            Toast.makeText(this.context, R.string.perm_denied, Toast.LENGTH_SHORT).show()
        }
    }

    // Query missing permissions
    private fun showAppSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.perm_required))
            .setMessage(getString(R.string.perm_hint) + " " + getString(R.string.perm_hint1))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri =
                    Uri.fromParts("package", requireContext().applicationContext.packageName, null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { _: DialogInterface?, _: Int -> dismiss() }
            .create().show()
    }

    // Query missing permission
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showAppSettingsDialog30() {
        //request for the permission
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", "com.wmstein.tourcount", null)
        intent.data = uri
        startActivity(intent)
        dismiss()
    }

    interface PermissionsGrantedCallback {
        fun locationCaptureFragment()
    }

    companion object {
        private const val TAG = "TourCntPermDialogFragment"

        @JvmStatic
        fun newInstance(): PermissionsDialogFragment {
            return PermissionsDialogFragment()
        }
    }
}