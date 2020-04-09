# Installation of ABDController_C_Code

The **ABDController_C_Code** is the C code that directly interfaces with the Matrix *RemoteAccess_API*.  This C code communicates with the **ABDContoller** java code via the Java Native Interface (jni).  

This document details setup of the Visual Studio Community IDE[¹] to allow modification of *com_MatrixInteface.dll* (the dynamic link library that calls functions in the *Matrix RemoteAccess_API*), as well as the use of jni to generate *com_MatrixInterface.h* (the C header file that defines communication between Java and C).

## 1. Set up the IDE




#### ¹ Disclaimer
[¹]:#-disclaimer
Certain commercial equipment, instruments, or materials are identified in the documents of this project to foster understanding. Such identification does not imply recommendation or endorsement by the national institute of standards and technology, nor does it imply that the materials or equipment identified are necessarily the best available for the purpose.
