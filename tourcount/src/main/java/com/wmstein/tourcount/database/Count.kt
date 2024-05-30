package com.wmstein.tourcount.database

/*************************************************
 * Definitions and basic functions for table Count
 * Basic structure by milo on 05/05/2014.
 * Created by wmstein on 2016-02-18,
 * last change in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2024-04-14
 */
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
    var name: String? = null
    @JvmField
    var code: String? = null
    @JvmField
    var notes: String? = null
    @JvmField
    var name_g: String? = null

    fun increase_f1i(): Int {
        count_f1i = count_f1i + 1
        return count_f1i
    }

    fun increase_f2i(): Int {
        count_f2i = count_f2i + 1
        return count_f2i
    }

    fun increase_f3i(): Int {
        count_f3i = count_f3i + 1
        return count_f3i
    }

    fun increase_pi(): Int {
        count_pi = count_pi + 1
        return count_pi
    }

    fun increase_li(): Int {
        count_li = count_li + 1
        return count_li
    }

    fun increase_ei(): Int {
        count_ei = count_ei + 1
        return count_ei
    }

    // decreases
    fun safe_decrease_f1i(): Int {
        if (count_f1i > 0) {
            count_f1i = count_f1i - 1
        }
        return count_f1i
    }

    fun safe_decrease_f2i(): Int {
        if (count_f2i > 0) {
            count_f2i = count_f2i - 1
        }
        return count_f2i
    }

    fun safe_decrease_f3i(): Int {
        if (count_f3i > 0) {
            count_f3i = count_f3i - 1
        }
        return count_f3i
    }

    fun safe_decrease_pi(): Int {
        if (count_pi > 0) {
            count_pi = count_pi - 1
        }
        return count_pi
    }

    fun safe_decrease_li(): Int {
        if (count_li > 0) {
            count_li = count_li - 1
        }
        return count_li
    }

    fun safe_decrease_ei(): Int {
        if (count_ei > 0) {
            count_ei = count_ei - 1
        }
        return count_ei
    }

}
