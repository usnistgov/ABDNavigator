# Installation of ABDController_C_Code

The **ABDController_C_Code** is the C code that directly interfaces with the Matrix *RemoteAccess_API*.  This C code communicates with the **ABDContoller** java code via the Java Native Interface (jni).  

This document details:
1. Setup of the Visual Studio Community IDE[¹] to allow modification of *com_MatrixInteface.dll* (the dynamic link library that calls functions in the *Matrix RemoteAccess_API*)
1. The use of jni to generate *com_MatrixInterface.h* (the C header file that defines communication between Java and C).

## 1. Set up the IDE
First, if needed, install Visual Studio Community (a free IDE that enables generation of a Windows10 compliant dll), which can be found at: 
https://visualstudio.microsoft.com/vs/community/

When choosing what components to install, make sure to enable "Desktop development with C++" - this should provide everything necessary to work with the "solution" file: [ABDController_C_Code/vc/com_MatrixInterface/com_MatrixInterface.sln](vc/com_MatrixInterface/com_MatrixInterface.sln).

Next open [ABDController_C_Code/vc/com_MatrixInterface/com_MatrixInterface.sln](vc/com_MatrixInterface/com_MatrixInterface.sln) in the Visual Studio IDE.

### 1a. Ensure that the Solution configuration is "Release" and the Solution platform is "x64":
![64 bit Release](../images/vs1.PNG)

### 1b. Open the project properties window:
![Project Properties](../images/vs2.PNG)

### 1c. Under configuration properties > General, ensure that the Configuration Type is "dll" and that the Windows SDK Version is "10.0 (latest installed version)":
![Project Properties](../images/vs3.PNG)


#### ¹ Disclaimer
[¹]:#-disclaimer
Certain commercial equipment, instruments, or materials are identified in the documents of this project to foster understanding. Such identification does not imply recommendation or endorsement by the national institute of standards and technology, nor does it imply that the materials or equipment identified are necessarily the best available for the purpose.
