# TourCount

TourCount is an Android app that supports species-specific and individually localized counting of butterflies in nature.

### Features

- Recording is organized according to a tour in nature with its expected butterfly species. 
That means, you will use a new database instance per tour by importing a prepared basic Database.
- Databases can be individually created and adapted within the app regarding meta data and 
expected butterfly species.
- There are prepared database files provided for most of the european species in /docs.
- Editor to set up or adapt the list of species
- Editor for the meta data to prepare a tour
- Editor for individual species data of a counted individual (e.g. sexus, stadium, state or remark)
- Automatic insertion of date, time, coordinates and height
- Automatic insertion of location data by reverse OSM-geocoding when GPS and internet are accessible
- Results are prepared for easy readout to transfer them to science4you entry masks
- Results are shown with meta data, totals, species results and individual data.
- Results may also be exported in SQLite- or CSV-format and transferred to a PC for your own 
processing, e.g. by importing a csv-file into a spreadsheet like MS Excel.
- The integrated help contains a detailed userguide.

### Prerequisites
The app demands for 
- storage access permit which is needed for im-/exporting the counting data, 
- GPS permit for coordinates and location info per count, 
- internet access for OpenStreetMap reverse geocoding and 
- the permit to prevent the phone from sleeping (to control the counting screen when used under 
Android 5.0.1 or newer).

The app is usable with Android Version 4.2.2 or newer.

### General
#### Documentation
Documentation is provided under  
https://github.com/wistein/TourCount/tree/master/docs.

Please read the userguide (provided in German and English) or consult its contents in the app's help 
before using the app.  
There are further documents e.g. for setting up and using the app as well as sample basic databases 
(tourcount0.db and respective English version) provided under /docs. 

The development history is listed in History.md in /docs.

Source file structure is for compilation by Android Studio.

#### Availability
The app is available on F-Droid under  
https://f-droid.org/packages/com.wmstein.tourcount/.
 
The apk-file 'tourcount-release.apk' is also provided in the GitHub repository under 
https://github.com/wistein/TourCount/tree/master/apk.

Please note that both versions are compiled from the same sources but signed differently and so 
cannot be mutually updated.

### License

Copyright 2016-2018 Wilhelm Stein (wistein)

TourCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

App icon and app background picture by wistein

Butterfly pictures: 
- p07083.png derived from "Bright babul blue 2.jpg" by Savandurga Karnataka CC BY-SA 4.0 (https://creativecommons.org/licenses/by-sa/4.0/)
- p07292.png derived from "Hypolimnas misippus (ento-csiro-au).jpg" by Svdmolen (Public Domain)
- all others by wistein 
