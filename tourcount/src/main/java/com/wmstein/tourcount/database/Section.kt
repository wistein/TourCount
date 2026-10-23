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
 * Definitions for table Section
 *
 * Created by wmstein on 2016-02-18,
 * last edited in Java on 2022-03-23,
 * converted to Kotlin on 2023-07-05,
 * last edited on 2026-10-19
 **************************************************************************************************/

class Section {
    @JvmField
    var id = 0
    @JvmField
    var name = ""
    @JvmField
    var country = ""
    @JvmField
    var plz = ""
    @JvmField
    var city = ""
    @JvmField
    var place = ""
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
    var date = ""
    @JvmField
    var start_tm = ""
    @JvmField
    var end_tm = ""
    @JvmField
    var notes = ""
    @JvmField
    var b_state = ""
    @JvmField
    var st_locality = ""
}
