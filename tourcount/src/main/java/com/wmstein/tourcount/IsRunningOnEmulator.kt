package com.wmstein.tourcount

import android.os.Build

/**
 * Global boolean value DLOG to control additional logcat logging on emulator.
 *
 * Tries to determine if the target is a virtual emulator device in Android Studio.
 * In that case it sets DLOG = true
 *
 * This version created on 2025-10-20,
 * last edited on 2025-10-20
 */
object IsRunningOnEmulator {
    var product: String = Build.PRODUCT
    var isEmulator: Boolean =
        (product.contains("sdk") ||
                product.contains("google_sdk") ||
                product.contains("Emulator"))

    @JvmField
    var DLOG: Boolean = isEmulator
}
