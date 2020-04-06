# ABDNavigator (Atom-Based Device Navigator)
Scanning probe control and sample navigation for atom-based devices.

## Project Components:
**ABDNavigator** is the primary interface seen by the end-user.  It is meant to be user friendly with a google-maps-like interface in which elements of the "map" such as scanning probe images, optical images, lithographic designs, and labels are editable and can be overlayed and aligned with one another.

**ABDController** is designed to interface with a scanned probe controller, providing a means for the ABDNavigator to communicate with and control a scanning probe.  Currently an interface to the ScientaOmicron Matrix controller is implemented, though by design ABDController can be extended in a modular fashion to implement communication with other scanning probe controllers that provide sufficient APIs.  

## Contents:
1. Installation
1. Usage (how to run ABDNavigator)
1. Contributing
1. License
1. How to cite

## 1. Installation:
The two project components, ABDNavigator and ABDController, are provided as source and can be installed by compiling following the instructions in [ABDNavigator/INSTALL.md](https://github.com/usnistgov/ABDNavigator/blob/master/ABDNavigator/INSTALL.md) and [ABDController/INSTALL.md](https://github.com/usnistgov/ABDNavigator/blob/master/ABDController/INSTALL.md), respectively.

## 2. Usage (how to run ABDNavigator):
### To run ABDNavigator on Windows10
*If ABDNavigator will be used to control a scanning probe, then ABDController should be running first (see instructions on this below).*

Navigate to the ABDNavigator subfolder and execute: [run.bat](https://github.com/usnistgov/ABDNavigator/blob/master/ABDNavigator/run.bat).  It is best to do this from the command line as all error output is directed to standard out.  For example:

```cmd
C:\Users\foo\git\ABDNavigator-master>cd ABDNavigator
C:\Users\foo\git\ABDNavigator-master\ABDNavigator>run.bat
```

The contents of [run.bat](https://github.com/usnistgov/ABDNavigator/blob/master/ABDNavigator/run.bat) are as follows: `java -Xmx3000m -cp bin;lib/* main.SampleNavigator`, which allocates 3 GB of memory (the option `-Xmx3000m`).  This quantity can be adjusted by editing the .bat file as needed.

### To run ABDController on Windows10
Before starting ABDContoller, the scanning probe controller software provided by the manufacturer should already be running as the ABDController will immediately try to open up communication with it.

Navigate to the ABDController subfolder and execute: [ABDController.bat](https://github.com/usnistgov/ABDNavigator/blob/master/ABDController/ABDController.bat).  As with ABDNavigator, it is best to do this from the command line:

```cmd
C:\Users\foo\git\ABDNavigator-master>cd ABDController
C:\Users\foo\git\ABDNavigator-master\ABDController>ABDController.bat
```

## 3. Contributing:
ABDNavigator is in a state of early development and is highly specialized to the work done in the Atom-Based Devices labs at NIST.  This code has been open-sourced to github with two purposes in mind: 1) as version control for development within our labs, and 2) as a means to share the ideas that have gone into this code to the wider scanning probe community.

As such, interested parties are welcome to clone and fork this project to develop it for their own purposes, or to incorporate the ideas and algorithms presented in this code into their own works; when appropriate, please cite this work as descibed in section 5 (How to cite) below.  There are no moderators currently dedicated to merging any externally developed changes to the code back into the master-branch.  For now, any updates to the master-branch will be due to development work within our labs at NIST.

## 4. License:
**ABDNavigator** and **ABDController** are covered by the NIST license: [LICENSE.md](https://github.com/usnistgov/ABDNavigator/blob/master/LICENSE.md).

### The following libraries are packaged with this code and are distributed under open source licenses as described below:
[JAMA, Jama-1.0.3.jar,](https://math.nist.gov/javanumerics/jama/) is covered by the NIST license.

**XMLUtils.jar** is currently used by ABDController but will be phased out, and is also covered by the NIST license.

[The Java GDSII API, JGDS.jar,](http://jgds.sourceforge.net/) is covered by the [gnu general public license](https://www.gnu.org/licenses/gpl-3.0.en.html).

[The Java Backus-Naur Test API, JBNT.jar,](http://jbnt.sourceforge.net/) is also covered by the [gnu general public license](https://www.gnu.org/licenses/gpl-3.0.en.html).

## 5. How to cite:
DOI comming soon.
