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
 * Adopted for TourCount by wmstein on 2018-06-20,
 * last edited in java on 2020-04-17,
 * converted to Kotlin on 2023-05-26,
 * last edited on 2026-03-20
 */
class PermissionsStorageDialogFragment : DialogFragment() {
    private var context: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "41, onCreate")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Check for given storage permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { //  (Android <11)
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "50, Manifest storage permission <11: $permission")
            permissionLauncherStorage.launch(permission)
        } else { // Android >= 11
            val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i(TAG, "55, Manifest storage permission >10: $permission")
            permissionLauncherStorage.launch(permission)
        }
    }

    // Request single permissions in system settings app
    private val permissionLauncherStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            dismiss()
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                showAppSettingsStorageDialog() // accept or deny WRITE_EXTERNAL_STORAGE permission
            }
            else {
                showAppSettingsManageStorageDialog() // accept or deny MANAGE_EXTERNAL_STORAGE permission
            }
        }
    }

    // Query and set missing external storage permission
    private fun showAppSettingsStorageDialog() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.d(TAG, "79, External storage dialog")
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
            .setNegativeButton(getString(R.string.cancelButton))
            { _: DialogInterface?, _: Int -> 
                dismiss()
            }
            .create().show()
    }

    // Query and set missing manage storage permission for API >= 30
    private fun showAppSettingsManageStorageDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.d(TAG, "102, Manage storage dialog")

            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", "com.wmstein.tourcount", null)
            intent.data = uri
            startActivity(intent)
            dismiss()
        }
    }

    override fun onDetach() {
        super.onDetach()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "116, onDetach")
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
