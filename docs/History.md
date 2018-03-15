# History

#### TourCount Copyright (C) 2016-2018, Wilhelm Stein
  
#### Version 2.4.5 (2018-03-15)
- Code optimization for Reverse Geocoding
- Added a camera button in counting menu
- Source Project restructured
- Bud fixes

#### Version 2.4.2 (2018-02-28)
- Reverse Geocoding from GPS coordinates for meta data and individual location
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

Main features comprise
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

