# Welcome to WeatherXM's Android App contributing guide.

First of all, thank you for investing your time to contribute in WeatherXM! Any contribution you
make will be reflected on
our [Android App](https://play.google.com/store/apps/details?id=com.weatherxm.app).

We use **[Github Flow](https://githubflow.github.io/)** as our branching model and *Clean
Architecture*.

In this guide you will get an overview of the contribution workflow from opening an issue, creating
a PR, reviewing, and merging the PR.

## Table Of Contents

[Code of Conduct](#code-of-conduct)

[Building the app](#building--environment)

* [Environment Variables](#environment-variables)
* [Different Flavors](#different-flavors)
* [Google Services JSON](#google-services-json)

[How to ask a question, report a bug or suggest a potential new feature/improvement?](#how-to-ask-a-question-report-a-bug-or-suggest-a-potential-new-featureimprovement)

* [Do you have a question](#do-you-have-a-question)
* [Reporting Bugs](#did-you-find-a-bug)
* [Suggesting Enhancements](#do-you-want-to-suggest-a-potential-improvement-or-a-new-feature)

[How to Contribute?](#how-to-contribute)

[Styleguide](#styleguide)

* [Detekt](#detekt)

[Additional Notes](#additional-notes)

* [Issue Labels](#issue-labels)

## Code of Conduct

This project and everyone participating in it is governed by the
[Code of Conduct](https://github.com/weatherxm-network/wxm-android/blob/main/CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code to keep our community approachable and
respectable.

## Building & Environment

### Android Studio Code Style Settings
All contributors must import our code style settings for Android Studio: 
[android-studio-settings.zip](https://github.com/weatherxm-network/wxm-android/blob/main/android-studio-settings.zip)

Steps to import them:
1. File
2. Manage IDE Settings
3. Import Settings

### Environment Variables

To build the app from source, you need to pass the following environment variables, through `gradle`
command line arguments, or by creating a `production.env` properties file in the root directory,
according to
the [template](https://github.com/weatherxm-network/wxm-android/blob/main/production.env.template) file,
that will be
automatically read into env variables by the build script. You must use your own environmental
variables
for contributing to the project.

All environment variables have descriptive comments on the template, but some extra information
should be given
regarding the Firebase and the Mapbox ones.

The `API_URL` is specified via these `*.env` files. In order to use a different `API_URL` than the
one provided
in `production.env.template`, for each different flavor (see below about our different flavors)
you need to create a different `{flavor}.env` file based on the following map:
Remote Mock -> `remotemock.env`
Remote Dev -> `development.env`
Remote Staging -> `staging.env`

#### Firebase Variables

We have two different variables for Firebase:

- `FIREBASE_PUBLIC_TEST_GROUP`
- `FIREBASE_INTERNAL_TEST_GROUP`
  These are used for using builds through Firebase App Distribution to testers, won't be needed for
  contributors so feel free to emit that.

#### Mapbox Variables

We have two different variables for MapBox:

- `MAPBOX_ACCESS_TOKEN`
- `MAPBOX_DOWNLOADS_TOKEN`
  These are mandatory for building the project. For creating those tokens you need to create
  a [Mapbox account](https://account.mapbox.com)
  and create the respective tokens in the [Tokens](https://account.mapbox.com/access-tokens/)
  section. The `MAPBOX_DOWNLOADS_TOKEN` has secret scopes.

You can view Mapbox guide on Access
Token [here](https://docs.mapbox.com/help/getting-started/access-tokens/).

### Different Flavors

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

One thing that needs to be mentioned is that for every flavor a different build type can be used,
either a `Debug` or a `Release` build type.

### Google Services JSON

The `google-services.json` configuration file is mandatory for building the app. Depending on the
flavor you want to use,
you should create your own Firebase project and download that file. A guide on how to do it can be
found [here](https://firebase.google.com/docs/android/setup#console).

The package names on the Firebase project you will create should be as below depending on the flavor
you want to use:

- **Local Mock**: `com.weatherxm.app.mock`
- **Remote Mock:** `com.weatherxm.app.mock`
- **Remote Dev**: `com.weatherxm.app.dev`
- **Remote Staging:** `com.weatherxm.app.staging`
- **Remote Prod:** `com.weatherxm.app`

## How to ask a question, report a bug or suggest a potential new feature/improvement?

### **Do you have a question?**

* **Ensure your question was not already asked** by searching on GitHub
  under [Issues](https://github.com/weatherxm-network/wxm-android/issues?q=label%3Aquestion+) under the
  label _Question_.

* If you're unable to find a response to your
  question , [open a new issue](https://github.com/weatherxm-network/wxm-android/issues/new/choose) by using
  the [**Ask a question** template](https://github.com/weatherxm-network/wxm-android/blob/main/.github/ISSUE_TEMPLATE/ask_a_question.md).
  Using this template is mandatory. Make sure to have a **clear title** and include as many details
  as possible as that information helps to answer your question as soon as possible.

### **Did you find a bug?**

* **Ensure the bug was not already reported** by searching on GitHub
  under [Issues](https://github.com/weatherxm-network/wxm-android/issues?q=label%3Abug) under the label
  _Bug_.

* If you're unable to find an open issue addressing the
  problem, [open a new issue](https://github.com/weatherxm-network/wxm-android/issues/new/choose) by using
  the [**Bug Report** template](https://github.com/weatherxm-network/wxm-android/blob/main/.github/ISSUE_TEMPLATE/bug_report.md).
  Using this template is mandatory. Make sure to have a **clear title** and include as many details
  as possible as that information helps to resolve issues faster.

### **Do you want to suggest a potential improvement or a new feature?**

* **Ensure this suggestion was not already reported** by searching on GitHub
  under [Issues](https://github.com/weatherxm-network/wxm-android/issues?q=label%3Aenhancement+) under the
  label _Enhancement_.

* If you're unable to find that
  suggestion, [create a new issue](https://github.com/weatherxm-network/beta-issue-tracker/issues/new/choose)
  by using the [**Feature Request** template](https://github.com/weatherxm-network/beta-issue-tracker/blob/main/.github/ISSUE_TEMPLATE/feature_request.md).
  Using this template is mandatory. Make sure to have a **clear title** and include as many details
  as possible.

## How to contribute?

We are open to contributions on [current issues](https://github.com/weatherxm-network/wxm-android/issues),
if the bug/feature/improvement you would like to work on isn't documented, please open a new issue
so we can approve it before you start working on it.

### Fix a bug, implement a new feature or conduct an optimization

Scan through our existing [issues](https://github.com/weatherxm-network/wxm-android/issues) to find one that
interests you (or create a new one). You can narrow down the search using `labels` as filters.
See [Issue Labels](#issue-labels) for more information. Please don't start working
on issues that are currently assigned to someone else or have the `in-progress` label. If you find
an issue to work on, please comment in it to get it assigned to you and you are welcome to open a PR
with a fix/implementation.

### Pull Request

When you're finished with the changes, create a pull request, also known as a PR.

- Fill the "Ready for review" template so that we can review your PR. This template helps reviewers
  understand your changes as well as the purpose of your pull request.
- Don't forget
  to [link PR to issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue)
  if you are solving one.
- Enable the checkbox
  to [allow maintainer edits](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/allowing-changes-to-a-pull-request-branch-created-from-a-fork)
  so the branch can be updated for a merge.
  Once you submit your PR, a WeatherXM team member will review your PR. We may ask questions or
  request additional information.
- We may ask for changes to be made before a PR can be merged. You can implement those changes in
  your fork, then commit them to your branch in order to update the PR.
- As you update your PR and apply changes, mark each conversation
  as [resolved](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/commenting-on-a-pull-request#resolving-conversations).

### Your PR is merged!

Congratulations 🥳 The WeatherXM team thanks you! We really appreciate your effort and help! ♥️

Once your PR is merged, your contributions will be publicly visible on
our [Android App](https://play.google.com/store/apps/details?id=com.weatherxm.app) on the next
release.

## Styleguide

We use **[Github Flow](https://githubflow.github.io/)** as our branching model and *Clean
Architecture*.

### Android Studio Code Style Settings
We use these code style settings on Android Studio:
[android-studio-settings.zip](https://github.com/weatherxm-network/wxm-android/blob/main/android-studio-settings.zip)

### Detekt

We use [Detekt](https://github.com/detekt/detekt) as a code analysis tool for finding potential code
optimizations. Our config file for Detekt can be found
in [detekt.yml](https://github.com/weatherxm-network/wxm-android/blob/main/config/detekt.yml).

## Additional Notes

### Issue Labels

This section lists the labels we use to help us track and manage issues.

#### Type of Issue Labels

| Label name        | Description                                                               |
|-------------------|---------------------------------------------------------------------------|
| `enhancement`     | New feature request or improvement                                        |
| `bug`             | Confirmed bugs or reports that are very likely to be bugs.                |
| `question`        | Questions more than bug reports or feature requests (e.g. how do I do X). |
| `in-progress`     | A bug, feature or improvement that is currently a Work-In-Progress.       |
| `needs-attention` | An issue that needs attention to be put under specific categories/labels. |
| `wontfix`         | An issue that won't be worked on.                                         |
