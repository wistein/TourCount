package com.wmstein.tourcount

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

/**
 * PermissionsDialogFragment provides the permission handling, which is
 * necessary since Android Marshmallow (M)
 *
 * Original version from RuntimePermissionsExample-master created by tylerjroach on 8/31/16,
 * licensed under the MIT License.
 *
 * Adopted for TourCount by wistein on 2018-06-20,
 * converted to Kotlin on 2023-05-26,
 * last edited on 2023-05-26
 */
class PermissionsDialogFragment : DialogFragment() {
    private var context: Context? = null
    private var listener: PermissionsGrantedCallback? = null
    private var shouldResolve = false
    private var externalGrantNeeded = false
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
            } else {
                //permissions have been accepted
                if (listener != null) {
                    listener!!.permissionCaptureFragment()
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    )
    {
        shouldResolve = true
        for (i in permissions.indices) {
            val permission = permissions[i]
            val grantResult = grantResults[i]
            if (!shouldShowRequestPermissionRationale(permission) && grantResult != PackageManager.PERMISSION_GRANTED) {
                externalGrantNeeded = true
                return
            } else if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
    }

    // deprecated, Todo
    private fun requestNecessaryPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun showAppSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.perm_required))
            .setMessage(getString(R.string.perm_hint) + " " + getString(R.string.perm_hint1))
            .setPositiveButton(getString(R.string.app_settings)) { dialogInterface: DialogInterface?, i: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri =
                    Uri.fromParts("package", requireContext().applicationContext.packageName, null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialogInterface: DialogInterface?, i: Int -> dismiss() }
            .create().show()
    }

    interface PermissionsGrantedCallback {
        fun permissionCaptureFragment()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101

        @JvmStatic
        fun newInstance(): PermissionsDialogFragment {
            return PermissionsDialogFragment()
        }
    }
}