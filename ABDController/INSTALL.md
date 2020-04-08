# Installation of ABDController

Installation of ABDController consists of compiling the source files into .class files, and copying one .h file into the appropriate location.  This is done most straightforwardly from the command line (cmd).

JDK 8 Should already have been installed during the installation of ABDNavigator.  If not, go back and do that first: [ABDNavigator/INSTALL.md](../ABDNavigator/INSTALL.md).

## 1. Compile the source code into .class files.
Open the windows command line (as illustrated in the ABDNavigator installation instructions).

Navigate to the folder in which your ABDController clone is stored, and execute install.bat:
```cmd
C:\Users\foo\git\ABDNavigator-master>cd ABDController
C:\Users\foo\git\ABDNavigator-master\ABDController>install.bat
```

This will compile all the ABDController source code and copy a needed file, *com_MatrixInterface.h*, to the *bin* folder.  This can be verified by opening [install.bat](./install.bat) in a text editor.

## 2. Copy RemoteAccess_API.dll.
*RemoteAccess_API.dll* comes with the ScientaOmicron Matrix software and is not directly distributable.  Assuming Matrix is installed, the *RemoteAccess_API.dll* file should be located in a directory structure that looks something like this:

```cmd
C:\Program Files\Scienta Omicron\MATRIX\V4.3.0\SDK\RemoteAccess\
```

Copy *RemoteAccess_API.dll* from its default location into the *ABDController* folder, e.g.: ```cmd C:\Users\foo\git\ABDNavigator-master\ABDController```


