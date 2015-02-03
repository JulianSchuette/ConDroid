ConDroid
==========================

This project is a fork and extension of the Acteve project on concolic execution for Android (https://code.google.com/p/acteve/)


ConDroid performs <i>concolic</i> execution of Android apps - a combination of pure symbolic and concrete execution of a program which has first been describen in [1] for C programs. 
The goal of ConDroid is to drive execution of Android app to specific code locations without requiring any manual interaction with the app. This allows to observe "interesting" behavior in a dynamic analysis, such as network traffic or dynamic code loading.


[1] CUTE: A concolic unit testing engine for C

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/JulianSchuette/condroid/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
<a href="https://scan.coverity.com/projects/3500">
<img alt="Coverity Scan Build Status"
src="https://scan.coverity.com/projects/3500/badge.svg"/></a>
