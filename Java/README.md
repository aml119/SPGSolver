JPGSolver
=========

An application framework written in Java for solving Parity Games.

Requirements
---------
- JDK
- Maven
- PGSolver (for game generation)

Building and Running JPGSolver
---------
Use Maven to build the .JAR file by using the command:
```
mvn clean compile assembly:single
```
The .JAR file will then be in the target/ directory. To run the application, use the command:
```
java -jar target/JPGSolver.jar --help
```
