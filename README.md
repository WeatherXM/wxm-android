# WeatherXM Android app

The Android app powering the people's weather network.

We use [**Github Flow**](https://githubflow.github.io/) as our branching model.

## Building

To build the app from source, you need to pass the following environment variables, 
through `gradle` command line arguments, or by creating a `.env` properties file
in the root directory, according to the [template](.env.template) file,
that will be automatically read into env variables by the build script.

## How to merge `feature/` in Git?
When merging a `feature/` PR on Git, we do `squash merge` with an explanatory and nice commit message.

## How To Release?
In order to create a new production release some mandatory steps should be followed that we will explain below:
1. `git checkout main` and create an Android App Bundle via `Generate Signed Bundle / APK` in Android Studio.
2. [Create a new GitHub release](https://github.com/WeatherXM/wxm-android/releases/new) out of main with the title being the version name (`X.X.X`). The same applies for the tag (create a new tag if not exist). On the description click `Auto-generate release notes` and format the text accordingly to remove authors and commit urls, and have just a human-readable list of release notes.
3. The Android App Bundle that was created before should be attached as a binary in that GitHub release. When the uploading is completed `Publish Release`.
4. Follow the procedure to upload that Android App Bundle in Google's Play Store and use the auto-generated release notes you got before. Make them even more generic and human-friendly. For example, we don't want to mention 5 different UI fixes but just write "UI Fixes and Improvements".
5. Create and push a new commit in `main` bumping the `versionCode` and `versionName` in `build.gradle`
6. After the release passes review and is available publicly, [use this template](https://docs.google.com/document/d/1U5_c8qvHTYPKzTJMKDRyJ6lnWswHiQMZ_3TILrIp0-A/edit#), and after filling the correct date, app version and release notes, publish it on Discord's announcement or have a community manager publish it.
7. At Firebase's Console, go to [remote config](https://console.firebase.google.com/u/0/project/weatherxm-321811/config) and edit the `android_app_changelog`, `android_app_minimum_code`, `android_app_version_code` as (and if) needed.

