RasPI Check
------------

![RasPi Check Store Graphic](graphics/web_1024_500.jpg)

Android app for checking the status of your Raspberry Pi ® or other SBCs.

The goal of this Android app is to show the user the current system status of a running Raspberry Pi ®.

RasPi Check uses a SSH connection (using [SSHJ](https://github.com/hierynomus/sshj)) to connect to your Raspberry Pi ® and queries the information using Linux utilities like `ps`, `df` or the [`/proc` virtual filesystem](https://www.tldp.org/LDP/Linux-Filesystem-Hierarchy/html/proc.html). This app also works on other SBCs via [fake_vcgencmd](https://github.com/clach04/fake_vcgencmd), e.g. when running [Armbian](https://www.armbian.com).

This fork is upgraded to Android 13 but only supports login via password.

Copyright Information
------------

Forked from https://github.com/eidottermihi/rpicheck

The app logo is a derivative of "Raspberry.ico" by [Martina Šmejkalová](http://www.sireasgallery.com/), used under [CC BY](http://creativecommons.org/licenses/by/2.0/). The app logo is licensed under [CC BY](http://creativecommons.org/licenses/by/2.0/) by [Michael Prankl](https://github.com/eidottermihi).

'RasPi' is one of the Rasberry Pi ® abbreviations. For more information visit [http://www.raspberrypi.org](http://www.raspberrypi.org). Raspberry Pi is a trademark of the Raspberry Pi Foundation.
