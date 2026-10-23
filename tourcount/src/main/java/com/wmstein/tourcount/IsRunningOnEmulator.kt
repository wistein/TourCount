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
 * Provides global boolean value DLOG to control additional logcat logging on emulator.
 *
 * Tries to determine if the target is a virtual emulator device in Android Studio.
 * In that case it sets DLOG = true
 *
 * Created by wmstein on 2025-10-20,
 * last edited on 2026-10-19
 **************************************************************************************************/

import android.os.Build

object IsRunningOnEmulator {
    var product: String = Build.PRODUCT
    var isEmulator: Boolean =
        (product.contains("sdk") ||
                product.contains("google_sdk") ||
                product.contains("Emulator"))

    @JvmField
    var DLOG: Boolean = isEmulator

}
