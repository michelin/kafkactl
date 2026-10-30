# Agent Instructions

## Repository Structure

Single-module Gradle Micronaut Picocli CLI project with the base package `com.michelin.kafkactl`.

## Commands

- Build with `./gradlew build`.
- Run the tests with `./gradlew test`.
- Run `./gradlew spotlessApply` before committing to apply Palantir Java Format.

## Coding Standards

- Target Java 25.
- Never use `var`. Always declare variables with their explicit type.
- Use Lombok for models and loggers (`@Data`, `@Builder`, `@Slf4j`, etc.).
- Use `@Inject` field injection in Picocli commands, and explicit constructor injection in other beans.
- Prefer guard clauses (early returns) over `if ... else ...`.
- Code follows Palantir Java Format.
- Add minimal Javadoc to every method in production code (`src/main`), including constructors and `@Override` methods, with descriptions for parameters and return values. Start each description with an uppercase letter.

## Testing Standards

- Name test classes `<ClassName>Test`.
- Name unit tests with the "should..." convention (e.g. `shouldGetVersionWhenServerDoesNotRespond`).
- Use JUnit Jupiter assertions (`org.junit.jupiter.api.Assertions`).
- Use Mockito with `@ExtendWith(MockitoExtension.class)`.
