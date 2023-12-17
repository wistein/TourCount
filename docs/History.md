# History

#### TourCount Copyright (C) 2016-2023, Wilhelm Stein

#### Version 3.4.5 (2023-12-15)
- SQlite DB: Table and column names 'temp' changed for reserved term conflict
- Code adaptations for Android 13
- Code improvements

#### Version 3.4.4 (2023-12-05)
- Compiled with SDK Ver. 34 for targetSdkVersion 33
- Gradle Plugin upgraded -> 8.1.1
- Import of DB files from Documents/TourCount folder
- Export of DB and CSV files into Documents/TourCount folder
- Results page improved
- More species added
- Missing pictures supplemented
- Docs updated
- Code improvements
- Bugs fixed

#### Version 3.4.2 (2023-07-30)
- More missing species added
- Docs updated

#### Version 3.4.0 (2023-07-13)
- Landscape mode removed (as hardly used, even on tablet)
- Some more modules translated from Java to Kotlin
- Code adaptations for Kotlin
- Some code deprecations resolved
- Code improvements
- Docs updated

#### Version 3.3.5 (2023-06-19)
- Compiled with SDK Ver. 33 for targetSdkVersion 33
- Gradle Plugin upgraded -> 8.0.2
- Gradle upgraded -> 8.0
- Most deprecated functions replaced
- .csv-files exported to Documents/TourCount/ (allows access by other apps)
- Option to vibrate, short for counting-up and longer for counting-down
- Counting sound deeper when counting down
- Some more code improvements
- Sourcecode partly translated to Kotlin
- One more species added
- Minimal Android Version 7.1 (Nougat)
- Copyright texts updated

#### Version 3.3.3 (2022-10-25)
- Gradle Plugin upgraded -> 7.3.1
- Gradle upgraded -> 7.4
- Compiled with SDK Ver. 32 for targetSdkVersion 31
- Many pictures of species improved to show recto and verso sides
- Some more species with pictures added
- Bugfix in arrays.xml

#### Version 3.2.9 (2022-05-21)
- Project adaption for Android Studio 2021.2.1 and Gradle 7.3.3
- Compiled for SDK version 31 (needed for current libs)
- Portrait/landscape handling improved
- Location background permission removed
- Documents and example-DBs updated
- Copyright and license texts updated
- Some more moths species with pictures added
- State info for pupa, caterpillar or egg removed
- Code improvements

#### Version 3.2.6 (2021-08-22)
- Project adaption for Android Studio 2020.3.1 and Gradle 7.0.1
- Layout and function of Edit species list improved
- Some outdated butterfly names updated (within app and example DBs)
- Outdated code names updated
- Permission handling for Access Background Location
- Fix for broken loading picture function since Gradle 6.5
- Some missing butterfly pictures added
- Some pictures improved

#### Version 3.2.3 (2020-09-09)
- Project adaption for Android Studio 4.0.1 and Gradle 6.1.1
- Location service active in major activities
- More butterfly pictures integrated
- Integrated initial list of species enlarged
- Background picture better scaled for long displays
- Code for restricted Storage Access adapted for Android 10
- Data files now in app specific directory 
- Code for Preferences adapted for Android 10
- Use of AndroidX libraries
- Strings cleared up
- Help text updated
- Many minor code refinements
- Docs updated
- Location service fix for coarse location
- Bug fix in button sound

#### Version 3.1.5 (2020-02-26)
- Project adaption for Android Studio 3.6 and Gradle 5.6.4
- Code optimized for storage and power efficiency
- App icon adapted to better match Android 9 screen
- Undercutting of species names in results page corrected
- Minor text improvements
- Changes in AndroidManifest.xml and build.gradle

#### Version 3.1.2 (2019-08-04)
- Project adaption for Android Studio 3.4.1 and Gradle 5.1.1
- Added/changed butterfly pictures
- Exported csv-file with total of different species counted
- Minimal changes to starting page
- Integrated list of many European species
- Instead of manual input, add further species to your species list by scroll-down selection from a comprehensive list that contains only species not contained yet
- Bugfix in database handling

#### Version 3.0.8 (2019-03-25)
- Project adaption for Android Studio 3.3.2 and Gradle 4.10.1
- Minimum Android version changed to 4.4 KitKat
- Database structure supplemented for local butterfly names
- Example databases adapted for local butterfly names
- Exported results include local butterfly names
- Additional butterfly pictures
- Some pictures edited to show both sides of wings
- Cosmetic changes to several app pages
- Docs updated
- Some code cleaning
- Fix for crash when species selector is rapidly repeatedly pressed

#### Version 3.0.5 (2018-09-20)
- Project changes for Android Studio 3.1.2 and Gradle 3.1.2
- Location retrieval function more reliable, changed to service
- Additional manual input for date, start-time and stop-time with long press
- Additional butterfly pictures
- Where applicable toasts replaced with snackbars
- Code improvements
- Fixed GPS height correction not working
- Fix for crash in permission handling of Android >= Marshmallow
- Fix for crash in "Edit Species list" functionality
- Other bug fixes

#### Version 3.0.1 (2018-04-03)
- Project changes for Android Studio 3.1, Gradle 3.1.0 and SDK 27 (Android 8.1)
- Unnecessary permission WRITE_SETTINGS removed
- Redesign of counting page layout with scroll-down selection od species
- Separate counters for ♂ or ♀, ♂, ♀, pupa, larva, egg
- Disabled multi-window-mode (Android 7+) for incompatible pull-down-menu (Spinner)
- Correct handling of subtraction for categories of species and individuals
- Database version change 2 to 3 (takes care of additional counters)
- Automatic conversion of existing databases to new version while loading
- First use now creates a direct usable internal DB with common european species
- Layout of starting page updated
- Adapted results page layout
- More butterfly pictures added
- Documentation adapted for Ver. 3.0
- Bug fixes

#### Version 2.4.6 (2018-03-19)
- Source Project restructured
- OSM Reverse Geocoding by Nominatim Service of OpenStreetMap for meta data and individual location
- Added a camera button in counting menu
- Results page improved (average coordinates, listing of individual counts with data)
- Diverse improvements of details
- Obsolete functions eliminated
- Added pictures of species for Canaries and Madeira
- Added additional prepared databases for Canaries and Madeira in <a href="https://github.com/wistein/TourCount/tree/master/docs" target="_blank">https://github.com/wistein/TourCount/tree/master/docs</a>
- Documents: German Introductary presentation added
- Documents updated
- Bug fixes

#### Version 2.3.0 (2017-09-27)
- Code adapted and compiled for Android 7.1
- Correct height with geoid offset from EarthGravitationalModel EGM96
- Write system log only in MyDebug mode
- Added links in App Info and Help pages
- Improved layout of results page
- Added database version control to be compatible with old database structure prior version 2.1.4
- Added Option to switch between portrait and landscape mode
- Show correct headline immediately after loading/resetting DB
- CSV export now shows No. of species according to recorded sex and stadium
- CSV export now suppresses zeros for empty counts
- Finishing individual input now has button sound
- Database structure adapted to enhanced individuals data export 
- Press back button twice to exit
- Additional support for coarse location from Wifi and Network
- Results page with totals
- Additional butterfly pictures
- Minor string improvements
- Fix crash when counting using a database of old structure prior version 2.1.4
- Bug fixes and minor code improvements

#### Version 2.1.4 (2016-12-01)
- Added pictures of species on counting and results page
- Default background picture different from that of TransektCount app
- Icons on settings page
- Icons in menus
- Codes for species added
- Option added: Sorting of species by names or codes
- Results screen layout improved
- Multiple counts on individuals page
- Option counting screen for lefties
- Exportable csv-table improved
- Internal DB structure adapted
- Switch screen off on counting page as long as phone gets pocketed
- Option "Screen bright" in Preferences
- File import dialog for existing DB
- Code cleaned, unused functions removed
- Permission handling for Android M
- Records geogr. altitude 
- DB structure adapted and improved
- Input masks improved
- Code improvement
- Bug fixes

#### Version 2.0.2 (2016-05-27)
- Sorted csv export
- Limited to 1 counting list
- Code improved
- Functionality of individuals data handling completed
- Results pages and export to CSV adapted and completed

#### Version 1.0.2 (2016-05-18)
- Count buttons activate an individuals page to get data per individual count
- Table "individuals" with time, coordinates, gender, stadium, remarks added to DB
- Meta data (date, time) added
- Plausi control for meta data
- Prepared for GPS recording
    
#### Version 1.0.0 (2016-04-18)
##### 1. Release.
TourCount derived from TransektCount whose basic program structure and counting functionality originally derived partly from BeeCount by knirirr
(https://github.com/knirirr/BeeCount.git)

Main features of 1. release comprise
- "Reset Data" function
- App runs always in portrait mode
- Exports results in csv-format 
- View for counting results
- Input page for meta data
- Detailed Help page
- Base-DB-Import/Export: Raw Database 'tourcount0.db'
- DB-Export: Database 'tourcount_yyyymmdd-hhmmss.db'
- DB-Results export: Excel file 'tourcount_yyyymmdd-hhmmss.csv'
- In German and English language 
- Text and structure adaptation for butterfly registration in the field
- Modified AutoFitText.java, original code from author: pheuschk (open source code from stackoverflow)
- Modified ChangeLog.java original code from author: Karsten Priegnitz (open source code from code.google.com)
- Modified CSVWriter.java original code copyright 2015 Bytecode Pty Ltd. (Apache License 2.0)
- Modified File Chooser dialog based on android-file-chooser, 2011, Google Code Archive, GNU GPL v3

