# Testing

This project uses JUnit 5 and Mockito. Coverage is collected with JaCoCo.

## Run tests

- mvn -B test
- Coverage report is written to target/site/jacoco/index.html
- JUnit results are written to target/surefire-reports

## What is covered

- SettingsManager tests for singleton and reset.
- HighScores tests for ordering and trimming.
- Parameterized test for HighScore comparison.
- GameEvents tests for notification, error isolation, and mock verification.

## Advanced testing

- Parameterized tests use JUnit 5 CsvSource.
- Spy is shown with a simple listener that records events.
- Mockito mock verifies listener invocation.