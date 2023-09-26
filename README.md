# BDD testing 6502

This subdirectory contains the code for BDD testing ASM code.

## Building

Because this relies on the [BDD6502 framework](https://github.com/martinpiper/BDD6502), there
are some libraries that first have to be built into your local maven repository.
Hopefully this will eventually change when the author of BDD6502 releases his code to a
public repository.

Install the following:

- java 8 or 11 - [sdkman](https://sdkman.io/) or other mechanism of your choice
- maven - [sdkman](https://sdkman.io/), or [Apache Maven Project](https://maven.apache.org/install.html)

Note: The required BDD6502 libraries currently do not support higher than Java 14.

### Install BDD6502 libraries

Clone the following 3 projects into a directory on your machine, build them, then come back to this project.

```shell
mkdir bdd
cd bdd
git clone https://github.com/martinpiper/ACEServer.git
git clone https://github.com/martinpiper/BDD6502.git
git clone https://github.com/martinpiper/CukesPlus.git

cd ACEServer
mvn install

cd ../CukesPlus
mvn install

cd ../BDD6502
mvn install -DskipTests
```

You will now have the required libraries installed locally to be able to compile this project.

### Build and install fujinet-bdd library

The project needs to be built and installed in a local maven repo so it can be used to run your BDD tests in appropriate projects.

```shell
./gradlew publishToMavenLocal
```

This will install the library in your HOME dir, under `.m2/repository/fujinet`

You can access it in your project with the artifact `fujinet:fujinet-bdd:1.0.0-SNAPSHOT`
