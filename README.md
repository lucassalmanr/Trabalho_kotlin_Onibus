# Mobilibus Ao Vivo

Android app in Kotlin and Jetpack Compose for following live Mobilibus vehicles.

## What the app delivers

- Retrofit + coroutines consume the supplied live endpoint.
- The endpoint is an `application/x-protobuf` feed, so `ProtoVehicleDecoder` converts its wire fields into app models instead of assuming JSON.
- Loading, success, empty and error states are handled in the vehicle list.
- Local account registration and login use Room. Passwords are stored only as SHA-256 hashes.
- DataStore keeps the logged-in user across app restarts. Sign out clears only that session.
- Room favorites are keyed by `userId + vehicleId`, so one account cannot see another account's saved vehicles.
- Navigation Compose provides three bottom tabs (Vehicles, Saved, Account) and a typed `VehicleDetail(vehicleId)` route.
- Code follows `ui -> domain <- data`: ViewModels depend on domain interfaces, and only the app graph wires concrete data implementations.

## Run

1. Open this folder in Android Studio (JDK 17).
2. Let Gradle sync dependencies.
3. Run on an Android 8.0+ emulator/device with internet access.

The API endpoint supplied for the project uses HTTP. The app therefore explicitly permits cleartext traffic only so that endpoint can be reached.

## API

`GET http://realtime4.mobilibus.com/web/4ch6j/vehicle-positions?accesskey=982a57efd77a9462bf1665696fb25984`

## Submission checklist

- Add emulator screenshots to this README after testing login, all three tabs, detail and favorites.
- Add the team's task division and one short reflection per member.
- Confirm every contributor has committed to the GitHub repository.
