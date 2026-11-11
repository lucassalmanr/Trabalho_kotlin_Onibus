# Mobilibus Ao Vivo

Android app in Kotlin and Jetpack Compose for following live Mobilibus vehicles.

## What the app delivers

- Retrofit + coroutines consume the supplied live endpoint.
- The endpoint is an `application/x-protobuf` feed, so `ProtoVehicleDecoder` converts its wire fields into app models instead of assuming JSON.
- Loading, success, empty and error states are handled in the vehicle list.
- Local account registration and login use Room. Passwords are stored only as SHA-256 hashes.
- DataStore keeps the logged-in user across app restarts. Sign out clears only that session.
- Room favorites are keyed by `userId + vehicleId`, so one account cannot see another account's saved vehicles.
- Navigation Compose provides four bottom tabs (Vehicles, Map, Saved, Account) and a typed `VehicleDetail(vehicleId)` route.
- A live map tab shows the real-time GTFS vehicle positions. Orange markers identify positions older than five minutes.
- The "my location" control asks for foreground approximate or precise location only when selected; it never requests background location.
- Code follows `ui -> domain <- data`: ViewModels depend on domain interfaces, and only the app graph wires concrete data implementations.

## Run

1. Open this folder in Android Studio (JDK 17).
2. Let Gradle sync dependencies.
3. Run on an Android 8.0+ emulator/device with internet access.

### Google Maps setup

The map uses the Google Maps SDK for Android. Create a key restricted to this Android app package and add it to your untracked `local.properties` file:

```properties
MAPS_API_KEY=your_google_maps_android_key
MOBILIBUS_ACCESS_KEY=your_mobilibus_access_key
```

Enable **Maps SDK for Android** for that key in Google Cloud. The app intentionally keeps both credentials out of versioned source code. Without a valid maps key the map cannot load; without the Mobilibus key the app displays a retryable vehicle-feed error.

The API endpoint supplied for the project uses HTTP. The app therefore explicitly permits cleartext traffic only so that endpoint can be reached.

## API

`GET http://realtime4.mobilibus.com/web/4ch6j/vehicle-positions?accesskey=YOUR_MOBILIBUS_ACCESS_KEY`

## Submission checklist

- Add emulator screenshots to this README after testing login, all three tabs, detail and favorites.
- Add the team's task division and one short reflection per member.
- Confirm every contributor has committed to the GitHub repository.
