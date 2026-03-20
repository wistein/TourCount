package com.wmstein.tourcount.database

/*************************************************
 * Definitions for table Individuals
 * Created by wmstein for TourCount on 2016-04-20
 * Last edited in Java on 2022-03-25,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-02-24
 */
class Individuals {
    @JvmField
    var id = 0
    @JvmField
    var count_id = 0 // deprecated -> code (String)
    @JvmField
    var name: String? = ""
    @JvmField
    var coord_x = 0.0 // latitude
    @JvmField
    var coord_y = 0.0 // longitude
    @JvmField
    var coord_z = 0.0 // height
    @JvmField
    var uncert: String? = ""
    @JvmField
    var date_stamp: String? = ""
    @JvmField
    var time_stamp: String? = ""
    @JvmField
    var locality: String? = ""
    @JvmField
    var sex: String? = ""
    @JvmField
    var stadium: String? = ""
    @JvmField
    var state_1_6 = 0 // takes numbers 0-6 with 0 translated to "-"
    @JvmField
    var notes: String? = ""
    @JvmField
    var icount = 0
    @JvmField
    var icategory = 0 // 1=♂|♀, 2=♂, 3=♀, 4=pupa, 5=larva, 6=egg
    @JvmField
    var code: String? = ""
}
