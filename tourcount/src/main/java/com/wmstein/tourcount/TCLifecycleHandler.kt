package com.wmstein.tourcount

/***************************************************************************************************
 * Copyright © 2026, Wilhelm Stein, Bonn, Germany
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
 * TCLifecycleHandler controls the state of all activities and checks if your application
 * is in foreground or background.
 * Needed to stop AddrRequestService when app is finished but not yet destroyed and
 * takes care of the special activity handling of the permit dialog of MANAGE_EXTERNAL_STORAGE.
 *
 * Inspired by Cornstalks answer on https://stackoverflow.com/questions/3667022/
 *
 * Checking if an android application is running in the background
 *
 * Adopted from TransektCount for TourCount and converted to Kotlin by wmstein on 2026-05-11,
 * last edited on 2026-10-21.
 **************************************************************************************************/

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log

class TCLifecycleHandler : ActivityLifecycleCallbacks {
    var activityName: String = ""
    var isWelcomeActivity = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    // onActivityStarted counts up
    override fun onActivityStarted(activity: Activity) {
        started++ // counts any activity

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("TCLifecycleHandler ","49, Activities started: $started times")
    }

    // After requestAllFilesAccessPermission in WelcomeActivity, WelcomeActivity resumes,
    //   so here corrects activity counts (adds additional stopped count)
    override fun onActivityResumed(activity: Activity) {
        if (activity.toString().contains("WelcomeActivity")) {
            activityName = "WelcomeActivity"
            isWelcomeActivity = true
        } else {
            activityName = "other"
            isWelcomeActivity = false
        }

        if (TourCountApplication.isStorPermReq && isWelcomeActivity) {
            stopped++ // counts only for isStorPermReq in WelcomeActivity

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i("TCLifecycleHandler ","67, $activityName resumed, activities stopped: $stopped times")

            TourCountApplication.isStorPermReq = false
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    // requestAllFilesAccessPermission in WelcomeActivity pauses WelcomeActivity,
    //   so here corrects activity counts (adds additional started count)
    override fun onActivityPaused(activity: Activity) {
        if (activity.toString().contains("WelcomeActivity")) {
            activityName = "WelcomeActivity"
            isWelcomeActivity = true
        } else {
            activityName = "other"
            isWelcomeActivity = false
        }

        if (TourCountApplication.isStorPermReq && isWelcomeActivity) {
            started++ // counts only for isStorPermReq in WelcomeActivity

            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
                Log.i("TCLifecycleHandler ","91, $activityName paused, activities started: $started times")
        }
    }

    // onActivityStopped counts down
    override fun onActivityStopped(activity: Activity) {
        stopped++ // counts any activity

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i("TCLifecycleHandler ", "100, Activities stopped: $stopped times")
            Log.i("TCLifecycleHandler ", "101, Application is visible: $isApplicationVisible")
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    companion object {
        // Increment/decrement the variables 'started' and 'stopped' by all activities
        private var started = 0
        private var stopped = 0

        @JvmStatic
        val isApplicationVisible: Boolean
            // Static function to check if the application is in foreground or background
            get() = started > stopped
    }

}
