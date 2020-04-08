# AAIoT Lib

## Prerequisites
The following software needs to be installed for this project to compile and run:
* Java JDK 8+
* Gradle

This project also depends on the ace-java (https://bitbucket.org/sebastian_echeverria/ace-java-postgres) library. You should download, compile, and deploy it to a local Maven repo, so that this project will it them when resolving its dependencies.

To do this:
1. Clone that repo to your PC.
1. From the command line inside the folder with your cloned repo, execute `mvn install -DskipTests`
 
## Configuration
No configuration is needed.
 
## Usage
Deploy to a local Maven cache, and include it in your dependencies. 

To install to local Maven repo:
1. Run `./gradlew publishToMavenLocal` from the main repo folder.

To include in your dependencies in a Gradle project, add this to the dependencies section of build.gradle of your project:

`compile group: 'edu.cmu.sei.ttg', name: 'aaiot-lib', version: '0.0.1-SNAPSHOT'`
