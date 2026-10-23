package com.wmstein.tourcount

/***************************************************************************************************
 * Copyright © 2016-2026, Wilhelm Stein, Bonn, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use any of its files except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************************************
 *
 * Parts based on RuntimePermissionsExample-master created by tylerjroach, on 8/31/16.
 * Copyright © 2016 tylerjroach.
 *
 * Licensed under the MIT License (MIT):
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ***************************************************************************************************
 *
 * PermissionsLocationDialogFragment provides the permission handling, which
 * is necessary since Android Marshmallow (M) for
 * - ACCESS_COARSE_LOCATION,
 * - ACCESS_FINE_LOCATION
 *
 * Partly adopted for TourCount by wmstein on 2019-02-08,
 * last edited in java on 2024-09-30,
 * converted to Kotlin on 2025-01-22,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class PermissionsLocationDialogFragment : DialogFragment() {
    private var context: Context? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        this.context = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "43, onCreate")

        setStyle(STYLE_NO_TITLE, R.style.PermissionsDialogFragmentStyle)
        isCancelable = false

        // Request foreground location permission
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "51, Manifest location permission: $permission")

        // launch permission request dialog
        permissionLauncherLocation.launch(permission)
    }

    // Request foreground location permissions in system settings dialog
    private val permissionLauncherLocation = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted) {
            dismiss()
        } else {
            showAppSettingsLocationDialog()
        }
    }

    // Query and set missing foreground permissions
    private fun showAppSettingsLocationDialog() {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "72, AppSettingsForegrLocDialog")

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_fine_location_title))
            .setMessage(getString(R.string.dialog_fine_location_message))
            .setPositiveButton(getString(R.string.app_settings))
            { _: DialogInterface?, _: Int ->
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri =
                    Uri.fromParts("package",
                        requireContext().applicationContext.packageName,
                        null)
                intent.data = uri
                requireContext().startActivity(intent)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancelButton)) { _: DialogInterface?, _: Int -> dismiss() }
            .create().show()
    }

    override fun onDetach() {
        super.onDetach()

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i(TAG, "97, onDetach")
        context = null
    }

    companion object {
        private const val TAG = "PermLocFragm"

        @JvmStatic
        fun newInstance(): PermissionsLocationDialogFragment {
            return PermissionsLocationDialogFragment()
        }
    }

}
