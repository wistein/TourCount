package com.wmstein.tourcount;

/**
 * Global constant to control logcat logging.
 * <p>
 * Should always be set to false in released versions,
 * should only temporarily set to true for debugging.
 * <p>
 * Put back MyDebug.kt to MyDebug.java as in Kotlin2 "object MyDebug" produces Errors when called
 * from java modules.
 * Last edited on 2024-10-21
 */
public class MyDebug
{
    public static boolean dLOG = false; // for release version
//    public static boolean dLOG = true; // for debug version
}
