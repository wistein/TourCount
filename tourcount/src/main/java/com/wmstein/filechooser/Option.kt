package com.wmstein.filechooser

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
 * Option.kt is part of filechooser.
 * It will be called within AdvFileChooser.
 *
 * Based on android-file-chooser, 2011, Google Code Archiv, under GNU GPL v3 license.
 *
 * Adopted by wmstein on 2016-06-18,
 * last change in Java on 2021-01-26,
 * converted to Kotlin on 2023-10-19
 * last edited on 2026-10-20.
 **************************************************************************************************/

import java.util.Locale

class Option(val name: String?, val data: String, val path: String, val isBack: Boolean) :
    Comparable<Option> {

    override fun compareTo(other: Option): Int {
        return name?.lowercase(Locale.getDefault())?.compareTo(other.name!!
            .lowercase(Locale.getDefault()))?: throw IllegalArgumentException()
    }

}
