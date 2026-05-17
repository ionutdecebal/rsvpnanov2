# shared (Kotlin Multiplatform)

This module contains shared business logic for RSVPNanoCompanion.

Quick start:

- Run `bash ./gradlew :shared:check :androidApp:assembleDebug`.
- Build the iOS XCFramework with `bash RSVPNanoCompanion/tools/build_shared_xcframework.sh`.
- Add the produced iOS framework to Xcode or add the module to your Android project.

Design goals:
- Keep platform-specific code minimal by using interfaces.
- Centralize parsing, encoding, and serialization logic in `commonMain`.
- Treat shared JSON stores as the only persistence contract for iOS and Android.
- Create platform Ktor clients from shared wiring (`createAndroidNanoClient`,
  `createIosNanoClient`) so UI code does not duplicate HTTP behavior.
- Keep byte-for-byte converter parity covered by tests under `shared/src/*Test`, including the
  demo RSVP pass-through vector in `docs/demo-books`.
