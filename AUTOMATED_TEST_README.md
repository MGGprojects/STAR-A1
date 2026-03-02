# RUNNING AUTOMATED TESTS

This repository contains automated tests for the NewsFeed server. The test implementation connects to a local instance of the NewsFeed server and runs assertions automatically. 

Below are the instructions to successfully run `NewsFeedAutomatedTests.java`.

## Requirements
- **Java 17** (or above) installed and configured on your system.
- **Maven** installed and configured.

## Execution Steps

### 1. Start the NewsFeed Server
Before executing any tests, the NewsFeed server must be running since the automated test attempts to connect to it over a TCP socket at port `2222`.

Open a terminal and navigate to this repository's root directory, then execute the following command:

```bash
java -jar newsfeed/newsfeed-server.jar 2222
```

*(Keep this terminal open, as it will act as the server listening window. Alternatively, you can run it in the background.)*

### 2. Run the Tests Using Maven
Open a **new** terminal window and navigate back to the same root directory containing the `pom.xml` file.

To run the `NewsFeedAutomatedTests` test class specifically, execute:

```bash
mvn test -Dtest=NewsFeedAutomatedTests
```

### 3. Review the Results
Once the command successfully runs, Maven will output the test execution results within the terminal.

It will also generate reports in the `target/surefire-reports` folder. You can examine these log files (e.g., `NewsFeedAutomatedTests.txt` and `TEST-NewsFeedAutomatedTests.xml`) to see extensive details regarding the passed, failed, and timing info of the test suites.

You can also view the high-level report generated in `AUTOMATED_TEST_REPORT.md`.