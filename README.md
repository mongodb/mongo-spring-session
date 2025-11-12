# MongoDB Spring Session extension

This product enables applications to use [Spring Session](https://spring.io/projects/spring-session)
with [MongoDB](https://www.mongodb.com/) and provides a `SessionRepository` implementation backed
by MongoDB using [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb).

## Overview

Spring Session provides an API and implementations for managing a user's session information,
while also making it trivial to support clustered sessions without being tied to an application container
specific solution. It also provides transparent integration with:

 * `HttpSession` - allows replacing the `HttpSession` in an application container (i.e. Tomcat) neutral way, with support for providing session IDs in headers to work with RESTful APIs.
 * `WebSocket` - provides the ability to keep the `HttpSession` alive when receiving WebSocket messages
 * `WebSession` - allows replacing the Spring WebFlux's `WebSession` in an application container neutral way.

## Migrating from `spring-session-data-mongodb`
Todo..

## Support / Feedback

For issues with, questions about, or feedback for the MongoDB Java, Kotlin, and Scala drivers, please look into
our [support channels](https://www.mongodb.com/docs/manual/support/). Please
do not email any of the driver developers directly with issues or
questions - you're more likely to get an answer on [StackOverflow](https://stackoverflow.com/questions/tagged/mongodb+java).

At a minimum, please include in your description the exact version of the driver that you are using.  If you are having
connectivity issues, it's often also useful to paste in the line of code where you construct the MongoClient instance,
along with the values of all parameters that you pass to the constructor. You should also check your application logs for
any connectivity-related exceptions and post those as well.

## Bugs / Feature Requests

Think you’ve found a bug? Want to see a new feature in the drivers? Please open a
case in our issue management tool, JIRA:

- [Create an account and login](https://jira.mongodb.org).
- Navigate to [the JAVA Frameworks project](https://jira.mongodb.org/browse/JAVAF).
- Click **Create Issue** - Please provide as much information as possible about the issue type, which driver you are using, and how to reproduce your issue.

Bug reports in JIRA for the extension and the Core Server (i.e. SERVER) project are **public**.

If you’ve identified a security vulnerability in a driver or any other
MongoDB project, please report it according to the [instructions here](https://www.mongodb.com/docs/manual/tutorial/create-a-vulnerability-report).

## Versioning

We follow [semantic versioning](https://semver.org/spec/v2.0.0.html) when releasing.

## Binaries

Binaries and dependency information for Maven, Gradle, Ivy and others can be found at
[https://central.sonatype.com/search](https://central.sonatype.com/search?namespace=org.mongodb&name=mongodb-driver-sync).

Example for Maven:

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongo-spring-session</artifactId>
    <version>x.y.z</version>
</dependency>
```
Snapshot builds are also published regulary via Sonatype.

Example for Maven:

```xml
<repositories>
    <repository>
        <name>Central Portal Snapshots</name>
        <id>central-portal-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### Testing

This project uses separate directories for unit and integration tests:

- [unit test](src/test)
- [integration test](src/integrationTest)

#### Gradle Tasks

##### All checks
```console
./gradlew clean check
```

##### Unit Tests only
```console
./gradlew clean test
```

##### Integration Tests only
```console
./gradlew clean integrationTest
```

Integration tests require a MongoDB deployment to be available

### CI/CD
This project uses [evergreen](https://github.com/evergreen-ci/evergreen), a distributed continuous integration system from MongoDB.
The evergreen configuration is in the [.evergreen](/.evergreen) directory.
