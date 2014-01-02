#### DashTube extension for DashClock

DashTube is a simple extension that calls Transport For London's Line Status XML feed, and displays any lines with problems, together
with the severity as returned by TfL.

Some general notes:

1. The actual line status URL is not included in the source; developers have to register for access to the feed, so I'm not at liberty
    to divulge it in the repo. You can see in the DashTube module's `.gitignore` that `private.xml` is registered - this contains a single string called
    `line_status_api_url`, containing the full URL to the web service. If building this project from scratch, include this string resource somewhere
    appropriate in your values/ folder (in strings.xml will be fine).
2. DashTube uses Google's Http Client libs, and also their XmlPullParser. I've had to set ProGuard to keep all XML-related model classes, as by default
    their default empty constructors were getting stripped out by ProGuard.

##### Building

To checkout and build DashTube in Android Studio:

###### Check out from GitHub

     git clone https://github.com/chriswalker/dashtube.git ./dashtube

###### Import into Android Studio (correct as of v0.4.0)

     TBC
