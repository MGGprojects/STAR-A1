# BDD TESTS README

This repository contains BDD tests for the NewsFeed server using Cucumber + Gherkin.
The BDD suite checks protocol negotiation and subscription behavior.

## Requirements
- **Java 17** (or above) installed and configured.
- **Maven** installed and configured.
- **NewsFeed server JAR** available at `newsfeed/newsfeed-server.jar`.

## BDD Test Structure
- **Feature files:** `src/test/resources/features/`
- **Step definitions:** `src/test/java/NewsFeedStepDefinitions.java`
- **Cucumber test runner:** `src/test/java/RunCucumberTests.java`
- **Cucumber HTML report output:** `target/cucumber-reports/report.html`

Current feature files:
- `protocol_negotiation.feature`
- `subscriptions.feature`

## Execution Steps

### 1. Start the NewsFeed Server
The BDD tests connect to `localhost:2222`, so start the server first from the project root:

```bash
java -jar newsfeed/newsfeed-server.jar 2222
```

Keep this terminal running while tests execute.

### 2. Run the BDD Tests
Open a new terminal in the same project root and run:

```bash
mvn test -Dtest=RunCucumberTests
```

Or navigate to the RunCucumberTests class and run: the file.

This command executes the Cucumber scenarios defined in `src/test/resources/features/`.

### 3. Review Test Results
- Console output includes scenario-by-scenario pass/fail status.
- Cucumber HTML report is generated at:
  - `target/cucumber-reports/report.html`

## Writing or Updating BDD Tests

### Add a new scenario
1. Open an existing `.feature` file in `src/test/resources/features/` or create a new one.
2. Write scenarios using Gherkin syntax (`Feature`, `Background`, `Scenario`, `Given/When/Then`).
3. If you introduce new step sentences, implement matching step definitions in `NewsFeedStepDefinitions.java`.

### Change manufacturer for different test behavior
The manufacturer value is defined directly in the `Given` step inside feature files, for example:

`Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"`

You can change `"Axini"` to another manufacturer to get different test outcomes.

### Example scenario template
```gherkin
Scenario: Example behavior
  Given the NewsFeed server is running on "localhost" port 2222 with manufacturer "Axini"
  When I send a ProtocolRequest with versions "3.0" and CorrelationId 1
  Then I should receive a ProtocolResponse within 1 second
```

## Notes
- The test setup expects the server on port `2222`.
- If the server is not running, tests will fail with connection errors.
- Test execution result (Axini): 10/10 BDD tests passed when using manufacturer `"Axini"`.
