package com.wmstein.tourcount

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log

/**********************************************************************************************
 * TCLifecycleHandler controls the state of all activities and checks if your application
 * is in foreground or background.
 * Needed to stop AddrRequestService when app is finished but not yet destroyed and
 * takes care of the special activity handling of the permit dialog of MANAGE_EXTERNAL_STORAGE.
 * 
 * Inspired by https://stackoverflow.com/questions/3667022/
 * checking-if-an-android-application-is-running-in-the-background/13809991#13809991
 * 
 * Adopted for TourCount in Kotlin by wmstein on 2026-05-11,
 * Last edited on 2026-07-18
 */
class TCLifecycleHandler : ActivityLifecycleCallbacks {
    var activityName: String = ""
    var isWelcomeActivity = false

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    // onActivityStarted counts up
    override fun onActivityStarted(activity: Activity) {
        started++ // counts any activity

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG)
            Log.i("TCLifecycleHandler ","32, Activities started: $started times")
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
                Log.i("TCLifecycleHandler ","50, $activityName resumed, activities stopped: $stopped times")

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
                Log.i("TCLifecycleHandler ","74, $activityName paused, activities started: $started times")
        }
    }

    // onActivityStopped counts down
    override fun onActivityStopped(activity: Activity) {
        stopped++ // counts any activity

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i("TCLifecycleHandler ", "83, Activities stopped: $stopped times")
            Log.i("TCLifecycleHandler ", "84, Application is visible: $isApplicationVisible")
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
