@echo off

:: Set the path to your JavaFX SDK
set JAVAFX_PATH="C:\Users\hmesa\Documents\javafx-sdk-23.0.2\lib"

:: Set the path to your JDK
set JAVA_HOME="C:\Program Files\Java\jdk-22"

:: Use the appropriate version of java (make sure it's JDK 22 or your desired version)
set PATH=%JAVA_HOME%\bin;%PATH%

:: Set the path to your JAR file
set JAR_PATH="Automonius\target\Automonius-1.0-SNAPSHOT.jar"

:: Run the JavaFX application from the JAR
java --module-path %JAVAFX_PATH% --add-modules javafx.controls,javafx.fxml -jar %JAR_PATH%

pause
