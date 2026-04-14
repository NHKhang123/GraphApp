@echo off
set SCRIPT_DIR=%~dp0
set JAVAFX_PATH=%SCRIPT_DIR%lib\javafx-sdk-26\lib
set MAIN_JAR=%SCRIPT_DIR%out\artifacts\GraphApplication_jar\GraphApplication.jar

javaw --module-path "%JAVAFX_PATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
  --enable-native-access=javafx.graphics ^
  -cp "%MAIN_JAR%" ui.Main