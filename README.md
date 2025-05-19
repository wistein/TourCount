# TourCount

TourCount is an Android app that supports species-specific and individually localized registering of butterflies.

### Features

- Data recording is organized for a tour in nature with its expected butterfly species.
 That means, you will use a fresh database instance per tour by clearing the current database or importing a prepared regional basic database.
- The app comes with a prepared basic database with the most common species in Europe as a starting point.
- Databases can be individually created and adapted within the app regarding meta data and expected butterfly species.
- You may use an individual species list per region.
- The app contains editors 
  - to setup or adapt the current species list with add and delete species functions,
  -  to prepare the meta data for a tour and
  -  for individual species data of a count (e.g. status, remark or bulk count entry).
- The counting page has a scroll-list to select a species by picture and name.
- Separate counters for ♂|♀, ♂, ♀, pupa, larva or egg.
- The app automatically inserts date, time, coordinates and height as well as
  location data by reverse OSM-geocoding when GPS and internet access are granted.
- Results are prepared for easy readout to transfer them to the science4you entry mask for butterfly registration.
- Results are shown with meta data, totals, remarks, species results and individual data.
- Results may be exported as SQLite-DB or in CSV-format (e.g. for transfer to a PC for processing in a spreadsheet).
- The current butterfly list can be exported for later re-import or imported by the complementary app TransektCount.
- Help contains a detailed userguide.
- TourCount uses the codes of the European coding scheme for butterflies from Karsholt/Razowski.

### Prerequisites
The app demands for 
- storage access permit which is needed for exporting the counting data, 
- the permit to read location data for coordinates and location info per count, 
- internet access for OpenStreetMap reverse geocoding, 
- the permit to keep the device awake and

The device screen should have a minimum resolution of 1920 x 1024 pixels.  
The app is usable with Android version 7.1 (Nougat) or newer.

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
 
All versions of the apk-file 'tourcount-release_nnn.apk' are provided in the GitHub repository under 
https://github.com/wistein/TourCount.

Please note that both F-Droid and GitHub versions are compiled from the same sources but signed 
differently and so cannot be mutually updated.

On GitHub you will find the published stable version when clicking the "master"-button and select 
the Tag with the highest Branch number n.n.n. Then get the file tourcount-release_nnn.apk under apk. 

The latest beta development version of 'tourcount-release_nnn.apk' is also provided in the GitHub 
repository under the master branch https://github.com/wistein/TourCount/tree/master/apk.

### Licenses

Copyright 2016-2025 Wilhelm Stein

TourCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Pictures of this work by Wilhelm Stein may be used for scientific or other non-commercial purposes without prior permission.
But it is not permitted to upload pictures to social media platforms, as most of here mentioned licenses do not comply with and I do not agree with the licensing terms of these platforms.

Please note that illustrations provided by other authors remain the copyright of those authors and should not be reproduced or distributed other than with their permission.

For detailed Copyrights on external code and pictures see the separate document "License.txt" in TourCount/docs.

