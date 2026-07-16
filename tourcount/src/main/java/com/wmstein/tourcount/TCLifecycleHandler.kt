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
 * Based on [...](https://stackoverflow.com/questions/3667022/)
 * checking-if-an-android-application-is-running-in-the-background/13809991#13809991
 * 
 * Adopted for TourCount by wmstein on 2026-05-11,
 * Last edited on 2026-07-16
 */
class TCLifecycleHandler : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
            "TCLifecycleHandler ","23, Activity created: $activity $started"
        )
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
            "TCLifecycleHandler ","29, Activity destroyed: $activity $started"
        )
    }

    override fun onActivityResumed(activity: Activity) {
        // Reduce activity counts for startActivity(stoIntent) from requestAllFilesAccessPermission
        // in WelcomeActivity
        val activ: String = activity.toString()
        var isWelcomeActivity = false
        if (activ.contains("WelcomeActivity"))
            isWelcomeActivity = true

        if (TourCountApplication.isStorPermReq && isWelcomeActivity) {
            stopped++
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
                "TCLifecycleHandler ","44, Activity resumed, stopped: $activ $stopped"
            )
            TourCountApplication.isStorPermReq = false
        }
    }

    override fun onActivityPaused(activity: Activity) {
        // Increase activity counts for startActivity(stoIntent) from requestAllFilesAccessPermission
        // in WelcomeActivity
        val activ: String = activity.toString()
        var isWelcomeActivity = false
        if (activ.contains("WelcomeActivity"))
            isWelcomeActivity = true

        if (TourCountApplication.isStorPermReq && isWelcomeActivity) {
            started++
            if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
                "TCLifecycleHandler ","61, Activity paused, started: $activ $started"
            )
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStarted(activity: Activity) {
        started++

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
            "TCLifecycleHandler ","73, Activity started: $activity $started"
        )
    }

    override fun onActivityStopped(activity: Activity) {
        stopped++

        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i("TCLifecycleHandler ", "81, Activity stopped: $activity $stopped")
            Log.i("TCLifecycleHandler ", "82, Application is visible: " + (started > stopped))
        }
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
