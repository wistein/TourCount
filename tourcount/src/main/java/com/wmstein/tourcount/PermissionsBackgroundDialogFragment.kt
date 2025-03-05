package com.wmstein.tourcount

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**********************************************************************
 * PermissionsBackgroundDialogFragment provides the permission handling, 
 * which is necessary since Android Android Q for
 * - ACCESS_BACKGROUND_LOCATION
 *
 * Based on RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 *
 * Adopted for TourCount in Kotlin by wistein on 2025-02-22,
 * last edited on 2025-02-22
 */
class PermissionsBackgroundDialogFragment : DialogFragment() {
    private var context: Context? = null

    private var externalGrantNeeded = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (MyDebug.DLOG) Log.i(TAG, "38, onCreate")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Request background location permission
        requestBackgroundPermission()
    }
    // End of onCreate()

    // Solution for optional background location with single permission launcher
    private fun requestBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

            //launcher permission request dialog
            permissionLauncherBackground.launch(permission)
        }
    }

    // Request single permission ACCESS_BACKGROUND_LOCATION in system settings dialog
    private val permissionLauncherBackground = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            externalGrantNeeded = false
            if (MyDebug.DLOG) Log.i(TAG, "65, permissionLauncherBackground granted: $isGranted")
            dismiss()
        } else {
            externalGrantNeeded = true
            showAppSettingsBackgroundDialog()
        }
    }

    // Inform about optional background permission when denied
    private fun showAppSettingsBackgroundDialog() {
        if (MyDebug.DLOG) Log.i(TAG, "75, AppSettingsBackgrDlg")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_background_loc_title))
            .setMessage(getString(R.string.dialog_background_loc_message) + " "
                    + getString(R.string.grant_perm_later))
            .setPositiveButton("Ok")
            { _: DialogInterface?, _: Int ->
                dismiss()
            }
            .create().show()
    }

    override fun onDetach() {
        super.onDetach()

        if (MyDebug.DLOG) Log.i(TAG, "91, onDetach")

        context = null
    }

    companion object {
        private const val TAG = "PermBackgrndDlgFragm"

        @JvmStatic
        fun newInstance(): PermissionsBackgroundDialogFragment {
            return PermissionsBackgroundDialogFragment()
        }
    }

}
