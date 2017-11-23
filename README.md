# CloseBy

CloseBy is a mobile app that is developed for the Android platform. The app is a simple tracking app that takes your 
current position and provides map indication of nearby bars. 

## How to run a sample

Steps to run this project will be explained using Android Studio. Download for the IDE can be found [here](https://developer.android.com/studio/index.html).

**1.** Clone the project onto your local

```
git clone https://github.com/AhnChris/CloseBy.git
```

**2.** Start up Android Studio and open the project by clicking ```Open an existing Android Studio project```

**3.** Launch the Android SDK Manager through the top menu tabs

> Tools -> Android -> SDK Manager


**4.** Ensure that the following components are installed and updated to the latest version
 - *Android SDK Platform-Tools*
 - *Android SDK Tools*
 - *Support Repository*
    
Hit ```OK``` to exit out of the Android SDK Manager

**5.** Get an API Key

Since we will be using the Google services for maps, locations, and places we will need to grab an API key 
from [google](https://console.developers.google.com/apis/) and enable the ```Google maps for Android API``` 
and ```Google Places API for Android```.

**6.** After obtaining the key and enabling the above API. Copy the key into your **gradle.properties** file with the variable
**GOOGLE_MAPS_API_KEY**.

```GOOGLE_MAPS_API_KEY=__YOUR_API_KEY_HERE__ ```

The **gradle.properties** file can be found under the ```Gradle Scripts``` directory from the Project Solution Explorer 
using the ```Android``` view.

**7.** Compile and run
