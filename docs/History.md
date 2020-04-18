# History

#### TourCount Copyright (C) 2016-2019, Wilhelm Stein

#### Version 3.2.0 (2020-04-17)
- Code adapted for restricted Storage Access of Android 10
- Use of AndroidX libraries
- Help text updated
- Many minor code refinements
- Project adaption for Android Studio 3.6.2

#### Version 3.1.5 (2020-02-26)
- Code optimized for storage and power efficiency
- App icon adapted to better match Android 9 screen
- Project adaption for Android Studio 3.6 and Gradle 5.6.4

#### Version 3.1.4 (2019-12-08)
- Undercutting of species names in results page corrected
- Minor text improvements
- Project adaption for Android Studio 3.5.3 and Gradle 5.4.1
- Changes in AndroidManifest.xml and build.gradle 

#### Version 3.1.2 (2019-08-04)
- Added/changed butterfly pictures
- Exported csv-file with total of different species counted
- Bugfix in database handling

#### Version 3.1.1 (2019-04-12)
- Project adaption for Android Studio 3.4.1 and Gradle 5.1.1
- Minimal changes to starting page
- Bugfix

#### Version 3.1.0 (2019-04-16)
- Integrated list of many European species
- Instead of manual input, add further species to your species list by scroll-down selection from a comprehensive list that contains only species not contained yet

#### Version 3.0.8 (2019-03-25)
- Database structure supplemented for local butterfly names
- Example databases adapted for local butterfly names
- Exported results include local butterfly names
- Additional butterfly pictures
- Some minor screen design changes
- Some code cleaning
- Project adaption for Android Studio 3.3.2 and Gradle 4.10.1
- Minimum Android version changed to 4.4 KitKat 

#### Version 3.0.7 (2018-12-16)
- Additional butterfly pictures
- Some pictures edited to show both sides of wings
- Cosmetic changes to several app pages
- Fix for crash when species selector is rapidly repeatedly pressed
- Docs updated

#### Version 3.0.5 (2018-09-20)
- Fixed GPS height correction not working

#### Version 3.0.4 (2018-08-03)
- Location retrieval function more reliable, changed to service
- Additional butterfly pictures
- Where applicable toasts replaced with snackbars
- Some code cleaning
- Fix for crash in permission handling of Android >= Marshmallow
- Fix for crash in "Edit Species list" functionality

#### Version 3.0.3 (2018-05-01)
- Additional manual input for date, start-time and stop-time with long press
- Project changes for Android Studio 3.1.2 and Gradle 3.1.2

#### Version 3.0.2 (2018-04-17)
- Code improvements
- Bug fixes

#### Version 3.0.1 (2018-04-03)
- Unnecessary permission WRITE_SETTINGS removed
  
#### Version 3.0.0 (2018-03-31)
- Redesign of counting page layout with scroll-down selection od species
- Separate counters for ♂ or ♀, ♂, ♀, pupa, larva, egg
- Database version change 2 to 3 (takes care of additional counters)
- First use now creates a direct usable internal DB with common european species
- Automatic conversion of existing databases to new version
- Correct handling of subtraction for categories of species and individuals
- Adapted results page layout 
- Layout of starting page updated
- More butterfly pictures added
- Documentation adapted
- Project changes for Android Studio 3.1, Gradle 3.1.0 and SDK 27 (Android 8.1)
- Bug fixes

#### Version 2.4.6 (2018-03-19)
- Code optimization for Reverse Geocoding
- Added a camera button in counting menu
- Source Project restructured
- Bug fixes

#### Version 2.4.2 (2018-02-28)
- Reverse Geocoding by Nominatim Service of OpenStreetMap for meta data and individual location
- Results page improved (average coordinates, listing of individual counts with data)
- Diverse improvements of details
- Obsolete functions eliminated and bug fixes
- Documents updated

#### Version 2.4.0 (2018-02-19)
- OSM Reverse Geocoding from GPS coordinates for meta data and individual location
- Improved Results page with individual counts data
- Documents updated

#### Version 2.3.2 (2018-01-01)
- Added pictures of species for Canaries and Madeira
- Bug fix

#### Version 2.3.0 (2017-09-27)
- Code adapted and compiled for Android 7.1
- Minor code improvements
- Bug fixes

#### Version 2.2.0 (2017-09-11)
- Write log only in debug mode

#### Version 2.1.9 (2017-09-09)
- Improved layout of results page
- Added database version control to be compatible with old database structure prior version 2.1.4
- Fix crash when counting using a database of old structure prior version 2.1.4
- Bug fixes

#### Version 2.1.8 (2017-08-30)
- Added Option to switch between portrait and landscape mode
- Show correct headline immediately after loading/resetting DB
- Bug fixes and minor code improvements

#### Version 2.1.7 (2017-08-22)
- Correct height with geoid offset from EarthGravitationalModel EGM96

#### Version 2.1.6 (2017-07-14)
- CSV export now shows No. of species according to recorded sex and stadium
- CSV export now suppresses zeros for empty counts
- Finishing individual input now has button sound
- Database structure adapted to enhanced individuals data export 
- Press back button twice to exit
- minor string improvements

#### Version 2.1.5 (2017-05-27)
- Additional support for coarse location from Wifi and Network
- GPS height correction
- Results page with totals
- Additional butterfly pictures

#### Version 2.1.4 (2016-12-01)
- Small bug fix
- Pictures of species on counting and results page
- Default background picture different from that of TransektCount app
    
#### Version 2.1.3 (2016-10-31)
- Code improvement
- Icons on settings page
- Icons in menus
- Codes for species added
- Option added: Sorting of species by names or codes

#### Version 2.1.2 (2016-10-03)
- Results screen layout improved

#### Version 2.1.1 (2016-09-30)
- Multiple counts on individuals page

#### Version 2.1.0 (2016-09-09)
- Option counting screen for lefties

#### Version 2.0.9 (2016-08-29)
- Exportable csv-table improved
- Internal DB structure adapted

#### Version 2.0.7 (2016-08-04)
- Bug fix
- Code optimized
    
#### Version 2.0.6 (2016-07-24)
- Switch screen off on counting page when in pocket
    
#### Version 2.0.5 (2016-06-22)
- Option "Screen bright" in Preferences
- File import dialog for existing DB
    
#### Version 2.0.4 (2016-06-20)
- Code cleaned, unused functions removed
- Permission handling for Android M
- Records geogr. altitude 
- DB structure adapted
- Bug fix

#### Version 2.0.3 (2016-06-06)
- DB structure improved
- Results view improved
- Input masks improved
- Bug fixes

#### Version 2.0.2 (2016-05-27)
- Sorted csv export
- Limited to 1 counting list
- Code improved

#### Version 2.0.1 (2016-05-19)
- Functionality of individual data handling completed
- Results pages and export to CSV adapted and completed

#### Version 1.0.2 (2016-05-18)
- Count buttons activate an individuals page to get data per individual count
- Table "individuals" with time, coordinates, sex, stadium, remarks added to DB

#### Version 1.0.1 (2016-05-14)
- Meta data (date, time) added
- Plausi control for meta data
- Prepared for GPS recording
    
#### Version 1.0.0 (2016-04-18)
##### 1. Release.
TourCount derived from TransektCount which originally derives partly from BeeCount by knirirr
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

