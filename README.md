# AutoDial

A minimal Android app intended for dedicated Android/POS devices where a
`tel:` link from a PWA/browser should initiate a cellular call without
requiring the user to press the green Call button.

## Flow

PWA/browser:
    <a href="tel:+254712345678">Call</a>

Android:
    tel: intent -> AutoDial -> ACTION_CALL -> cellular call

## Important Android requirement

On Android 10+ the app must be selected by the user as the default phone/dialer
app to qualify for the `ROLE_DIALER` role. The Android platform requires
dialer-role apps to implement the DIAL intent and InCallService.

The app also requests CALL_PHONE at runtime because ACTION_CALL requires it.

## Build

Open this folder in Android Studio and let Gradle sync.

Build debug APK:

    ./gradlew assembleDebug

APK:

    app/build/outputs/apk/debug/app-debug.apk

Install:

    adb install -r app/build/outputs/apk/debug/app-debug.apk

Then open AutoDial, grant phone permission, and select
"Make AutoDial the default phone app".

## Test

From Chrome or your PWA, use:

    <a href="tel:+254712345678">Call Customer</a>

Use a real test number rather than the placeholder in the app.

## Caveat

Modern Android requires a default dialer implementation to provide a complete
in-call experience. This starter project contains the required InCallService
component, but for a Play Store-quality public dialer it should be expanded
with proper incoming-call and ongoing-call UI.
# android-auto-dialer
