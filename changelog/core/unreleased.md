### Added

- IoC container with constructor injection, `@Component`, `@Service`, `@Repository`, `@Qualifier`, `@Primary`, `@Order`, and `Option[T]` and `List[T]` dependencies (COILIX-00000).
- `@Slf4s` compiler plugin that adds an SLF4J `log` field (COILIX-00000).
- Application startup with `@CoilixApplication`, `Coilix.run`, `ApplicationRunner`, command-line arguments, a banner and a shutdown hook (COILIX-00002).
- Configuration from HOCON, YAML and properties files, command-line options and system properties, `@Value`, `@ConfigurationProperties`, `@Configuration` with `@Bean`, and replacement of framework components through a `class` key under their `coilix.*` path (COILIX-00003).
- `EditOption[T]` for partial updates, which tells a field that stays unchanged (`NoEdit`) apart from a field that is set (`SetTo(Some(value))`) or cleared (`SetTo(None)`) (COILIX-00022).
- `Validator[T]` with `Violation`, `ViolationTemplate` and `ValidationException`, which checks every rule of a value and reports all broken rules at once. `Violation("...")` and `ViolationTemplate("... %s")(...)` take their code from the name of the `val` and fail compilation for a malformed message or template (COILIX-00022).
- `@ValueType` for classes that wrap one value, with `ValueTypeResolver` to create them from their inner value, and `Secret`, a value type for passwords and tokens that `toString` hides (COILIX-00022).

### Changed

- Startup log messages are now `Launching <app>` and `<app> is up after <n> s` (COILIX-00008).
- Artifacts are compiled with Scala 3.9 LTS for JDK 17, so they work with Scala 3.9 or newer and JDK 17 or newer (COILIX-00009).

### Fixed

- A bean with an `Option[T]` or `List[T]` dependency now receives every matching bean, not only the beans created before it. A cycle through an `Option[T]` dependency is now reported as `CircularDependencyException` (COILIX-00012).
