# History

#### TourCount Copyright © 2016-2025, Wilhelm Stein, Bonn, Germany

#### Version 3.6.7 (2025-07-20)
- Compiled for Android 16 (SDK 36)
- Help and License info with new design
- App info with background adjusted to new design
- Adaptive app icon added for Android 16 (prevents white frame)
- Some code and layout cleaning
- Pictures of Ennomos quercinaria and Eilema lurideola added
- Docs updated
- Bugfixes

#### Version 3.6.6 (2025-07-02)
- Vibrator will only be used if Android Version is >= 8
- Code adapted for Android 15 for now forced edge-to-edge layout
- Some page layouts adapted for edge-to-edge layout
- All Snackbars (pop-up notifications) changed to colored Toasts
- Use dark navigation bar for TourCount when System setting "Dark theme" is off
- Filename structure of exported data files changed
- Filename of exported species list with language indicator (de or en)
- Some code cleaning
- Bugfixes

#### Version 3.6.5 (2025-05-18)
- Option to set the proximity Wakelock sensitivity (if supported by device)
- Corrected message when there is no suitable file for import
- Export to a Basic DB now writes the tour name into the filename
- Import of a Basic DB with file selection
- Optional button vibration fixed for Android Version > 11
- Some other bugfixes
- Code, data and layout improvements
- Docs updated

#### Version 3.6.4 (2025-03-25)
- Export of the current species list for import by TransektCount and TourCount
- Some text updates and corrections
- Bugfix
- Docs updated

#### Version 3.6.3 (2025-03-17)
- Added pictures of Yponameuta melinellus, Agriphila geniculea, Phigalia pilosaria, Idaea rusticata, Xanthorhoe fluctuata, Eupithecia vulgata, Eileme caniola
- Added export of current species list for import in TransektCount
- Added import of an exported TransektCount species list
- Update Gradle -> 8.11.1
- Update AGP dependency -> 8.9.0
- Update kotlin -> 2.1.0
- Docs updated

#### Version 3.6.2 (2025-03-06)
- Permission handling revised and adapted
- Location permission option 'Allow all the time' added for Android >= 10 (Q)
- Location attributes enhanced and adapted to modified Nominatim attributes
- Database structure adapted to new location attributes
- Backpress logic on starting page improved
- Bugfix for using default camera as only camera
- Bugfix in RetrieveAddrWorker
- Deprecated functions replaced
- Update Gradle -> 8.10.2
- Update AGP dependency -> 8.8.2
- Docs updated

#### Version 3.6.0 (2024-12-17)
- Option to preselect species by initial letters for editing them in the counting list
- App gets completely removed from memory when finished
- Code improvements and Kotlin code adapted to Kotlin 2
- Bugfixes
- Update Gradle -> 8.9
- Update AGP dependency -> 8.7.1
- Docs updated

#### Version 3.5.1 (2024-10-14)
- Filenames of exported current DB and csv table files include tour name
- Option to preselect species names by initial letters for adding them to the counting list
- Option to preselect species names by initial letters for deleting them from the counting list
- Bugfixes
- Docs updated

#### Version 3.5.0 (2024-08-25)
- Separate views for editing, deleting and adding species
- Checkboxes for bulk deleting of species from the counting list
- Checkboxes for bulk adding of further species to the counting list
- DB structure changed, DB version increased to 7 (side effect: deletes individuals count data when importing an old DB)
- Example DBs updated to version 7 structure
- Docs updated

#### Version 3.4.9 (2024-07-23)
- Obsolete 'Save' button removed from View of 'Edit Species List'
- Extra sort option for results and csv-output (names or codes)
- In csv-export individuals sorted by recorded time
- Licenses text separated with its own menu entry
- Bug fixed: Deleting a species deletes related individual counts also
- Docs updated

#### Version 3.4.8 (2024-07-02)
- Views of 'Edit Species List' and 'Add Species' improved
- Checkboxes for bulk appending of further species to the counting list in 'Add Species'
- Some code improvements
- Deprecated function replaced
- Bugs fixed
- Docs updated

#### Version 3.4.7 (2024-05-28)
- Headline for File Selection screen added
- Preferences view enhanced with headline
- Preferences show their states in menu
- Menu option 'Check duplicates' eliminated (checks always)
- Additional fields for registration of temperature, wind and clouds at end of a tour
- CSV-export layout adopted for new registered values
- Some code and layout improvements
- Bugs fixed
- Docs updated

#### Version 3.4.6 (2024-02-26)
- Code adaptations for Android 14
- More pictures of (Sardinian) species added
- Some local species names added
- Bugs fixed
- Docs updated

#### Version 3.4.5 (2023-12-15)
- SQLite DB: Table and column names 'temp' changed for reserved term conflict
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
- Code adaptations for Android 13

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
- Bugfix in button sound

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
- Bugfixes

#### Version 2.4.6 (2018-03-19)
- Source Project restructured
- OSM Reverse Geocoding by Nominatim Service of OpenStreetMap for meta data and individual location
- Added a camera button in counting menu
- Results page improved (average coordinates, listing of individual counts with data)
- Diverse improvements of details
- Obsolete functions eliminated
- Added pictures of species for Canaries and Madeira
- Added additional prepared databases for Canaries and Madeira in <a href="https://github.com/wistein/TourCount/tree/master/docs" target="_blank">https://github.com/wistein/TourCount/tree/master/docs</a>
- Documents: German Introductory presentation added
- Documents updated
- Bugfixes

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
- Bugfixes and minor code improvements

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
- Records geographic altitude
- DB structure adapted and improved
- Input masks improved
- Code improvement
- Bugfixes

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
TourCount derived from TransektCount 1.2.0 whose basic program structure and counting functionality originally derived partly from BeeCount by knirirr
(https://github.com/knirirr/BeeCount.git)
- Text and structure adaptation for butterfly registration in the field

Other sources:
- Modified AutoFitText.java, author: pheuschk (open source code from stackoverflow)
- Modified ChangeLog.java, author: Karsten Priegnitz (open source code from code.google.com)
- Modified CSVWriter.java, Bytecode Pty Ltd. (Apache License 2.0)
- Modified File Chooser dialog, Google Code Archive, GNU GPL v3

Main features:
- Input page for meta data
- Counting page
- Page to edit counting options
- Results page with meta data
- Settings page
- Detailed Help page
- In German and English language
- "Reset Data" function
- Base-DB-Import/Export: Raw Database 'tourcount0.db'
- DB-Export: Database 'tourcount_yyyymmdd-hhmmss.db'
- DB-Results export as table in csv-format: 'tourcount_yyyymmdd-hhmmss.csv'
- App runs always in portrait mode

