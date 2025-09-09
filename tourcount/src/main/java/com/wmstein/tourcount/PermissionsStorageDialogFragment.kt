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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * PermissionsStorageDialogFragment provides the storage permission handling,
 * which is necessary since Android Marshmallow (M)
 *
 * Based on RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 *
 * Adopted for TourCount by wistein on 2018-06-20,
 * last edited in java on 2020-04-17,
 * converted to Kotlin on 2023-05-26,
 * last edited on 2025-08-18
 */
class PermissionsStorageDialogFragment : DialogFragment() {
    private var context: Context? = null
    private var shouldResolve = false
    private var externalGrantNeeded = false
    private var externalGrant30Needed = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "43, onCreate")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Check for given storage permission
        requestStoragePermissions()
    }

    // Request storage permission
    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { //  (Android <11)
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (MyDebug.DLOG) Log.i(TAG, "56, permission $permission")
            permissionLauncherStorage.launch(permission)
        } else { // Android >= 11
            val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
            if (MyDebug.DLOG) Log.i(TAG, "60, permission $permission")
            permissionLauncherStorage.launch(permission)
        }
    }

    // Request single permissions in system settings app
    private val permissionLauncherStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        shouldResolve = true
        if (MyDebug.DLOG) Log.d(TAG, "71, isGranted: $isGranted")

        if (isGranted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                externalGrantNeeded = false
            } else {
                externalGrant30Needed = false
            }
            dismiss()
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                externalGrantNeeded = true
            } else {
                externalGrant30Needed = true
            }
            if (MyDebug.DLOG) Log.d(TAG, "86, externalGrantNeeded: true")
        }
    }

    override fun onResume() {
        super.onResume()

        if (MyDebug.DLOG) Log.i(TAG, "93, onResume")

        if (shouldResolve) {
            if (externalGrantNeeded) {
                if (MyDebug.DLOG) Log.d(TAG, "97, onResume: externalGrantNeeded: true")
                showAppSettingsStorageDialog() // accept or deny WRITE_EXTERNAL_STORAGE permission
            }
            if (externalGrant30Needed) {
                if (MyDebug.DLOG) Log.d(TAG, "101, onResume: externalGrant30Needed: true")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30
                    showAppSettingsManageStorageDialog() // accept or deny MANAGE_EXTERNAL_STORAGE permission
                }
            }
        }
    }

    // Query missing external storage permission
    private fun showAppSettingsStorageDialog() {
        if (MyDebug.DLOG) Log.d(TAG, "111, onResume: StorSettingsDlg")
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_storage_title))
            .setMessage(getString(R.string.dialog_storage_message))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", "com.wmstein.tourcount", null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel))
            { _: DialogInterface?, _: Int -> 
                dismiss()
            }
            .create().show()
    }

    // Query missing manage storage permission for API >= 30
    private fun showAppSettingsManageStorageDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30
            if (MyDebug.DLOG) Log.d(TAG, "133, onResume: StorSettings30Dlg")

            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", "com.wmstein.tourcount", null)
            intent.data = uri
            startActivity(intent)
            dismiss()
        }
    }

    override fun onDetach() {
        super.onDetach()

        if (MyDebug.DLOG) Log.i(TAG, "146, onDetach")
        context = null
    }

    companion object {
        private const val TAG = "PermStorFragm"

        @JvmStatic
        fun newInstance(): PermissionsStorageDialogFragment {
            return PermissionsStorageDialogFragment()
        }
    }

}
