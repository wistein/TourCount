package com.wmstein.tourcount

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.util.Log

/***************************************************************************************
 * TCLifecycleHandler controls the state of all activities and check if your application
 * is in foreground or background.
 * Needed to stop AddrRequestService when app is finished but not yet destroyed.
 * 
 * 
 * Based on [...](https://stackoverflow.com/questions/3667022/)
 * checking-if-an-android-application-is-running-in-the-background/13809991#13809991
 * 
 * 
 * Adopted for TourCount by wmstein on 2026-05-11,
 * Last edited on 2026-05-14
 */
class TCLifecycleHandler : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityStarted(activity: Activity) {
        started++
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) Log.i(
            "TCLifecycleHandler ","Application started: $started"
        )
    }

    override fun onActivityStopped(activity: Activity) {
        stopped++
        if (IsRunningOnEmulator.DLOG || BuildConfig.DEBUG) {
            Log.i("TCLifecycleHandler ", "Application stopped: $stopped")
            Log.i("TCLifecycleHandler ", "Application is visible: " + (started > stopped))
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
