# Compile Guide for TourCount

## Environment for the Project
Windows 64-bit with adb driver or
Linux 64-bit

Android Smartphone with high resolution screen (Android 7.1 or higher)

## Dependencies
- Android Studio (current version)

## Android Studio Components
Android SDK with
- Android Platforms: 9, 10, 11, 12, 13, 14, 15
- SDK Tools: Android Emulator, Android SDK Platform-Tools, Android SDK Tools, Android Support Library, Google USB Driver, Intel x86 Emulator Accelerator, Android Support Repository, Google Repository
- Plugins: .ignore, Android Support, CVS Integration, EditorConfig, Git Integration, GitHub, Gradle, Groovy, hg4idea, I18n for Java, IntelliLang, Java Bytecode Decompiler, JUnit, Properties Support, SDK Updater, Subversion Integration, Task Management, Terminal 

### Java SE 64-bit (current version)

### build.gradle (TourCount)
- buildscript:
  ext:
    kotlin_version = '2.0.20' (or higher)
  repositories:
    mavenCentral()
    google()
  dependencies:
    classpath 'com.android.tools.build:gradle:8.7.3' (or higher)
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

### build.gradle (tourcount)
- Compiled with SDK Ver. 35 for targetSdk 34 and minSdk 25.
- JavaVersion.VERSION_17 (or current version)
- External Libraries:
  implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
  implementation 'androidx.appcompat:appcompat:x.y.z' (current version)
  implementation 'androidx.legacy:legacy-support-v4:x.y.z' (current version)
  implementation 'com.google.android.material:material:x.y.z' (current version)
  implementation 'androidx.preference:preference-ktx:x.y.z' (current version)
  implementation 'androidx.work:work-runtime:x.y.z' (current version)
  implementation 'androidx.core:core-ktx:x.y.z' (current version)

## Start the Project
Get the project source by downloading the master.zip.

Extract it to a directory "TourCount".

Load the directory as a project in Android Studio.

Set up your Android Studio environment regarding compiling key, apk directory and GitHub destination.
