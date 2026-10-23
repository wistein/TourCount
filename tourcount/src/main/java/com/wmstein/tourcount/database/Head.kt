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
 * Interface for the data record of transect Table Head
 *
 * Created by wmstein on 2016-03-31,
 * last edited in Java on 2022-03-23,
 * converted to Kotlin on 2026-02-24,
 * last edited on 2026-10-21.
 **************************************************************************************************/

// Data definitions for table 'Head'
class Head {
    @JvmField
    var id = 0
    @JvmField
    var observer = ""
    @JvmField
    var datalanguage = ""
}
