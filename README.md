#### DashTube extension for DashClock

DashTube is a simple extension for [DashClock](https://play.google.com/store/apps/details?id=net.nurik.roman.dashclock) that calls Transport For London's Line Status XML feed, and displays any lines with problems, together
with the severity as returned by TfL.

Some general notes:

1. The actual line status URL is not included in the source; developers have to register for access to the feed, so I'm not at liberty
    to divulge it in the repo. You can see in the DashTube module's `.gitignore` that `private.xml` is listed - this contains a single string
    called `line_status_api_url`, containing the full URL to the web service, and is not committed to the repo. If building this project from
    scratch, include this string resource somewhere appropriate in your `res/values/strings.xml` or similar.
2. DashTube uses Google's Http Client libs, and also their XmlPullParser. I've had to configure ProGuard to leave all XML-related model classes alone, as by default
    their default empty constructors were getting stripped out during the proguardRelease task.

##### Building

To checkout and build DashTube in Android Studio:

###### Check out from GitHub

     git clone https://github.com/chriswalker/dashtube.git ./DashTube

###### Create dashtube.properties

For app signing, all signing-related info (keystore, pwd, alias, keystore pwd) are stored in a separate file, which is not committed to the repo.
Assuming you have cloned the repo as described above, create a new file `DashTube/DashTube/dashtube.properties`, which contains the following keys:

    keystore=[path to your keystore]
    password=[keystore password]
    alias=[alias for signing]
    keyPassword=[key password]

The `dashtube.properties` file is referenced in `DashTube/DashTube/build.gradle`;  extrapolation of the keystore info was implemented as described
[here](https://www.timroes.de/2013/09/22/handling-signing-configs-with-gradle/), and also requires you to define a `DashTube.properties` property
in your `~/.gradle/gradle.properties` file, so the property is available to the DashTube module's gradle script.

###### Import into Android Studio (correct as of v0.4.0)

     File -> Import Project
     Select top-level DashTube directory
     Accept Gradle defaults

This should get you a functioning, building project, to fiddle with as desired.
