/** What's this? **/
This project demonstrates how to use QuickMarkSDK.

/** Build **/
Within this, there is a build.xml Ant build file which controls building of that component.
If Apache's ant tool is already present on your system, simply type "ant" to build that component. 
See http://ant.apache.org for details on how to obtain this free, standard build tool for Java.

/** Run on Android **/
1. Download and install the latest public Android SDK.
2. Edit build.properties and change the android-home property to point to the SDK install location.
3. Download Proguard, version 4.4 minimum, and install it where you like.
4. Edit build.properties and set proguard-jar to the full path (including the filename) of the ProGuard library.
5. Build sdkDemo
	ant
6. Connect your device via USB
7. On the device, under Settings > Application, selected "Unknown Sources"
8. The application should have been built to bin/sdkDemo-debug.apk. Install with Android's adb tool:
	adb reinstall bin/sdkDemo-debug.apk

/** Build on Eclipse **/
1. Start the Eclipse IDE
2. From the menu select File -> New -> Project¡K
3. In the new window select Android -> Android Project
4. If Build targets are NOT populated at all and there¡¦s activity in the progress tab:
	1) Close the New Project window.
	2) Wait for the Android SDK Content Loader activity to complete (watch the progress tab!)
	3) From the menu select File -> New -> Project¡K again
	4) In the new window select Android -> Android Project again
	5) At this point the Build Targets should be properly populated and the progress tab should be idle/empty.
5. Give the project a name.  For this example I used ¡§sdkDemo¡¨ and will refer to that going forward.
6. Select Create project from existing source
7. Browse to where you put the sdkDemo source tree (this will become your workspace!) and click ok.
8. The Application name will be auto-filled as CaptureActivity
9. Click Finish
	