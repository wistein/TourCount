# TourCount

TourCount is an Android app that supports species-specific counting of butterflies in nature.

The integrated database is organized according to a tour in nature with its expected butterfly species. That means, a new (importable and prepared) basic database instance will be used per tour.

Databases can be individually created and adapted within the app regarding meta data and expected butterfly species. The recorded data (meta data, counts, species data and remarks) may either be read on the smartphone or exported in SQLite- or CSV-format and transferred to a PC for your own processing, e.g. by importing a csv-file into MS Excel.

The app demands for storage access permit which is needed for im-/exporting the counting data, GPS permit for location info per count and the permit to prevent the phone from sleeping (to control the counting screen when used under Android 5.0.1 or newer).

Before using the app, please, read the documentation (provided in German and English).
There are a detailed description for setting up and using the app as well as sample basic databases (tourcount0.db and respective English version) provided under https://github.com/wistein/TourCount/tree/master/docs.

The app is available on F-Droid (https://f-droid.org/packages/com.wmstein.tourcount/). The apk-file 'tourcount-release.apk' is also provided in the GitHub repository under https://github.com/wistein/TourCount/tree/master/apk.
Please note that both versions are compiled from the same sources but signed differently and so cannot be mutually updated.

It is usable with Android Version 4.2.2 or newer.

Source file structure is for compilation by Android Studio.

### License

Copyright 2016-2017 Wilhelm Stein (wistein)

TourCount is licensed under the Apache License, Version 2.0 (the "License");
you may not use any of its files except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

App icon and app background picture: wistein

All butterfly pictures: wistein
