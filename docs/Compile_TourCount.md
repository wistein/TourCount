# Compile Guide for TourCount

## Environment for the Project
Windows 64-bit with adb driver

Android Smartphone with high resolution screen

## Dependencies
- Android Studio (current version)

## Android Studio Components
Android SDK with
- Android Platforms: 6.0, 7.1, 8.1, 10, 11
- SDK Tools: Android Emulator, Android SDK Platform-Tools, Android SDK Tools, Android Support Library, Google USB Driver, Intel x86 Emulator Accelerator, Android Support Repository, Google Repository
- Plugins: .ignore, Android Support, CVS Integration, EditorConfig, Git Integration, GitHub, Gradle, Groovy, hg4idea, I18n for Java, IntelliLang, Java Bytecode Decompiler, JUnit, Properties Support, SDK Updater, Subversion Integration, Task Management, Terminal 

### Java SE 17.0.2 64-bit (or current version)

### build.gradle
    // Compiled with SDK Ver. 32 but for targetSdkVersion 29!
    //  targetSdkVersion > 29 prevents installation from "unknown sources",
    //  and so inhibits me from installing and testing my own compilation under Android 11.
    //  androidx.appcompat:appcompat > 1.4.2 also demands for a higher API-level.
    //  Compiling with a higher API-level is only required for Google Play which is not used.
    //  Related compiling errors can be ignored and don't affect functionality!
    //
    // 'com.google.android.material:material:1.6.1' is still used as version 1.7.0 produces 
    //  a duplicate class error
    //
- compileSdkVersion 32
- targetSdkVersion 29
- External Libraries (part of or imported by Android Studio)
  implementation 'androidx.appcompat:appcompat:1.4.2'
  implementation 'androidx.legacy:legacy-support-v4:1.0.0'
  implementation 'androidx.preference:preference:1.2.0'
  implementation 'com.google.android.material:material:1.6.1'

## Start the Project
Get the project source by downloading the master.zip.

Extract it to a directory "TourCount".

Load the directory as a project in Android Studio.

Set up your Android Studio environment regarding compiling key, apk directory and GitHub destination.
