@echo off
set JAVA_HOME=C:\Proyectos\jdk-17.0.17+10
set PATH=%JAVA_HOME%\bin;C:\Proyectos\apache-maven-3.9.6\bin;%PATH%
cd /d C:\Proyectos\ferreteria-java
mvn javafx:run
