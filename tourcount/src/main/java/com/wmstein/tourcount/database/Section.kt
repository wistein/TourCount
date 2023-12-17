package com.wmstein.tourcount.database

/**********************************
 * Definitions for table Section
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2023-12-15.
 */
class Section {
    @JvmField
    var id = 0
    @JvmField
    var name: String? = null
    @JvmField
    var country: String? = null
    @JvmField
    var plz: String? = null
    @JvmField
    var city: String? = null
    @JvmField
    var place: String? = null
    @JvmField
    var tmp = 0
    @JvmField
    var wind = 0
    @JvmField
    var clouds = 0
    @JvmField
    var date: String? = null
    @JvmField
    var start_tm: String? = null
    @JvmField
    var end_tm: String? = null
    @JvmField
    var notes: String? = null
}