package com.wmstein.tourcount.database

/***********************************
 * Definitions for table Temp
 * (unfortunately 'Temp' is misinterpreted as error but works as expected)
 * Created by wmstein on 2016-05-15,
 * last edited in Java on 2022-03-23,
 * converted to Kotlin on 2026-02-24
 */
class Temp {
    @JvmField
    var id = 0
    @JvmField
    var temp_loc: String? = ""  // last locality found or entered
    @JvmField
    var temp_cnt = 0  // for future use
}
