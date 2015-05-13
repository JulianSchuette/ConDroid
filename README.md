ConDroid
==========================

This project is a fork and extension of the Acteve project on concolic execution for Android (https://code.google.com/p/acteve/) [1]

ConDroid performs <i>concolic</i> execution of Android apps - a combination of pure symbolic and concrete execution of a program which has first been described in [2] for C programs. 
The goal of ConDroid is to drive execution of Android app to specific code locations without requiring any manual interaction with the app. This allows to observe "interesting" behavior in a dynamic analysis, such as network traffic or dynamic code loading.

Some details on the extensions have been published in [3].

[1] Saswat Anand, Mayur Naik, Hongseok Yang, Mary Jean Harrold. Automated Concolic Testing of Smartphone Apps. In ACM International Symposium on Foundations of Software Engineering (FSE), 2012. 

[2] Koushik Sen, Darko Marinov, Gul Agha. CUTE: A concolic unit testing engine for C. In Proceedings of the 10th European software engineering conference held jointly with 13th ACM SIGSOFT international symposium on Foundations of software engineering, 2005

[3] Julian Schütte, Rafael Fedler, Dennis Titze. ConDroid: Targeted Dynamic Analysis of Android Applications. In IEEE Conference on Advanced Information Networking and Applications (AINA), 2015

[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/JulianSchuette/condroid/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
<a href="https://scan.coverity.com/projects/3500">
<img alt="Coverity Scan Build Status"
src="https://scan.coverity.com/projects/3500/badge.svg"/></a>
<a href="#" name="status-images" class="open-popup" data-ember-action="786"><img src="https://travis-ci.org/JulianSchuette/ConDroid.svg" data-bindattr-787="787" title="Build Status Images"></a>
