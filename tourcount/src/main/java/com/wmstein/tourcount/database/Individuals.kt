package com.wmstein.tourcount.database

/*************************************************
 * Definitions for table Individuals
 * Created by wmstein for TourCount on 2016-04-20
 * Last edited in Java on 2022-03-25,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-05-26
 */
class Individuals {
    @JvmField
    var id = 0
    @JvmField
    var count_id = 0 // deprecated -> code (String)
    @JvmField
    var name = ""
    @JvmField
    var coord_x = 0.0 // latitude
    @JvmField
    var coord_y = 0.0 // longitude
    @JvmField
    var coord_z = 0.0 // height
    @JvmField
    var uncert = ""
    @JvmField
    var date_stamp = ""
    @JvmField
    var time_stamp = ""
    @JvmField
    var locality = ""
    @JvmField
    var sex = ""
    @JvmField
    var stadium = ""
    @JvmField
    var state_1_6 = 0 // takes numbers 0-6 with 0 translated to "-"
    @JvmField
    var notes = ""
    @JvmField
    var icount = 0
    @JvmField
    var icategory = 0 // 1=♂|♀, 2=♂, 3=♀, 4=pupa, 5=larva, 6=egg
    @JvmField
    var code = ""
}
