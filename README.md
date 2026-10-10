# DG Scout

An Android app for scouting a disc golf course before an event, then
carrying the same notes into competition.

For each hole you can:

- Record the **basket position** by GPS.
- See a live **distance to the basket** (and to any located note) from
  wherever you're standing, along with the accuracy of that measurement.
- Add, edit, and delete **notes**, each optionally tagged with the GPS
  location where you wrote it (e.g. "start of OB, river left").
- Jump straight to the **previous/next hole** without going back to the
  hole list.
- Choose **feet or metres** for all distance readouts (defaults to metres
  outside the US).

All data is stored locally on the device as JSON — there's no account,
server, or network dependency, so it works with no signal on a fairway.

## Requirements

- Android Studio (or a JDK 21 + Android SDK toolchain) with SDK Platform
  37 installed.
- A device or emulator running Android 7.0 (API 24) or later.

## Building and running

```
./gradlew assembleDebug     # build the debug APK
./gradlew installDebug      # build and install to a connected device/emulator
```

Or open the project in Android Studio and run the `app` configuration.

## Permissions

The app requests `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` to
record basket positions, tag notes with a location, and compute live
distances. Location is only read while a hole screen is open (GPS stays
on there so it can settle to an accurate fix) and is never sent
anywhere off the device.

## Project layout

- `app/src/main/java/com/undy/dgscout/model` — data model (`Course`,
  `Hole`, `Note`, `GeoPoint`, units).
- `app/src/main/java/com/undy/dgscout/data` — local JSON persistence
  (`CourseRepository`).
- `app/src/main/java/com/undy/dgscout/location` — GPS helpers and
  distance/unit formatting.
- `app/src/main/java/com/undy/dgscout/ui` — Compose screens and the
  shared `CourseViewModel`.

## License

GNU General Public License v3.0 or later — see [LICENSE](LICENSE).
