# TourCount

TourCount is an Android app that supports species-specific and individually localized registering of butterflies.

### Features

- Data recording is organized for a tour in nature with its expected butterfly species.
- The app comes with a prepared basic database with the most common species in Europe as a starting point.
- Databases can be individually created and adapted within the app regarding meta data and expected butterfly species.
- You may use an individual species list per region.
- The app contains editors to setup or adapt the current species list,
- to prepare the meta data for a tour and
- for individual species data of a count (e.g. state, remark or bulk count entry).
- Counting page with scroll-down-menu to select a species by picture and name.
- Separate counters for ♂♀, ♂, ♀, pupa, larva or egg.
- The app automaticaly inserts date, time, coordinates and height as well as
  location data by reverse OSM-geocoding when GPS and internet are accessible.
- Results are prepared for easy readout to transfer them to the science4you entry mask for butterfly registration.
- Results are shown with meta data, totals, remarks, species results and individual data.
- Results may be exported as SQLite-DB or in CSV-format (e.g. for transfer to a PC for processing in a spreadsheet).
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

Copyright 2016-2024 Wilhelm Stein

TourCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Pictures of this work by Wilhelm Stein may be used for scientific or other non-commercial purposes without prior permission.
But it is not permitted to upload pictures to social media platforms, as most of their licenses do not comply with and I do not agree with the licensing terms of these platforms.

Please note that illustrations provided by other authors remain the copyright of those authors and should not be reproduced or distributed other than with their permission.

### External references

Integrated code from external references:
- Basic counting functions structure from BeeCount, (C) 2016 Milo Thurston (knirirr), 
  Apache License, Version 2.0, https://github.com/knirirr/BeeCount
- AutoFitText.java, modified, original code from author: pheuschk, 18.04.2013, 
  Open Source, https://pastebin.com/raw/e6WyrwSN
- ChangeLog.java, modified, original code (C) 2011-2013, Karsten Priegnitz, 
  Other Open Source, https://github.com/koem/android-change-log/
- CSVWriter.java, modified, original code (C) 2015 Bytecode Pty Ltd., 
  Apache License, Version 2.0
- filechooser based on android-file-chooser, 2011, Google Code Archiv, GNU GPL v3.
- egm derived from GeoTools, (C) 2006-2008, Open Source Geospatial Foundation, 
  https://geotools.org/

Butterfly pictures from external references: 
- Adela paludicolella, created from picture in
  "Adelidae (Lepidoptera) - Beitrag zur Kenntnis der Biologie und Bestimmungshilfe für die europäischen Arten",
  Author: Rudolf Bryner, free for non-commercial purposes in education and science and for private use
- Crambus pascuella derived from https://en.wikipedia.org/wiki/File:Crambus.pascuella.7563.jpg,
  Author: Olaf Leillinger, GNU Free Documentation License
- Cucullia verbasci, derived from https://en.wikipedia.org/wiki/Mullein_moth, 
  Author: Dumi, GNU Free Documentation License, version 1.2
- Deltote bankiana derived from http://www.freenatureimages.eu/Animals/index.html, 
  Author: Andre den Ouden, licensed by www.saxifraga.nl free for non-commercial use
- Evergestis extimalis derived from https://upload.wikimedia.org/wikipedia/commons/3/37/Evergestis_extimalis1.JPG,
  Author: Adam Furlepa, CC BY-SA 3.0
- Hipparchia neomiris, created from pictures of "Die Groß-Schmetterlinge der Erde",
  Author: Adalbert Seitz, 1909, Public Domain
- Hyles gallii derived from https://tpittaway.tripod.com/sphinx/h_gal_a3.jpg (non-commercial use allowed)
- Hyles hippophaes derived from https://tpittaway.tripod.com/sphinx/h_hip_a2.jpg (non-commercial use allowed)
- Hyponephele lupinus (Public Domain), derived from https://upload.wikimedia.org/wikipedia/commons/0/07/Hyponephelelupinus.jpg
- Jordanita globulariae derived from https://commons.wikimedia.org, Author: Reza Zahiri et al, 
  Creative Commons Attribution License (CC BY 4.0)
- Marumba quercus derived from https://tpittaway.tripod.com/sphinx/m_que_a4.jpg (non-commercial use allowed)
- Melanargia arge, Author: Notafly, Creative Commons Attribution-Share Alike 3.0 Unported license, derived from 
  https://en.wikipedia.org/wiki/File:Melanargiaarge.JPG
- Melitaea parthenoides derived from https://en.wikipedia.org/wiki/Melitaea_parthenoides, 
  Author: Didier Descouens, Creative Commons Attribution-Share Alike 4.0 International license
- Muschampia lavatherae derived from https://en.wikipedia.org/wiki/Muschampia_lavatherae, 
  Author: Dumi, GNU Free Documentation License, version 1.2
- Nemaphora barbatellus, created from picture in
  "Adelidae (Lepidoptera) - Beitrag zur Kenntnis der Biologie und Bestimmungshilfe für die europäischen Arten",
  Author: Rudolf Bryner, free for non-commercial purposes in education and science and for private use
- Pediasia contaminella derived from
  https://commons.wikimedia.org/wiki/File:Pediasia_contaminella_(33480878804).jpg,
  Author: Ben Sale, Creative Commons Attribution 2.0 Generic License
- Phragmataecia castaneae derived from https://species.wikimedia.org/wiki/Phragmataecia_castaneae,
  Author: Dumi, GNU Free Documentation License, version 1.2 
- Scopula rubiginata derived from http://www.freenatureimages.eu/Animals/index.html,
  Author: Rob Felix, licensed by www.saxifraga.nl free for non-commercial use
- Scopula virgulata derived from http://www.freenatureimages.eu/Animals/index.html,
  Author: Zoran Bozovic, licensed by www.saxifraga.nl free for non-commercial use
- Sesia apiformis derived from picture on https://en.wikipedia.org/wiki/Hornet_moth
  Author: Jyrki Lehto, free use
- Tiliacea aurago, derived from http://www.freenatureimages.eu/Animals/index.html, 
  Author: Peter Gergely, licensed by www.saxifraga.nl free for non-commercial use

- All other pictures, app icon and background (C) Wilhelm Stein. 
