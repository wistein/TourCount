# Compile Guide for TourCount

## Environment for the Project
Windows 64-bit with adb driver

Android Smartphone with high resolution screen

## Dependencies
- Android Studio (current version)

## Android Studio Components
Android SDK with
- Android Platforms: 9, 10, 11, 12
- SDK Tools: Android Emulator, Android SDK Platform-Tools, Android SDK Tools, Android Support Library, Google USB Driver, Intel x86 Emulator Accelerator, Android Support Repository, Google Repository
- Plugins: .ignore, Android Support, CVS Integration, EditorConfig, Git Integration, GitHub, Gradle, Groovy, hg4idea, I18n for Java, IntelliLang, Java Bytecode Decompiler, JUnit, Properties Support, SDK Updater, Subversion Integration, Task Management, Terminal 

### Java SE 64-bit (current version)

### build.gradle
- Compiled with SDK Ver. 32 but for targetSdkVersion 31.
- External Libraries:
  implementation 'androidx.appcompat:appcompat:x.y.z' (current version)
  implementation 'androidx.legacy:legacy-support-v4:x.y.z' (current version)
  implementation 'androidx.preference:preference:x.y.z' (current version)
  implementation 'com.google.android.material:material:x.y.z' (current version)
  implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:x.y.z' (current version)

## Start the Project
Get the project source by downloading the master.zip.

Extract it to a directory "TourCount".

Load the directory as a project in Android Studio.

Set up your Android Studio environment regarding compiling key, apk directory and GitHub destination.
