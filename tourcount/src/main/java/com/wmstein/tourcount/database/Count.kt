package com.wmstein.tourcount.database

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
 * Definitions and basic functions for table 'Count'
 *
 * Basic structure based on Count.java by Milo Thurston (knirirr) crreated for BeeCount
 * on 2014-05-05, licensed under the Apache License, Version 2.0.
 *
 * Created by wmstein on 2016-02-18,
 * last change in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-10-21
 **************************************************************************************************/

class Count {
    @JvmField
    var id = 0
    @JvmField
    var count_f1i = 0
    @JvmField
    var count_f2i = 0
    @JvmField
    var count_f3i = 0
    @JvmField
    var count_pi = 0
    @JvmField
    var count_li = 0
    @JvmField
    var count_ei = 0
    @JvmField
    var name = ""
    @JvmField
    var code = ""
    @JvmField
    var notes = ""
    @JvmField
    var name_g = ""

    fun increaseF1i(): Int {
        count_f1i += 1
        return count_f1i
    }

    fun increaseF2i(): Int {
        count_f2i += 1
        return count_f2i
    }

    fun increaseF3i(): Int {
        count_f3i += 1
        return count_f3i
    }

    fun increasePi(): Int {
        count_pi += 1
        return count_pi
    }

    fun increaseLi(): Int {
        count_li += 1
        return count_li
    }

    fun increaseEi(): Int {
        count_ei += 1
        return count_ei
    }

    // decreases
    fun safeDecreaseF1i(): Int {
        if (count_f1i > 0) {
            count_f1i -= 1
        }
        return count_f1i
    }

    fun safeDecreaseF2i(): Int {
        if (count_f2i > 0) {
            count_f2i -= 1
        }
        return count_f2i
    }

    fun safeDecreaseF3i(): Int {
        if (count_f3i > 0) {
            count_f3i -= 1
        }
        return count_f3i
    }

    fun safeDecreasePi(): Int {
        if (count_pi > 0) {
            count_pi -= 1
        }
        return count_pi
    }

    fun safeDecreaseLi(): Int {
        if (count_li > 0) {
            count_li -= 1
        }
        return count_li
    }

    fun safeDecreaseEi(): Int {
        if (count_ei > 0) {
            count_ei -= 1
        }
        return count_ei
    }

}
