# WeatherXM Android App

The Android app powering the people's weather network.

We use **[Github Flow](https://githubflow.github.io/)** as our branching model and *Clean
Architecture*.

# Android Studio Environment

All contributors must import our code style settings for Android Studio:
[android-studio-settings.zip](https://github.com/WeatherXM/wxm-android/blob/main/android-studio-settings.zip)

Steps to import them:

1. File
2. Manage IDE Settings
3. Import Settings

# Building

To build the app from source, you need to pass the following environment variables, through `gradle`
command line arguments, or by creating a `production.env` properties file in the root directory,
according to the
[template](https://github.com/WeatherXM/wxm-android/blob/main/production.env.template) file,
that will be automatically read into env variables by the build script.

You would also need the `google-services.json` which can be downloaded from Firebase.

If you want to release on production, you would also need the correct keystore.

# Different Flavors

We have 5 different app flavors (consider flavors as different “build variants”, read
more [here](https://developer.android.com/build/build-variants)). For each flavor a
different `{flavor}.env`file can be created for different environment variables such as `API_URL`.

The 5 different app flavors are:

1. **Local Mock**: We use this to **mock responses from the API using some local JSON files** that
   can be found in `root/app/src/local/assets/mock_files/*` in order to test some edge cases and/or
   user reported bugs.
2. **Remote Mock:** This flavor communicates with our **remote mock API**.
   Powered by `remotemock.env`. **Currently only for internal use.**
3. **Remote Dev**: This flavor communicates with our **dev API**.
   Powered by `development.env`. **Currently only for internal use.**
4. **Remote Staging:** Initially used for communicating with our **staging API**.
   Powered by `staging.env`. **Not currently used.**
5. **Remote Prod:** This flavor communicates with our **production API**, found
   on `app.weatherxm.com`. Powered by `production.env`.
   The `API_URL` on that environment file should
   be [https://api.weatherxm.com](https://api.weatherxm.com)
6. **Solana:** This flavor communicates with our **production API**, found
   on `app.weatherxm.com`. Powered by `solana.env` and `production.env`. Used for publishing
   releases to Solana phones.

One thing that needs to be mentioned is that for every flavor a different build type can be used,
either a `Debug` or a `Release` build type.

# Android Studio Code Style Settings

We use these code style settings on Android Studio:
[android-studio-settings.zip](https://github.com/WeatherXM/wxm-android/blob/main/android-studio-settings.zip)

# JaCoCo - Code Coverage

We use [JaCoCo](https://docs.gradle.org/current/userguide/jacoco_plugin.html) tool in order to get
code coverage reports on every PR and deploy the report on each push on main. **Our Code Coverage
Report can be found in [GitHub Pages](https://weatherxm.github.io/wxm-android/coverage-report).**

# Detekt

We use [Detekt](https://github.com/detekt/detekt) as a code analysis tool for finding potential code
optimizations. Our config file for Detekt can be found in `detekt.yml`.

# GitHub Actions

We have 4 different [GitHub Actions](https://github.com/features/actions):

1. **Build & Code Analysis & Unit Tests & Coverage:** An action that runs on **every Pull Request**,
   building the app and running `./gradlew :app:detekt` in order to find potential code
   optimizations. Also runs the unit tests and creates a coverage report.
2. **Build Development and Distribute on Firebase:** An action that runs on **every push on
   ** `main`, building the app, and running
   `./gradlew :app:assembleDevDebugRelease :app:appDistributionUploadDevDebugRelease`
   in order to distribute **Dev Debug** versions of the app
   through [Firebase App Distribution](https://firebase.google.com/docs/app-distribution) in the
   Firebase Channels. Currently used for internal testing. Also deploys the Coverage Report in
   [GitHub Pages](https://weatherxm.github.io/wxm-android/coverage-report).
3. **Build Production & Google Play Internal Channel Distribution:** An action that runs on **every
   tag pushed,** building the app, and
   running `./gradlew bundleRemoteProdRelease -PSKIP_PRODUCTION_ENV` and
   `uses: r0adkll/upload-google-play@v1.1.3` in order to distribute a **Remote Prod** version of the
   app bundled as AAB through in the Google Play Internal Testing Channel.
4. **Build QA Release and Distribute on Firebase:** An action that gets
   [manually triggered](https://github.blog/changelog/2020-07-06-github-actions-manual-triggers-with-workflow_dispatch/)
   and takes as input the `environment`, the `build type` and creates a release and distributes it
   through
   [Firebase App Distribution](https://firebase.google.com/docs/app-distribution) in the QA
   Firebase Channels. Currently used for internal testing.

# How to merge `feature/` in Git?

When merging a `feature/` PR on Git, we do `squash merge` with an explanatory and human-friendly
commit message.

# Firebase Releases

For releases to the tech team, the QA team or the product team we are utilizing Firebase and more
specifically [Firebase App Distribution](https://firebase.google.com/docs/app-distribution) (can be
found under the *Release & Monitor* tab in Firebase Console). All different `Remote` flavors are
supported.

In order to upload a version up for testing, two different ways can be used:

1. Locally build an APK/AAB, upload it in Firebase Console → Release & Monitor → App Distribution (
   mind the current flavor which can be changed at the top of the page) and distribute it to the
   testers you prefer.
2. Using a GitHub Action to automatically build, upload and distribute an AAB/APK through Firebase.

# Internal Testing Releases (How To)

In order to create a new public beta release (**called Open Testing release on Play Console**) some
mandatory steps should be followed that we will explain below:

1. Create and push a new **tag** in `main` which will be the version we want to release. The tag
   uses the format `RC-Y_X.X.X` where `Y` is the RC number and `X.X.X` is the version name.
2. [Create a new GitHub release](https://github.com/WeatherXM/wxm-android/releases/new) out of main
   with the title being the version name (`X.X.X-RC-Y`). The same applies for the tag (use the tag
   you created in the previous step). On the description click `Auto-generate release notes` and
   format the text
   accordingly to remove authors and commit urls, and have just a human-readable list of release
   notes.
3. The Android App Bundle has been created automatically and deployed to "Internal Testing" in
   Google Play.
   When the deployment is completed, download the `AAB` file from App Bundle Explorer in Google Play
   Console"
   and attach it to that GitHub Release and then click `Publish Release`.
4. In Google Play Internal Testing release, edit the release notes and make them even more generic
   and human-friendly. For example, we don't want to mention 5 different UI fixes but just write "
   Numerous UI Fixes and Improvements". Also, the title of the release should contain
   the `-internal` suffix.
5. Send the new release for review by Google (we
   use [Google Play Managed Publishing](https://play.google.com/console/about/publishingoverview/)).

**Usually, after some days if everything is OK, we move this internal release from Internal
Testing → Open Testing on Play
Console (using console’s “Promote Release” @ Internal Testing → Releases → Find your release →
Promote
Release) and we execute the step at the below guide “Public Beta Releases (How To)”.**

# Public Beta Releases (How To)

In order to create a new public beta release (**called Open Testing release on Play Console**) some
mandatory steps should be followed after we have promoted our release from internal testing:

1. Send the new release for review by Google (we
   use [Google Play Managed Publishing](https://play.google.com/console/about/publishingoverview/)).

**Usually, after some days, we move this beta release from Open Testing → Production on Play
Console (using console’s “Promote Release” @ Open Testing → Releases → Find your release → Promote
Release) and we execute the steps 1-5 at the below guide “Production Releases (How To)”.**

# Production Releases (How To)

In order to create a new production release some mandatory steps
should be followed after we have promoted our release from open testing:

1. Send the new release for review by Google (we
   use [Google Play Managed Publishing](https://play.google.com/console/about/publishingoverview/)).
2. After the release passes Google’s review and you publish the app to the
   public, [use this template](https://outline.weatherxm.com/doc/templates-for-update-announcements-Uiek4uZYjE),
   and after filling the correct date, app version and release notes, publish it on Discord's
   #announcements channel or have a community manager publish it.
3. Create and push a new **tag** in `main` which will be the version we want to release. The tag
   uses the format `X.X.X` where `X.X.X` is the version name.
4. [Create a new GitHub release](https://github.com/WeatherXM/wxm-android/releases/new) out of main
   with the title being the version name (`X.X.X`). The same applies for the tag (use the tag you
   created in the previous step). On the description click `Auto-generate release notes` and format
   the text accordingly to remove authors and commit urls, and have just a human-readable list of
   release notes.
5. At Firebase's Console, go
   to [remote config](https://console.firebase.google.com/u/0/project/weatherxm-321811/config) and
   edit the `android_app_changelog`, `android_app_minimum_code` , `android_app_version_code` as
   needed.

# Solana Releases (How To)

Releases on Solana require manual handling. The following steps should be completed:

1. Create and push a new **tag** in `main` which will be the version we want to release. The tag
   uses the format `Solana_X.X.X` where `X.X.X` is the version name. **It is required to use the**
   `**Solana_**` **prefix**.
2. Create a new APK locally - that uses the above tag we created to get the correct version code and
   version name - via Android Studio:
    1. Build
    2. Generate Signed App Bundle or Apk
    3. Select “APK” and using the Solana’s keystore create a `remoteSolanaReleaseOnSolana` APK.
3. Manually publish that newly created APK via Solana’s CLI tool.
