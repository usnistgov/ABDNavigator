# Quick Start Tutorial

## Startup
Before starting, ensure that a directory has been setup where the data will be stored which contains a navigation file.  A default navigation file can be copied from the ABDNavigator folder: [default_deviceNavigation.xml](default_deviceNavigation.xml).

![copy](../images/sn1.PNG)

When you first start ABDNavigator (using [run.bat](run.bat)) from the command line, a "file open" dialog is displayed. Find the data directory you setup and choose the navigation xml (e.g. *default_deviceNavigation.xml*) file. 

![open nav file](../images/sn2.PNG)

## Some First Steps
There is currently no menu system implemented, so all functionality is achieved through **hot-keys** (detailed in the next section) and operations exposed by the **Attribute Editor**.  As an introduction, here are some first steps to try:

## 1. Toggle the **Tree View** 
Press: **Ctrl** + **T**

![Tree View](../images/sn3.PNG)

## 2. Toggle the **Attribute Editor**
Press: **Ctrl** + **A**

![Attribute Editor](../images/sn4.PNG)

## 3. Create a **PathLayer**
Select *NavigationLayer* in the **Tree View**.  Then Press: **Ctrl** + **=**

![PathLayer](../images/sn5.PNG)

## 4. Create a course step
Select the *PathLayer* or one of the existing coars steps within it.  Then press: **=**

Now move the mouse to the new location to be coarse walked to, and click.

![PathNode](../images/sn6b.PNG)

## 5. Add an image to the *NavigationLayer*
Select *NavigationLayer* in the **Tree View**.  Then press: **Ctrl** + **o**

In the **File Open** dialog, ensure that "Image File" is selected in the file type dropdown.

![FileType](../images/sn7.PNG)

Then choose the desired image file.  This will add an image as a "child" of the *Navigation Layer*.

![Image](../images/sn8.PNG)

## 6. Zoom in and out, and move the map
Use the **middle mouse-wheel** to zoom in and out.  **Left-click** and **drag** to move around on the **map**.  Click on map objects to select them (or alternatively click on them in the **Tree View**).

![zoom](../images/sn9.PNG)

## 7. Move the selected object
Select the image that was added in step 5.  While holding down **Alt**, **left-click** and **drag**.  

![move](../images/sn10.PNG)

## 8. Hide selected objects
Select an object on the map or in the **Tree View**.  Press: **Space**.  

![hide](../images/sn11.PNG)

The selected object will be hidden, and its listing in the **Tree View** will be grayed out.

## 9. Add an image to something other than the *NavigationLayer*
Select one of the *image* groups from the **Tree View**.  Then open an image as before (using **Ctrl** + **o**).

![newImage](../images/sn12.PNG)

## 10. Adjust the position of one of the coarse walk nodes
This is useful if the coarse walk calibration is a little off.  Select the node in the tree view and perform a move operation as described above (while holding **Alt**, **left-click** and **drag**).

![moveNode](../images/sn13.PNG)

## 11. Move the *PathLayer* in front of the image that was added to the *NavigationLayer* before
Select *PathLayer* in the **Tree View**.  Then press: **Ctrl** + **F**.

![forwards](../images/sn14.PNG)

(**Ctrl** + **B** will move it backwards)

## 12. Rotate the map
**Right-click** and **drag**

![rotateView](../images/sn15.PNG)

## 13. Add a *Group* object to the *NavigationLayer*
Select *NavigationLayer* in the **Tree View**.  Then press: **Ctrl** + **g**.

![new group](../images/sn16.PNG)

The *Group* can be re-named by selecting it in the **Tree View** and editing the *name* attribute in the **Attribute Editor**.

![name group](../images/sn17.PNG)

For the edit to take place, make sure to press **Enter**.

![group named](../images/sn18.PNG)

## 14. Save your edits
Press: **Ctrl** + **s**
