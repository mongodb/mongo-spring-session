## Contributing to the MongoDB Spring Session extension

Thank you for your interest in contributing to the MongoDB Spring Session extension.

We are building this software together and strongly encourage contributions from the community that are within the guidelines set forth
below.

Bug Fixes and New Features
--------------------------

Before starting to write code, look for existing [tickets](https://jira.mongodb.org/browse/JAVAF) or
[create one](https://jira.mongodb.org/secure/CreateIssue!default.jspa) for your bug, issue, or feature request. This helps the community
avoid working on something that might not be of interest or which has already been addressed.

Pull Requests
-------------

Pull requests should generally be made against the main (default) branch and include relevant tests, if applicable.

Code should compile with the Java 17 compiler and tests should pass under all Java versions which the driver currently
supports.

The results of pull request testing will be appended to the request. If any tests do not pass, or relevant tests are not included, the
pull request will not be considered.

To run all checks locally run:

```console
./gradlew clean check
```

Talk To Us
----------

If you want to work on something or have questions / complaints please reach out to us by creating a Question issue at
(https://jira.mongodb.org/secure/CreateIssue!default.jspa).
