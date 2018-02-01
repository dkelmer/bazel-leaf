# 🍂🐘 Bazel Leaf

A Gradle plugin that builds modules using Bazel.

# Setup

## Development Setup
* This was only tested with _Android Studio 3.1-alpha9_, on macOS.
* ensure you have Bazel installed. Follow the instructions [here](https://docs.bazel.build/versions/master/install.html)
* (optionally) if `bazel` binary in not in PATH set the path to it in `local.properties`:
  * `bazel.bin.path=/usr/local/bin/bazel`
* Open this project using Android Studio.
* Read the [Known Issues](#known-issues) section.

# Repo Structure:

```
app - an Android app which is built using Gradle
  |
  |_ lib1 - a Java lib that is built using Gradle
  |    |
  |    |_ Lib2 - a Java lib that is build using Bazel
  |
  |_ lib3 - a Java lib that is built using Bazel
  |    |
  |    |_ lib4 - a Java lib that is built using Bazel
  |
  |_ andlib - an Android lib that is built using Bazel
  |    |
  |    |_ innerandlib - an Android lib that is built using Bazel
  |
  |_ gandlib - an Android lib that is built using Gradle
       |
       |_ innerandlib - an Android lib that is built using Bazel
       |
       |_ lib4 - a Java lib that is built using Bazel


```

## Module Setup
For each module that is built using Bazel:
* create a `build.gradle` file for the module
* apply the `bazel-leaf` plugin:
```
apply plugin: 'bazelleaf'
```
* add build configuration, and specify which target should be built with Bazel:
```
bazel {
    target = 'jar'
}
```

# Road Map
* Support for running tests using JUnit.
* Support for running tests using Robolectric.
* Support for remote (Maven?) dependencies.

# Known Issues
* In most cases, you'll need to first build the project from command line before importing it into Android Studio. For this project, run
`./gradlew :app:assembleDebug` before importing into Android Studio, or before doing gradle-sync for the first time.
* When running a gradle task from the Android Studio UI, you'll fail, and get the following error: `Error running 'bazel-leaf:app [assembleDebug]': com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration cannot be cast to com.intellij.execution.configurations.ModuleBasedConfiguration` Still trying to figure this one out.