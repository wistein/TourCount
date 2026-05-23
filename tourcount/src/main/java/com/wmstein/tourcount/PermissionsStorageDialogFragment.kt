package com.wmstein.tourcount

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts
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
 * last edited on 2026-05-19
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
            Log.i(TAG, "39, onCreate")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Check for given storage permission, Android >= 11
        val permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "47, Manifest storage permission >=11: $permission")
        permissionLauncherStorage.launch(permission)
    }

    // Request single permissions in system settings app
    private val permissionLauncherStorage = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            dismiss()
        } else {
            showAppSettingsManageStorageDialog() // accept or deny MANAGE_EXTERNAL_STORAGE permission
        }
    }

    // Query and set missing manage storage permission for API >= 30
    private fun showAppSettingsManageStorageDialog() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "66, Manage storage dialog")

        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", "com.wmstein.tourcount", null)
        intent.data = uri
        startActivity(intent)
        dismiss()
    }

    override fun onDetach() {
        super.onDetach()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "79, onDetach")
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
