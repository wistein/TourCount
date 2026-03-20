package com.wmstein.tourcount.database

/**********************************
 * Definitions for table Section
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-02-24
 */
class Section {
    @JvmField
    var id = 0
    @JvmField
    var name: String? = ""
    @JvmField
    var country: String? = ""
    @JvmField
    var plz: String? = ""
    @JvmField
    var city: String? = ""
    @JvmField
    var place: String? = ""
    @JvmField
    var tmp = 0
    @JvmField
    var wind = 0
    @JvmField
    var clouds = 0
    @JvmField
    var tmp_end = 0
    @JvmField
    var wind_end = 0
    @JvmField
    var clouds_end = 0
    @JvmField
    var date: String? = ""
    @JvmField
    var start_tm: String? = ""
    @JvmField
    var end_tm: String? = ""
    @JvmField
    var notes: String? = ""
    @JvmField
    var b_state: String? = ""
    @JvmField
    var st_locality: String? = ""
}
