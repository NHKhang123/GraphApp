::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCqDJGqQ4UYpFDhgHlfbGTvoS5sV7ebE4/+GrHIeW90sd7DV3bWAKNwy+lXYV5cu3V9UnPcfHhhkdx+sUg4kuSNLtWuLec6fvG8=
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSTk=
::cBs/ulQjdF+5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+JeA==
::cxY6rQJ7JhzQF1fEqQJQ
::ZQ05rAF9IBncCkqN+0xwdVs0
::ZQ05rAF9IAHYFVzEqQJQ
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQJQ
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATElA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::Zh4grVQjdCqDJGqQ4UYpFDhgHlfbGTvoS5sV7ebE4/+GrHIeW90sd7DV3bWAKNwy+lXYUoQsxnJ7iNtCCQNdHg==
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
@echo off
set SCRIPT_DIR=%~dp0
set JAVAFX_PATH=%SCRIPT_DIR%lib\javafx-sdk-26\lib
set MAIN_JAR=%SCRIPT_DIR%out\artifacts\GraphApplication_jar\GraphApplication.jar

javaw --module-path "%JAVAFX_PATH%" ^
  --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base,javafx.swing ^
  --enable-native-access=javafx.graphics ^
  -cp "%MAIN_JAR%" ui.Main