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

Copy *RemoteAccess_API.dll* from its default location into the *ABDController* folder, e.g.: ```C:\Users\foo\git\ABDNavigator-master\ABDController\```

## 3. Add current and z samplers to the ScientaOmicron Matrix.
In order to read the tip height, **z**, and tunnel current, **I_t**, signals, Samplers need to be set up.  A Sampler consists of a window in the Matrix software as well as an experiment element connecting the appropriate signal to the Sampler.  To create these, two Matrix xml files need to be edited.

As a sub-element of ```<WindowDescription>```, copy the following xml code into the file _C:/Users/[your user name here]/AppData/Roaming/Scienta Omicron/MATRIX/default_V4_3_0/Experiments/STM_AtomManipulation.expd_:

```
    <Window name="STMSSZa_Sampler_Z" caption="Sampler_Z">
      <Layout spacing="7">
        <BoxSpecification/>
      </Layout>
      <Panel name="STMSSZa_Sampler_Z_Panel" experimentElementInstanceName="Sampler_Z" panelType="Sampler"/>
    </Window>

    <Window name="STMSSZa_Sampler_I" caption="Sampler_I">
      <Layout spacing="7">
        <BoxSpecification/>
      </Layout>
      <Panel name="STMSSZ_Sampler_I_Panel" experimentElementInstanceName="Sampler_I" panelType="Sampler"/>
    </Window>
    
    <WindowGroup name="STMS_Window_Group_6" caption="Sampler Signals" icon="AtomManipulation%NOTOOLBAR">
      <WindowRef windowRef="STMSSZa_Sampler_Z"/>
      <WindowRef windowRef="STMSSZa_Sampler_I"/>
    </WindowGroup>
```
Note that if you already have more or fewer than 5 window groups defined, you will need to adjust the name of the added ```<WindowGroup>``` to be one more than the existing number of window groups.

Finally, as a sub-element of ```<ExperimentStructure>```, copy the following xml code into the file _C:/Users/[your user name here]/AppData/Roaming/Scienta Omicron/MATRIX/default_V4_3_0/Experiments/STM_AtomManipulation.exps_:

```
  <ExperimentElementInstance name="Sampler_I" elementType="Sampler" catalogue="SPMBasic">
    <DeploymentParameter name="Device" value="Default:1:IT_Image"/>
    <DeploymentParameter name="Device_Calibration_Name" value="Regulator::Setpoint_I"/>
    <DeploymentParameter name="Label" value="I-Sampler"/>
  </ExperimentElementInstance>
  
  <ExperimentElementInstance name="Sampler_Z" elementType="Sampler" catalogue="SPMBasic">
    <DeploymentParameter name="Device" value="Default:1:Z_In"/>
    <DeploymentParameter name="Device_Calibration_Name" value="Common::Z_Out"/>
    <DeploymentParameter name="Label" value="Z-Sampler"/>
  </ExperimentElementInstance>
```

## 4. For developers (optional):
**This concludes the standard installation for the ABDController.** For those who wish to do low-level development of the ABDController to Matrix interface, see the instructions in [ABDController_C_Code/INSTALL.md](../ABDController_C_Code/INSTALL.md).


