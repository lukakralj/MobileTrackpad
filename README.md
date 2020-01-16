# MobileTrackpad

A weekend project that transforms a phone's screen into a trackpad for a laptop mouse.

1. It works over the local WiFi so phone needs to be on the same local network as the laptop.
2. Configure laptop's IP and port in the settins in the app (the configurations are saved in history and can be re-used later with a single tap).
3. When connected, drag around to move the mouse and tap to click (left mouse click).

This app needs the second component to run on your laptop. See [this reporitory](https://github.com/lukakralj/MobileTrackpad_Server)
for more information on how to install it. 
This server also prints out the IP and port number to enter in this app's settings.

### Installation:

Two options:

a) With Android Studio:

  1. Clone repository.
  2. Open in Android Studio.
  3. Connect your phone with a USB cable.
  4. Choose "File tranfer" option on your phone's menu.
  5. Phone must have developer options enabled; installation via USB must be enabled.
  6. Click Run on Android Studio - it will build, install and run the app on your phone.
  7. Done.
  
b) Download .apk:

  1. On your phone, in settings, allow Installation from unknown sources.
  2. Go to Releases page.
  3. Download .apk I provided - the installation should start automatically.
  4. Done.
