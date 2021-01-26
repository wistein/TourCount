# TourCount

TourCount is an Android app that supports species-specific and individually localized registering of butterflies.

### Features

- Data recording is organized for a tour in nature with its expected butterfly species.
- The app comes with a prepared basic database with the most common species in Europe as a starting point.
- Databases can be individually created and adapted within the app regarding meta data and expected butterfly species.
- You may use an individual species list per region.
- The app contains an editor to setup or adapt the current species list,
- an editor to prepare the meta data for a tour,
- an editor for individual species data of a count (e.g. state, remark or bulk count entry).
- Counting page with scroll-down-menu to select a species and
- separate counters for ♂ or ♀, ♂, ♀, pupa, larva or egg.
- The app automaticaly inserts date, time, coordinates and height as well as
- location data by reverse OSM-geocoding when GPS and internet are accessible.
- Results are prepared for easy readout to transfer them to the science4you entry mask for butterfly registration.
- Results are shown with meta data, totals, remarks, species results and individual data.
- Results may be exported in SQLite- or CSV-format (for transfer to a PC for your own processing, e.g. in a spreadsheet.)
- Help contains a detailed userguide.
- TourCount uses the European coding scheme for butterflies from Karsholt/Razowski.

### Prerequisites
The app demands for 
- storage access permit which is needed for im-/exporting the counting data, 
- GPS permit for coordinates and location info per count, 
- internet access for OpenStreetMap reverse geocoding and 
- the permit to keep the device awake.

The smartphone screen should have a minimum resolution of 1920 x 1024 pixels.  
The app is usable with Android Version 4.4 (KitKat) or newer.

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

Copyright 2016-2021 Wilhelm Stein (wistein)

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
